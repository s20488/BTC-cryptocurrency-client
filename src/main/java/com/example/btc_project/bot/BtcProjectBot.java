package com.example.btc_project.bot;

import com.example.btc_project.service.impl.BtcProjectServiceImpl;
import com.example.btc_project.keyboard.KeyboardBuilder;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

@Component
public class BtcProjectBot extends TelegramLongPollingBot {

	private static final String START = "/start";
	private static final String BALANCE = "/balance";
	private static final String ADDRESSES = "/addresses";
	private static final String SEND = "/send";
	private static final String RECEIVE = "/receive";
	private static final String HISTORY = "/history";
	private static final String HELP = "/help";

	@Autowired
	private BtcProjectServiceImpl btcProjectServiceImpl;

	public BtcProjectBot(@Value("${bot.token}") String botToken) {
		super(botToken);
	}

	@Override
	public String getBotUsername() {
		return "bitcorewalletbot";
	}

	@Override
	public void onUpdateReceived(Update update) {
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}
		var message = update.getMessage().getText();
		var chatId = update.getMessage().getChatId();

		switch (message) {
			case START, "0" -> startCommand(chatId);
			case BALANCE, "1" -> balanceCommand(chatId);
			case ADDRESSES, "2" -> addressesCommand(chatId);
			case SEND, "3" -> sendCommand(chatId);
			case RECEIVE, "4" -> receiveCommand(chatId);
			case HISTORY, "5" -> historyCommand(chatId);
			case HELP, "6" -> helpCommand(chatId);
			default -> {
				if (message.matches("\\d+")) {
					unknownCommand(chatId);
				} else {
					String[] parts = message.split(" ");
					if (parts.length == 2) {
						try {
							String toAddress = parts[0];
							double amount = Double.parseDouble(parts[1]);
							int feeRate = 25;
							boolean subtractFeeFromAmount = true;
							String transactionId = btcProjectServiceImpl.sendFunds(toAddress, amount, feeRate, subtractFeeFromAmount);
							var text = "Funds sent successfully. Transaction ID: " + transactionId;
							sendMessage(chatId, text);
						} catch (NumberFormatException e) {
							unknownCommand(chatId);
						} catch (IOException e) {
							e.printStackTrace();
							var text = "Failed to execute the transaction. Please try again later.";
							sendMessage(chatId, text);
						}
					} else {
						try {
							double amount = Double.parseDouble(message);
							double balance = btcProjectServiceImpl.getBalance();
							if (amount > balance) {
								var text = "Insufficient funds on the balance. Try again.";
								sendMessage(chatId, text);
							} else {
								BufferedImage qrCode = btcProjectServiceImpl.getNewAddressAndGenerateQRCode(amount);
								sendQrCodeImage(chatId, qrCode, amount);
							}
						} catch (NumberFormatException | IOException | WriterException e) {
							e.printStackTrace();
							var text = "Failed to execute the transaction. Please try again later.";
							sendMessage(chatId, text);
						}
                    }
				}
			}
		}
	}

	private void startCommand(Long chatId) {
		var text =
				"Choose operation:\n" +
						"1. Balance\n" +
						"2. Addresses\n" +
						"3. Send\n" +
						"4. Receive\n" +
						"5. Transaction history\n" +
		                "6. Helpdesk";

		SendMessage message = new SendMessage(String.valueOf(chatId), text);

		KeyboardBuilder keyboardBuilder = new KeyboardBuilder();
		keyboardBuilder.addRow("1", "2", "3", "4", "5", "6");

		message.setReplyMarkup(keyboardBuilder.build());

		try {
			execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	private void balanceCommand(Long chatId) {
		try {
			double balance = btcProjectServiceImpl.getBalance();
			DecimalFormat df = new DecimalFormat("0.########");
			String balanceText = "Balance: " + df.format(balance) + " BTC";
			sendMessage(chatId, balanceText);
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(chatId, "Failed to retrieve balance. Please try again later.");
		}
	}

	private void addressesCommand(Long chatId) {
		try {
			List<Map<String, String>> addresses = btcProjectServiceImpl.getAddresses();
			DecimalFormat df = new DecimalFormat("0.########");

			StringBuilder response = new StringBuilder("Addresses:\n");
			for (Map<String, String> addressInfo : addresses) {
				String address = addressInfo.get("address");
				String type = addressInfo.get("type");
				double balanceAddress = btcProjectServiceImpl.getReceivedByAddress(address);
				response.append("Address: ").append(address).append(", Type: ").append(type)
						.append(", Balance: ").append(df.format(balanceAddress)).append(" BTC\n");
			}
			sendMessage(chatId, response.toString());
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(chatId, "Failed to retrieve addresses. Please try again later.");
		}
	}

	private void sendCommand(Long chatId) {
		var text = "Enter the recipient's address and the amount separated by a space:";
		var sendMessage = new SendMessage(String.valueOf(chatId), text);
		try {
			execute(sendMessage);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	private void receiveCommand(Long chatId) {
		var text = "Enter the desired amount:";
		var sendMessage = new SendMessage(String.valueOf(chatId), text);
		try {
			execute(sendMessage);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	private void sendQrCodeImage(Long chatId, BufferedImage qrCode, double amount) {
		try {
			DecimalFormat df = new DecimalFormat("0.########");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(qrCode, "PNG", baos);
			baos.flush();
			byte[] imageBytes = baos.toByteArray();
			baos.close();

			InputFile inputFile = new InputFile(new ByteArrayInputStream(imageBytes), "QRCode.png");
			SendPhoto sendPhoto = new SendPhoto();
			sendPhoto.setChatId(chatId.toString());
			sendPhoto.setPhoto(inputFile);
			sendPhoto.setCaption("Your new address. QR code with the amount " + df.format(amount) + " BTC was successfully generated!");

			execute(sendPhoto);
		} catch (IOException | TelegramApiException e) {
			e.printStackTrace();
			var text = "Failed to send QR code image. Please try again later.";
			sendMessage(chatId, text);
		}
	}

	private void historyCommand(Long chatId) {
		try {
			List<Map<String, Object>> transactionsWithStatus = btcProjectServiceImpl.getTransactionHistory();
			DecimalFormat df = new DecimalFormat("0.########");

			StringBuilder response = new StringBuilder("Transaction History:\n");
			for (Map<String, Object> transaction : transactionsWithStatus) {
				String time = transaction.get("time").toString();
				double amountTransaction = Double.parseDouble(transaction.get("amount").toString());
				String category = transaction.get("category").toString();
				String status = transaction.get("status").toString();
				response.append("Time: ").append(time).append(", Amount: ").append(df.format(amountTransaction))
						.append(", Category: ").append(category).append(", Status: ").append(status).append("\n");
			}
			sendMessage(chatId, response.toString());
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(chatId, "Failed to retrieve transaction history. Please try again later.");
		}
	}

	private void helpCommand(Long chatId) {
		var text = """
				Background information on the bot
				                
				To get the current information about the wallet, use the commands:
				/start - options menu output
				/balance - balance check
				/addresses - available address list
				/send - sending funds
				/receive - funds request 
				/history - previous transaction histories
				/help - command description information
				""";
		sendMessage(chatId, text);
	}

	private void unknownCommand(Long chatId) {
		var text = "Failed to recognize the command!";
		sendMessage(chatId, text);
	}

	private void sendMessage(Long chatId, String text) {
		var chatIdStr = String.valueOf(chatId);
		var sendMessage = new SendMessage(chatIdStr, text);
		try {
			execute(sendMessage);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
