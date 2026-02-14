package com.economy.files;

import com.economy.shop.ShopItem;
import com.economy.shop.ShopTracker;
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

/**
 * Arquivo de armazenamento JSON para lojas de NPCs (shopId > 0)
 */
public class ShopNpcBlockingFile extends BlockingDiskFile {

    private ShopTracker tracker;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ShopNpcBlockingFile(int shopId) {
        super(Path.of(FileUtils.MAIN_PATH + java.io.File.separator + "shop_npc_" + shopId + ".json"));
        this.tracker = new ShopTracker();
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        var rootElement = JsonParser.parseReader(bufferedReader);
        if (rootElement == null || !rootElement.isJsonObject()) return;
        var root = rootElement.getAsJsonObject();
        
        // Lê o próximo ID único
        if (root.has("NextUniqueId")) {
            this.tracker.setNextUniqueId(root.get("NextUniqueId").getAsInt());
        }
        
        JsonArray itemsArray = root.getAsJsonArray("Items");
        if (itemsArray == null) {
            // Se não tiver items, inicializa tracker vazio
            this.tracker = new ShopTracker();
            if (root.has("NextUniqueId")) {
                this.tracker.setNextUniqueId(root.get("NextUniqueId").getAsInt());
            }
            // Lê tabs se existir
            if (root.has("Tabs")) {
                JsonArray tabsArray = root.getAsJsonArray("Tabs");
                if (tabsArray != null) {
                    tabsArray.forEach(jsonElement -> {
                        if (jsonElement.isJsonPrimitive()) {
                            this.tracker.addTab(jsonElement.getAsString());
                        }
                    });
                }
            }
            return;
        }
        
        this.tracker = new ShopTracker();
        if (root.has("NextUniqueId")) {
            this.tracker.setNextUniqueId(root.get("NextUniqueId").getAsInt());
        }
        
        // Lê tabs se existir
        if (root.has("Tabs")) {
            JsonArray tabsArray = root.getAsJsonArray("Tabs");
            if (tabsArray != null) {
                tabsArray.forEach(jsonElement -> {
                    if (jsonElement.isJsonPrimitive()) {
                        this.tracker.addTab(jsonElement.getAsString());
                    }
                });
            }
        }
        
        itemsArray.forEach(jsonElement -> {
            JsonObject itemObj = jsonElement.getAsJsonObject();
            ShopItem item = new ShopItem();
            item.setUniqueId(itemObj.get("UniqueId").getAsInt());
            item.setItemId(itemObj.get("ItemId").getAsString());
            item.setQuantity(itemObj.get("Quantity").getAsInt());
            item.setPriceSell(itemObj.get("PriceSell").getAsDouble());
            item.setPriceBuy(itemObj.get("PriceBuy").getAsDouble());
            // Lê tab se existir
            if (itemObj.has("Tab")) {
                item.setTab(itemObj.get("Tab").getAsString());
            } else {
                item.setTab("");
            }
            // Lê campos de comando console se existirem
            if (itemObj.has("IsConsoleCommand")) {
                item.setConsoleCommand(itemObj.get("IsConsoleCommand").getAsBoolean());
            }
            if (itemObj.has("ConsoleCommand")) {
                item.setConsoleCommand(itemObj.get("ConsoleCommand").getAsString());
            }
            if (itemObj.has("DisplayName")) {
                item.setDisplayName(itemObj.get("DisplayName").getAsString());
            }
            if (itemObj.has("UseCash")) {
                item.setUseCash(itemObj.get("UseCash").getAsBoolean());
            }
            this.tracker.addItem(item);
        });
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("NextUniqueId", this.tracker.getNextUniqueId());
        
        JsonArray itemsArray = new JsonArray();
        for (ShopItem item : this.tracker.getAllItems()) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("UniqueId", item.getUniqueId());
            itemObj.addProperty("ItemId", item.getItemId());
            itemObj.addProperty("Quantity", item.getQuantity());
            itemObj.addProperty("PriceSell", item.getPriceSell());
            itemObj.addProperty("PriceBuy", item.getPriceBuy());
            itemObj.addProperty("Tab", item.getTab());
            itemObj.addProperty("IsConsoleCommand", item.isConsoleCommand());
            itemObj.addProperty("ConsoleCommand", item.getConsoleCommand());
            itemObj.addProperty("DisplayName", item.getDisplayName());
            itemObj.addProperty("UseCash", item.isUseCash());
            itemsArray.add(itemObj);
        }
        root.add("Items", itemsArray);
        
        // Adiciona lista de tabs
        JsonArray tabsArray = new JsonArray();
        for (String tab : this.tracker.getTabs()) {
            tabsArray.add(tab);
        }
        root.add("Tabs", tabsArray);
        
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        this.tracker = new ShopTracker();
        // Inicializa com estrutura básica incluindo Tabs vazio
        JsonObject root = new JsonObject();
        root.addProperty("NextUniqueId", 1);
        root.add("Items", new JsonArray());
        root.add("Tabs", new JsonArray());
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    public ShopTracker getTracker() {
        return tracker;
    }
}

