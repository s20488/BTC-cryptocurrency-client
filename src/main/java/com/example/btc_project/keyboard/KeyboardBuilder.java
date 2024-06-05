package com.example.btc_project.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;

public class KeyboardBuilder {

    private final ReplyKeyboardMarkup keyboardMarkup;

    public KeyboardBuilder() {
        this.keyboardMarkup = new ReplyKeyboardMarkup();
        this.keyboardMarkup.setSelective(true);
        this.keyboardMarkup.setResizeKeyboard(true);
        this.keyboardMarkup.setOneTimeKeyboard(false);
        this.keyboardMarkup.setKeyboard(new ArrayList<>());
    }

    public KeyboardBuilder addRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        for (String button : buttons) {
            row.add(button);
        }
        this.keyboardMarkup.getKeyboard().add(row);
        return this;
    }

    public ReplyKeyboardMarkup build() {
        return this.keyboardMarkup;
    }
}
