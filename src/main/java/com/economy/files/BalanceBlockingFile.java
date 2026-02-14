package com.economy.files;

import com.economy.economy.BalanceTracker;
import com.economy.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class BalanceBlockingFile extends BlockingDiskFile {

    private BalanceTracker tracker;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public BalanceBlockingFile() {
        super(Path.of(FileUtils.BALANCES_PATH));
        this.tracker = new BalanceTracker();
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        var rootElement = JsonParser.parseReader(bufferedReader);
        if (rootElement == null || !rootElement.isJsonObject()) return;
        var root = rootElement.getAsJsonObject();
        JsonArray valuesArray = root.getAsJsonArray("Values");
        if (valuesArray == null) return;
        this.tracker = new BalanceTracker();
        valuesArray.forEach(jsonElement -> {
            JsonObject balanceObj = jsonElement.getAsJsonObject();
            UUID uuid = UUID.fromString(balanceObj.get("UUID").getAsString());
            double balance = balanceObj.get("Balance").getAsDouble();
            int cash = balanceObj.has("Cash") && !balanceObj.get("Cash").isJsonNull() 
                    ? balanceObj.get("Cash").getAsInt() : 0;
            String nick = balanceObj.has("Nick") && !balanceObj.get("Nick").isJsonNull() 
                    ? balanceObj.get("Nick").getAsString() : "";
            this.tracker.setBalance(uuid, balance);
            this.tracker.setCash(uuid, cash);
            if (!nick.isEmpty()) {
                this.tracker.setPlayerNick(uuid, nick);
            }
        });
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray valuesArray = new JsonArray();
        for (com.economy.economy.PlayerBalance balance : this.tracker.getBalances()) {
            JsonObject balanceObj = new JsonObject();
            balanceObj.addProperty("UUID", balance.getUuid().toString());
            balanceObj.addProperty("Nick", balance.getNick() != null ? balance.getNick() : "");
            balanceObj.addProperty("Balance", balance.getBalance());
            balanceObj.addProperty("Cash", balance.getCash());
            valuesArray.add(balanceObj);
        }
        root.add("Values", valuesArray);
        // Usa Gson para formatar o JSON de forma legível
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        this.tracker = new BalanceTracker();
        write(bufferedWriter);
    }

    public BalanceTracker getTracker() {
        return tracker;
    }
}

