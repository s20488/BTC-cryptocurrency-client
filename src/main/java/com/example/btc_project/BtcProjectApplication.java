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
		Scanner scanner = new Scanner(System.in);
		System.out.println("Выберите операцию:");
		System.out.println("1. Получить список адресов");
		System.out.println("2. Проверить баланс");
		int choice = scanner.nextInt();

		switch (choice) {
			case 1:
				List<String> addresses = bitcoinRpcClient.getAddresses();
				System.out.println("Список доступных адресов:");
				for (String address : addresses) {
					System.out.println(address);
				}
				break;
			case 2:
				double balance = bitcoinRpcClient.getBalance();
				DecimalFormat df = new DecimalFormat("0.########");
				System.out.println("Balance: " + df.format(balance) + " BTC");
				break;
			default:
				System.out.println("Неверный выбор операции");
				break;
		}
	}
}
