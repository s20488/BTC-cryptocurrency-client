package com.example.btc_project;

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

}
