package com.economy.economy;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.UUID;

public class PlayerBalance {

    public static final BuilderCodec<PlayerBalance> CODEC = BuilderCodec.builder(PlayerBalance.class, PlayerBalance::new)
            .append(new KeyedCodec<>("UUID", Codec.UUID_STRING),
                    (balance, id, extraInfo) -> balance.uuid = id,
                    (balance, extraInfo) -> balance.uuid).add()
            .append(new KeyedCodec<>("Nick", Codec.STRING),
                    (balance, value, extraInfo) -> balance.nick = value != null ? value : "",
                    (balance, extraInfo) -> balance.nick).add()
            .append(new KeyedCodec<>("Balance", Codec.DOUBLE),
                    (balance, value, extraInfo) -> balance.balance = value,
                    (balance, extraInfo) -> balance.balance).add()
            .append(new KeyedCodec<>("Cash", Codec.INTEGER),
                    (balance, value, extraInfo) -> balance.cash = value != null ? value : 0,
                    (balance, extraInfo) -> balance.cash).add()
            .build();

    public static ArrayCodec<PlayerBalance> CODEC_ARRAY = new ArrayCodec<>(CODEC, PlayerBalance[]::new);

    private UUID uuid;
    private String nick;
    private double balance;
    private int cash = 0;

    public PlayerBalance(UUID uuid, double balance) {
        this.uuid = uuid;
        this.nick = "";
        this.balance = balance;
        this.cash = 0;
    }

    public PlayerBalance(UUID uuid, String nick, double balance) {
        this.uuid = uuid;
        this.nick = nick != null ? nick : "";
        this.balance = balance;
        this.cash = 0;
    }

    public PlayerBalance(UUID uuid, String nick, double balance, int cash) {
        this.uuid = uuid;
        this.nick = nick != null ? nick : "";
        this.balance = balance;
        this.cash = cash;
    }

    public PlayerBalance() {
        this(UUID.randomUUID(), 0.0);
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    public void subtractBalance(double amount) {
        this.balance -= amount;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick != null ? nick : "";
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public void addCash(int amount) {
        this.cash += amount;
    }

    public void subtractCash(int amount) {
        this.cash -= amount;
    }
}


