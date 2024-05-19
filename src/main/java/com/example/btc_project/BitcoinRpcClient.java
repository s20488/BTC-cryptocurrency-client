package com.example.btc_project;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public List<String> getAddresses() throws IOException {
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
            List<String> addresses = new ArrayList<>();
            Iterator<String> addressNames = responseJson.get("result").fieldNames();
            while (addressNames.hasNext()) {
                addresses.add(addressNames.next());
            }
            return addresses;
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
                System.out.println("QR-код успешно сгенерирован!");
            } catch (com.google.zxing.WriterException e) {
                System.err.println("Ошибка при создании QR-кода: " + e.getMessage());
            }
            return newAddress;
        }
    }
}
