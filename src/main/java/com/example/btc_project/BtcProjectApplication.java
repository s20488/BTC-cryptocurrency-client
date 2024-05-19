package com.example.btc_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.DecimalFormat;
import java.util.List;
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
		System.out.println("1. Get a list of addresses");
		System.out.println("2. Check wallet balance");
		System.out.println("3. Send funds");
		System.out.println("4. Get new address");
		int choice = scanner.nextInt();
		double amount = scanner.nextDouble();

		switch (choice) {
			case 1:
				List<String> addresses = bitcoinRpcClient.getAddresses();
				System.out.println("List of available addresses:");
				for (String address : addresses) {
					System.out.println(address);
				}
				break;
			case 2:
				System.out.println("Balance: " + df.format(balance) + " BTC");
				break;
			case 3:
				System.out.println("Enter the recipient's address:");
				String toAddress = scanner.next();
				System.out.println("Enter the amount to send:");
				String formattedAmount = df.format(amount);

				String formattedBalance = df.format(balance);
				if (formattedAmount.compareTo(formattedBalance) > 0) {
					System.out.println("Not enough funds to complete the transaction.");
					break;
				}

				String transactionId = bitcoinRpcClient.sendFunds(toAddress, amount);
				System.out.println("Funds sent successfully. Transaction ID: " + transactionId);
				break;
			case 4:
				System.out.println("Enter the desired amount:");
				String newAddress = bitcoinRpcClient.getNewAddressAndGenerateQRCode(amount);
				System.out.println("Your new address: " + newAddress + ". QR code with the amount " + amount + " BTC was successfully generated!");
				break;
			default:
				System.out.println("Incorrect operation selection");
				break;
		}
	}
}
