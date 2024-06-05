package com.example.btc_project.configuration;

import com.example.btc_project.bot.BtcProjectBot;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BtcProjectConfiguration {

//    @Bean
//    public OkHttpClient okHttpClient() {
//        return new OkHttpClient();
//    }

    @Bean
    public TelegramBotsApi telegramBotsApi(BtcProjectBot btcProjectBot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(btcProjectBot);
        return api;
    }
}
