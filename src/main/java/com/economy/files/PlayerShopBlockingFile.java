package com.economy.files;

import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopPlayer;
import com.economy.playershop.PlayerShopTracker;
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

public class PlayerShopBlockingFile extends BlockingDiskFile {

    private PlayerShopTracker tracker;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PlayerShopBlockingFile() {
        super(Path.of(FileUtils.PLAYER_SHOP_PATH));
        this.tracker = new PlayerShopTracker();
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        var rootElement = JsonParser.parseReader(bufferedReader);
        if (rootElement == null || !rootElement.isJsonObject()) return;
        var root = rootElement.getAsJsonObject();
        
        this.tracker = new PlayerShopTracker();
        
        // Lê o próximo ID único
        if (root.has("NextUniqueId")) {
            this.tracker.setNextUniqueId(root.get("NextUniqueId").getAsInt());
        }
        
        // Nova estrutura: "Shops" array com uuid, nick, isOpen e Items
        if (root.has("Shops")) {
            JsonArray shopsArray = root.getAsJsonArray("Shops");
            if (shopsArray != null) {
                shopsArray.forEach(jsonElement -> {
                    JsonObject shopObj = jsonElement.getAsJsonObject();
                    if (shopObj.has("uuid") && !shopObj.get("uuid").isJsonNull()) {
                        try {
                            UUID uuid = UUID.fromString(shopObj.get("uuid").getAsString());
                            String nick = shopObj.has("nick") ? shopObj.get("nick").getAsString() : "";
                            String customName = shopObj.has("customName") ? shopObj.get("customName").getAsString() : "";
                            String shopIcon = shopObj.has("shopIcon") ? shopObj.get("shopIcon").getAsString() : "";
                            boolean isOpen = shopObj.has("isOpen") && shopObj.get("isOpen").getAsBoolean();
                            
                            // Obtém ou cria o player, preservando valores existentes
                            com.economy.playershop.PlayerShopPlayer player = this.tracker.getPlayer(uuid);
                            if (player == null) {
                                // Se não existe, cria um novo
                                player = new com.economy.playershop.PlayerShopPlayer(uuid, nick);
                                this.tracker.getPlayers().put(uuid, player);
                            } else {
                                // Se existe, atualiza apenas o nick (preserva customName e shopIcon)
                                player.setNick(nick);
                            }
                            
                            // Define o nome personalizado e ícone se existirem
                            if (!customName.isEmpty()) {
                                player.setCustomName(customName);
                            }
                            if (!shopIcon.isEmpty()) {
                                player.setShopIcon(shopIcon);
                            }
                            
                            // Define o status de abertura
                            this.tracker.setShopOpen(uuid, isOpen);
                            
                            // Lê as tabs desta loja
                            if (shopObj.has("Tabs")) {
                                JsonArray tabsArray = shopObj.getAsJsonArray("Tabs");
                                if (tabsArray != null) {
                                    tabsArray.forEach(tabElement -> {
                                        if (tabElement.isJsonPrimitive()) {
                                            String tabName = tabElement.getAsString();
                                            if (tabName != null && !tabName.isEmpty()) {
                                                this.tracker.addTab(uuid, tabName);
                                            }
                                        }
                                    });
                                }
                            }
                            
                            // Lê os itens desta loja
                            if (shopObj.has("Items")) {
                                JsonArray itemsArray = shopObj.getAsJsonArray("Items");
                                if (itemsArray != null) {
                                    itemsArray.forEach(itemElement -> {
                                        JsonObject itemObj = itemElement.getAsJsonObject();
                                        PlayerShopItem item = new PlayerShopItem();
                                        item.setUniqueId(itemObj.get("UniqueId").getAsInt());
                                        item.setItemId(itemObj.get("ItemId").getAsString());
                                        item.setQuantity(itemObj.has("Quantity") ? itemObj.get("Quantity").getAsInt() : 1);
                                        item.setPriceBuy(itemObj.get("PriceBuy").getAsDouble());
                                        
                                        if (itemObj.has("PriceSell")) {
                                            item.setPriceSell(itemObj.get("PriceSell").getAsDouble());
                                        } else {
                                            item.setPriceSell(0.0);
                                        }
                                        
                                        item.setOwnerUuid(uuid);
                                        
                                        if (itemObj.has("Durability")) {
                                            item.setDurability(itemObj.get("Durability").getAsDouble());
                                        }
                                        
                                        if (itemObj.has("MaxDurability")) {
                                            item.setMaxDurability(itemObj.get("MaxDurability").getAsDouble());
                                        }
                                        
                                        if (itemObj.has("Stock")) {
                                            item.setStock(itemObj.get("Stock").getAsInt());
                                        }
                                        
                                        if (itemObj.has("Tab")) {
                                            item.setTab(itemObj.get("Tab").getAsString());
                                        }
                                        
                                        this.tracker.getItems().add(item);
                                    });
                                }
                            }
                        } catch (Exception e) {
                            // Ignora UUID inválido
                        }
                    }
                });
            }
        } else {
            // Compatibilidade com formato antigo
            // Lê informações dos jogadores
            if (root.has("Players")) {
                JsonArray playersArray = root.getAsJsonArray("Players");
                if (playersArray != null) {
                    playersArray.forEach(jsonElement -> {
                        JsonObject playerObj = jsonElement.getAsJsonObject();
                        if (playerObj.has("Uuid") && !playerObj.get("Uuid").isJsonNull()) {
                            try {
                                UUID uuid = UUID.fromString(playerObj.get("Uuid").getAsString());
                                String nick = playerObj.has("Nick") ? playerObj.get("Nick").getAsString() : "";
                                this.tracker.addOrUpdatePlayer(uuid, nick);
                            } catch (Exception e) {
                                // Ignora UUID inválido
                            }
                        }
                    });
                }
            }
            
            JsonArray itemsArray = root.getAsJsonArray("Items");
            if (itemsArray != null) {
                itemsArray.forEach(jsonElement -> {
                    JsonObject itemObj = jsonElement.getAsJsonObject();
                    PlayerShopItem item = new PlayerShopItem();
                    item.setUniqueId(itemObj.get("UniqueId").getAsInt());
                    item.setItemId(itemObj.get("ItemId").getAsString());
                    item.setQuantity(itemObj.get("Quantity").getAsInt());
                    item.setPriceBuy(itemObj.get("PriceBuy").getAsDouble());
                    
                    if (itemObj.has("PriceSell")) {
                        item.setPriceSell(itemObj.get("PriceSell").getAsDouble());
                    } else {
                        item.setPriceSell(0.0);
                    }
                    
                    if (itemObj.has("OwnerUuid") && !itemObj.get("OwnerUuid").isJsonNull()) {
                        try {
                            item.setOwnerUuid(UUID.fromString(itemObj.get("OwnerUuid").getAsString()));
                        } catch (Exception e) {
                            item.setOwnerUuid(null);
                        }
                    }
                    
                    if (itemObj.has("Durability")) {
                        item.setDurability(itemObj.get("Durability").getAsDouble());
                    }
                    
                    if (itemObj.has("MaxDurability")) {
                        item.setMaxDurability(itemObj.get("MaxDurability").getAsDouble());
                    }
                    
                    if (itemObj.has("Stock")) {
                        item.setStock(itemObj.get("Stock").getAsInt());
                    }
                    
                    if (itemObj.has("Tab")) {
                        item.setTab(itemObj.get("Tab").getAsString());
                    }
                    
                    this.tracker.getItems().add(item);
                });
            }
        }
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("NextUniqueId", this.tracker.getNextUniqueId());
        
        // Nova estrutura: agrupa por jogador
        JsonArray shopsArray = new JsonArray();
        
        // Agrupa itens por dono
        java.util.Map<UUID, java.util.List<PlayerShopItem>> itemsByOwner = new java.util.HashMap<>();
        for (PlayerShopItem item : this.tracker.getAllItems()) {
            if (item.getOwnerUuid() != null) {
                itemsByOwner.computeIfAbsent(item.getOwnerUuid(), k -> new java.util.ArrayList<>()).add(item);
            }
        }
        
        // Cria entrada para cada jogador que tem loja
        java.util.Set<UUID> processedOwners = new java.util.HashSet<>();
        
        // Processa jogadores que têm itens
        for (UUID ownerUuid : itemsByOwner.keySet()) {
            if (processedOwners.contains(ownerUuid)) continue;
            processedOwners.add(ownerUuid);
            
            JsonObject shopObj = new JsonObject();
            shopObj.addProperty("uuid", ownerUuid.toString());
            
            PlayerShopPlayer player = this.tracker.getPlayer(ownerUuid);
            String nick = player != null ? player.getNick() : "";
            shopObj.addProperty("nick", nick);
            
            // Salva o nome personalizado se existir
            String customName = player != null ? player.getCustomName() : "";
            if (customName != null && !customName.isEmpty()) {
                shopObj.addProperty("customName", customName);
            }
            
            // Salva o ícone da loja se existir
            String shopIcon = player != null ? player.getShopIcon() : "";
            if (shopIcon != null && !shopIcon.isEmpty()) {
                shopObj.addProperty("shopIcon", shopIcon);
            }
            
            // Salva o status de abertura
            boolean isOpen = this.tracker.isShopOpen(ownerUuid);
            shopObj.addProperty("isOpen", isOpen);
            
            // Salva as tabs deste jogador
            java.util.List<String> tabs = this.tracker.getTabs(ownerUuid);
            if (tabs != null && !tabs.isEmpty()) {
                JsonArray tabsArray = new JsonArray();
                for (String tab : tabs) {
                    tabsArray.add(tab);
                }
                shopObj.add("Tabs", tabsArray);
            }
            
            // Adiciona os itens deste jogador
            JsonArray itemsArray = new JsonArray();
            for (PlayerShopItem item : itemsByOwner.get(ownerUuid)) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("UniqueId", item.getUniqueId());
                itemObj.addProperty("ItemId", item.getItemId());
                itemObj.addProperty("PriceBuy", item.getPriceBuy());
                itemObj.addProperty("PriceSell", item.getPriceSell());
                itemObj.addProperty("Durability", item.getDurability());
                itemObj.addProperty("MaxDurability", item.getMaxDurability());
                itemObj.addProperty("Stock", item.getStock());
                if (item.getTab() != null && !item.getTab().isEmpty()) {
                    itemObj.addProperty("Tab", item.getTab());
                }
                itemsArray.add(itemObj);
            }
            shopObj.add("Items", itemsArray);
            
            shopsArray.add(shopObj);
        }
        
