package com.example.btc_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class BtcProjectApplication implements CommandLineRunner {

	@Autowired
	private BitcoinRpcClient bitcoinRpcClient;

	public static void main(String[] args) {
		SpringApplication.run(BtcProjectApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		double balance = bitcoinRpcClient.getBalance();
		DecimalFormat df = new DecimalFormat("0.########");

		Scanner scanner = new Scanner(System.in);
		System.out.println("Choose operation:");
		System.out.println("1. Balance ");
		System.out.println("2. Addresses");
		System.out.println("3. Send");
		System.out.println("4. Receive");
		System.out.println("5. Transaction history");
		int choice = scanner.nextInt();

		switch (choice) {
			case 1:
				System.out.println("Balance: " + df.format(balance) + " BTC");
				break;
			case 2:
				List<Map<String, String>> addresses = bitcoinRpcClient.getAddresses();
				for (Map<String, String> addressInfo : addresses) {
					String address = addressInfo.get("address");
					String type = addressInfo.get("type");
					double balanceAddress = bitcoinRpcClient.getReceivedByAddress(address);
					System.out.println("Address: " + address + ", Type: " + type + ", Balance: " + df.format(balanceAddress));
				}
				break;
			case 3:
				System.out.println("Enter the recipient's address:");
				String toAddress = scanner.next();
				System.out.println("Enter the amount to send:");
				double amountSend = scanner.nextDouble();
				String formattedAmount = df.format(amountSend);

				String formattedBalance = df.format(balance);
				if (formattedAmount.compareTo(formattedBalance) > 0) {
					System.out.println("Not enough funds to complete the transaction.");
					break;
				}

				String transactionId = bitcoinRpcClient.sendFunds(toAddress, amountSend);
				System.out.println("Funds sent successfully. Transaction ID: " + transactionId);
				break;
			case 4:
				System.out.println("Enter the desired amount:");
				double amountReceive = scanner.nextDouble();
				String newAddress = bitcoinRpcClient.getNewAddressAndGenerateQRCode(amountReceive);
				System.out.println("Your new address: " + newAddress + ". QR code with the amount " + amountReceive + " BTC was successfully generated!");
				break;
			case 5:
				List<Map<String, Object>> transactionsWithStatus = bitcoinRpcClient.getTransactionHistory();
				for (Map<String, Object> transaction : transactionsWithStatus) {
					String time = transaction.get("time").toString();
					double amountTransaction = Double.parseDouble(transaction.get("amount").toString());
					String category = transaction.get("category").toString();
					String status = transaction.get("status").toString();
					System.out.println("Time: " + time + ", Amount: " + df.format(amountTransaction) + ", Category: " + category + ", Status: " + status);
				}
				break;
			default:
				System.out.println("Incorrect operation selection");
				break;
		}
	}
}
