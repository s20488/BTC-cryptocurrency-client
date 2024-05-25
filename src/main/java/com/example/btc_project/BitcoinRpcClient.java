package com.example.btc_project;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;
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
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class BitcoinRpcClient {

    private final OkHttpClient httpClient;
    private final String rpcUrl;
    private final String credentials;
    private final ObjectMapper objectMapper;

    public BitcoinRpcClient(@Value("${bitcoin.rpc.url}") String rpcUrl,
                            @Value("${bitcoin.rpc.username}") String rpcUsername,
                            @Value("${bitcoin.rpc.password}") String rpcPassword) {
        this.httpClient = new OkHttpClient();
        this.rpcUrl = rpcUrl;
        this.credentials = Credentials.basic(rpcUsername, rpcPassword);
        this.objectMapper = new ObjectMapper();
    }

    public double getBalance() throws IOException {
        String requestBody = "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"getbalance\",\"params\":[]}";
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            JsonNode responseJson = objectMapper.readTree(response.body().string());
            return responseJson.get("result").asDouble();
        }
    }

    public List<Map<String, String>> getAddresses() throws IOException {
        String requestBody = "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"getaddressesbylabel\",\"params\":[\"\"]}";
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            JsonNode responseJson = objectMapper.readTree(response.body().string());
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
    }

    public double getReceivedByAddress(String address) throws IOException {
        String requestBody = String.format("{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"getreceivedbyaddress\",\"params\":[\"%s\"]}", address);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            JsonNode responseJson = objectMapper.readTree(response.body().string());
            return responseJson.get("result").asDouble();
        }
    }

    public String sendFunds(String toAddress, double amount) throws IOException {
        String requestBody = String.format("{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"sendtoaddress\",\"params\":[\"%s\", %f]}", toAddress, amount);
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code());
            }

            JsonNode responseJson = objectMapper.readTree(response.body().string());
            return responseJson.get("result").textValue();
        }
    }

    public String getNewAddressAndGenerateQRCode(double amount) throws IOException {
        String requestBody = "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"getnewaddress\",\"params\":[]}";
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            JsonNode responseJson = objectMapper.readTree(response.body().string());
            String newAddress = responseJson.get("result").textValue();

            String bitcoinUri = "bitcoin:" + newAddress + "?amount=" + amount;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            try {
                BufferedImage qrCode = MatrixToImageWriter.toBufferedImage(qrCodeWriter.encode(bitcoinUri, BarcodeFormat.QR_CODE, 200, 200));
                ImageIO.write(qrCode, "PNG", new File("QRCode.png"));
                System.out.println("QR code successfully generated!");
            } catch (WriterException e) {
                System.err.println("Error while creating a QR code: " + e.getMessage());
            }
            return newAddress;
        }
    }

    public List<Map<String, Object>> getTransactionHistory() throws IOException {
        String requestBody = "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"listtransactions\",\"params\":[\"*\", 100]}";
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            JsonNode responseJson = objectMapper.readTree(response.body().string());
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
                    transactions.add(transactionInfo);
                }
            }
            return transactions;
        }
    }

    public void monitorTransactionConfirmations(String txid) throws IOException, InterruptedException {
        final int requiredConfirmations = 6;
        String requestBody = "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"gettransaction\",\"params\":[\"" + txid + "\"]}";
        Request request = new Request.Builder()
                .url(rpcUrl)
                .addHeader("Authorization", credentials)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        while (true) {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                JsonNode responseJson = objectMapper.readTree(response.body().string());
                JsonNode result = responseJson.get("result");
                if (result != null) {
                    int confirmations = result.get("confirmations").asInt();
                    String status = "";
                    if (confirmations == 0) {
                        status = "Unconfirmed";
                    } else if (confirmations < requiredConfirmations) {
                        status = "Pending";
                    } else if (confirmations >= requiredConfirmations && confirmations < 10) {
                        status = "Confirmed";
                    }

                    System.out.println("Transaction ID: " + txid);
                    System.out.println("Confirmations: " + confirmations);
                    System.out.println("Status: " + status);

                    if (confirmations >= requiredConfirmations) {
                        System.out.println("Transaction has reached the required number of confirmations.");
                        break;
                    }
                } else {
                    System.out.println("Transaction not found.");
                    break;
                }
            }
            Thread.sleep(60000);
        }
    }
}