        // Adiciona jogadores que não têm itens mas têm loja registrada
        for (PlayerShopPlayer player : this.tracker.getAllPlayers()) {
            if (player.getUuid() != null && !processedOwners.contains(player.getUuid())) {
                JsonObject shopObj = new JsonObject();
                shopObj.addProperty("uuid", player.getUuid().toString());
                shopObj.addProperty("nick", player.getNick());
                
                // Salva o nome personalizado se existir
                String customName = player.getCustomName();
                if (customName != null && !customName.isEmpty()) {
                    shopObj.addProperty("customName", customName);
                }
                
                // Salva o ícone da loja se existir
                String shopIcon = player.getShopIcon();
                if (shopIcon != null && !shopIcon.isEmpty()) {
                    shopObj.addProperty("shopIcon", shopIcon);
                }
                
                boolean isOpen = this.tracker.isShopOpen(player.getUuid());
                shopObj.addProperty("isOpen", isOpen);
                
                // Salva as tabs deste jogador
                java.util.List<String> tabs = this.tracker.getTabs(player.getUuid());
                if (tabs != null && !tabs.isEmpty()) {
                    JsonArray tabsArray = new JsonArray();
                    for (String tab : tabs) {
                        tabsArray.add(tab);
                    }
                    shopObj.add("Tabs", tabsArray);
                }
                
                shopObj.add("Items", new JsonArray());
                shopsArray.add(shopObj);
            }
        }
        
        root.add("Shops", shopsArray);
        
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        this.tracker = new PlayerShopTracker();
        JsonObject root = new JsonObject();
        root.addProperty("NextUniqueId", 1);
        root.add("Shops", new JsonArray());
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    public PlayerShopTracker getTracker() {
        return tracker;
    }
}

