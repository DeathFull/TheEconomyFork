package com.economy.commands;

import com.economy.util.LanguageManager;
import com.hypixel.hytale.server.core.Message;

import java.awt.*;

public class CommandMessages {

    // Permissões de jogador
    public static final String PERMISSION_PLAYER_MONEY = "theeconomy.player.money";
    public static final String PERMISSION_PLAYER_MONEY_PAY = "theeconomy.player.money.pay";
    public static final String PERMISSION_PLAYER_MONEY_TOP = "theeconomy.player.money.top";
    public static final String PERMISSION_PLAYER_SHOP = "theeconomy.player.shop";
    public static final String PERMISSION_PLAYER_ITEMINFO = "theeconomy.player.iteminfo";
    public static final String PERMISSION_PLAYER_MYSHOP = "theeconomy.player.myshop";
    public static final String PERMISSION_PLAYER_MYSHOP_MANAGER = "theeconomy.player.myshop.manager";
    public static final String PERMISSION_PLAYER_PLAYERSHOP = "theeconomy.player.playershop";
    public static final String PERMISSION_PLAYER_SHOPS = "theeconomy.player.shops";
    public static final String PERMISSION_PLAYER_HUD = "theeconomy.player.hud";
    public static final String PERMISSION_PLAYER_CASH = "theeconomy.player.cash";
    
    // Permissões de admin
    public static final String PERMISSION_ADMIN_MONEY_SET = "theeconomy.admin.money.set";
    public static final String PERMISSION_ADMIN_MONEY_GIVE = "theeconomy.admin.money.give";
    public static final String PERMISSION_ADMIN_CASH_GIVE = "theeconomy.admin.cash.give";
    public static final String PERMISSION_ADMIN_SHOP_ADD = "theeconomy.admin.shop.add";
    public static final String PERMISSION_ADMIN_SHOP_REMOVE = "theeconomy.admin.shop.remove";
    public static final String PERMISSION_ADMIN_SHOP_MANAGER = "theeconomy.admin.shop.manager";
    public static final String PERMISSION_ADMIN_SHOP_RENAME_PLAYER = "theeconomy.admin.shop.rename.player";

    public static Message INSUFFICIENT_BALANCE() {
        return LanguageManager.getMessage("chat_insufficient_balance", Color.RED);
    }

    public static Message PLAYER_NOT_FOUND() {
        return LanguageManager.getMessage("chat_player_not_found", Color.RED);
    }

    public static Message INVALID_AMOUNT() {
        return LanguageManager.getMessage("chat_invalid_amount", Color.RED);
    }

    public static Message CANNOT_PAY_YOURSELF() {
        return LanguageManager.getMessage("chat_cannot_pay_yourself", Color.RED);
    }

    public static Message PAYMENT_SENT() {
        return LanguageManager.getMessage("chat_payment_sent", Color.GREEN);
    }

    public static Message PAYMENT_RECEIVED() {
        return LanguageManager.getMessage("chat_payment_received", Color.GREEN);
    }

    public static Message BALANCE_SET() {
        return LanguageManager.getMessage("chat_balance_set", Color.GREEN);
    }

    public static Message BALANCE_ADDED() {
        return LanguageManager.getMessage("chat_balance_added", Color.GREEN);
    }

    public static Message USAGE_MONEY() {
        return LanguageManager.getMessage("chat_usage_money", Color.YELLOW);
    }

    public static Message USAGE_PAY() {
        return LanguageManager.getMessage("chat_usage_pay", Color.YELLOW);
    }

    public static Message USAGE_SET() {
        return LanguageManager.getMessage("chat_usage_set", Color.YELLOW);
    }

    public static Message USAGE_GIVE() {
        return LanguageManager.getMessage("chat_usage_give", Color.YELLOW);
    }

    public static Message USAGE_CASH_GIVE() {
        return LanguageManager.getMessage("chat_usage_cash_give", Color.YELLOW);
    }

    public static Message CASH_ADDED() {
        return LanguageManager.getMessage("chat_cash_added", Color.GREEN);
    }

    public static Message NO_PERMISSION() {
        return LanguageManager.getMessage("chat_no_permission", Color.RED);
    }

    public static Message INVALID_ARGUMENTS() {
        return LanguageManager.getMessage("chat_invalid_arguments", Color.RED);
    }
}


