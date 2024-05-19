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
		System.out.println("Выберите операцию:");
		System.out.println("1. Получить список адресов");
		System.out.println("2. Проверить баланс кошелька");
		System.out.println("3. Отправить средства");
		System.out.println("4. Получить новый адрес");
		int choice = scanner.nextInt();
		double amount = scanner.nextDouble();

		switch (choice) {
			case 1:
				List<String> addresses = bitcoinRpcClient.getAddresses();
				System.out.println("Список доступных адресов:");
				for (String address : addresses) {
					System.out.println(address);
				}
				break;
			case 2:
				System.out.println("Balance: " + df.format(balance) + " BTC");
				break;
			case 3:
				System.out.println("Введите адрес получателя:");
				String toAddress = scanner.next();
				System.out.println("Введите сумму для отправки:");
				String formattedAmount = df.format(amount);

				String formattedBalance = df.format(balance);
				if (formattedAmount.compareTo(formattedBalance) > 0) {
					System.out.println("Недостаточно средств для выполнения транзакции.");
					break;
				}

				String transactionId = bitcoinRpcClient.sendFunds(toAddress, amount);
				System.out.println("Средства успешно отправлены. ID транзакции: " + transactionId);
				break;
			case 4:
				System.out.println("Введите желаемую сумму:");
				String newAddress = bitcoinRpcClient.getNewAddressAndGenerateQRCode(amount);
				System.out.println("Ваш новый адрес: " + newAddress + ". QR-код с указанием суммы " + amount + " BTC был успешно сгенерирован!");
				break;
			default:
				System.out.println("Неверный выбор операции");
				break;
		}
	}
}
