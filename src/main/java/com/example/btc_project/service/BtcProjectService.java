package com.example.btc_project.service;

import com.google.zxing.WriterException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BtcProjectService {

    double getBalance() throws IOException;

    List<Map<String, String>> getAddresses() throws IOException;

    double getReceivedByAddress(String address) throws IOException;

    String sendFunds(String toAddress, double amount, int feeRate, boolean subtractFeeFromAmount) throws IOException;

    BufferedImage getNewAddressAndGenerateQRCode(double amount) throws IOException, WriterException;

    List<Map<String, Object>> getTransactionHistory() throws IOException;

    String monitorTransactionStatus(String txid) throws IOException;
}
