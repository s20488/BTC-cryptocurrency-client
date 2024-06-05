package com.example.btc_project.service.impl;

import com.example.btc_project.service.BtcProjectService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BtcProjectServiceImpl implements BtcProjectService {

    private final OkHttpClient httpClient;
    private final String rpcUrl;
    private final String credentials;
    private final ObjectMapper objectMapper;

    public BtcProjectServiceImpl(@Value("${bitcoin.rpc.url}") String rpcUrl,
                                 @Value("${bitcoin.rpc.username}") String rpcUsername,
                                 @Value("${bitcoin.rpc.password}") String rpcPassword) throws IOException {
        this.httpClient = new OkHttpClient();
        this.rpcUrl = rpcUrl;
        this.credentials = Credentials.basic(rpcUsername, rpcPassword);
        this.objectMapper = new ObjectMapper();
        loadWallet();
    }

    private void loadWallet() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bitcoin-cli", "loadwallet", "testnet");
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = stdError.readLine()) != null) {
                    if (s.contains("Wallet \"testnet\" is already loaded.")) {
                        return;
                    }
                }
                throw new IOException("Error while loading wallet");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while loading wallet", e);
        }
    }

    private JsonNode makeRpcCall(String method, Object... params) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("jsonrpc", "1.0");
        requestBody.put("id", "curltext");
        requestBody.put("method", method);
        ArrayNode paramsNode = requestBody.putArray("params");
        for (Object param : params) {
            paramsNode.addPOJO(param);
        }

        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            assert response.body() != null;
            return objectMapper.readTree(response.body().string());
        }
    }


    public double getBalance() throws IOException {
        JsonNode responseJson = makeRpcCall("getbalance");
        return responseJson.get("result").asDouble();
    }

    public List<Map<String, String>> getAddresses() throws IOException {
        JsonNode responseJson = makeRpcCall("getaddressesbylabel", "");
        List<Map<String, String>> addresses = new ArrayList<>();
        JsonNode result = responseJson.get("result");
        if (result != null) {
            Iterator<Map.Entry<String, JsonNode>> addressFields = result.fields();
            while (addressFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = addressFields.next();
                String address = entry.getKey();
                JsonNode addressData = entry.getValue();
                String addressType = addressData.has("purpose") ? addressData.get("purpose").asText() : "unknown";
                Map<String, String> addressInfo = new HashMap<>();
                addressInfo.put("address", address);
                addressInfo.put("type", addressType);
                addresses.add(addressInfo);
            }
        }
        return addresses;
    }

    public double getReceivedByAddress(String address) throws IOException {
        JsonNode responseJson = makeRpcCall("getreceivedbyaddress", address);
        return responseJson.get("result").asDouble();
    }

    public String sendFunds(String toAddress, double amount, int feeRate, boolean subtractFeeFromAmount) throws IOException {
        String command = "bitcoin-cli -named sendtoaddress address=" + toAddress + " amount=" + amount + " fee_rate=" + feeRate + " subtractfeefromamount=" + subtractFeeFromAmount;
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }
        return output.toString();
    }

    public BufferedImage getNewAddressAndGenerateQRCode(double amount) throws IOException, WriterException {
        JsonNode responseJson = makeRpcCall("getnewaddress");
        String newAddress = responseJson.get("result").textValue();

        String bitcoinUri = "bitcoin:" + newAddress + "?amount=" + amount;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return MatrixToImageWriter.toBufferedImage(qrCodeWriter.encode(bitcoinUri, BarcodeFormat.QR_CODE, 200, 200));
    }


    public List<Map<String, Object>> getTransactionHistory() throws IOException {
        JsonNode responseJson = makeRpcCall("listtransactions", "*", 100);
        List<Map<String, Object>> transactions = new ArrayList<>();
        JsonNode result = responseJson.get("result");
        if (result != null && result.isArray()) {
            for (JsonNode transaction : result) {
                Map<String, Object> transactionInfo = new HashMap<>();
                long timestamp = transaction.get("time").asLong();
                Instant instant = Instant.ofEpochSecond(timestamp);
                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDateTime = dateTime.format(formatter);

                String amount = transaction.get("amount").asText();
                String category = transaction.get("category").asText();
                transactionInfo.put("time", formattedDateTime);
                transactionInfo.put("amount", amount);
                transactionInfo.put("category", category);

                String txid = transaction.get("txid").asText();

                String status = monitorTransactionStatus(txid);
                transactionInfo.put("status", status);

                transactions.add(transactionInfo);
            }
        }
        return transactions;
    }

    public String monitorTransactionStatus(String txid) {
        final int requiredConfirmations = 6;
        while (true) {
            try {
                JsonNode responseJson = makeRpcCall("gettransaction", txid);
                JsonNode result = responseJson.get("result");
                if (result != null) {
                    int confirmations = result.get("confirmations").asInt();
                    String status;
                    if (confirmations == 0) {
                        status = "Unconfirmed";
                    } else if (confirmations < requiredConfirmations) {
                        status = "Pending";
                    } else {
                        status = "Confirmed";
                    }
                    return status;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
}
