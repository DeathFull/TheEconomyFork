package com.economy.playershop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerShopTracker {
    private List<PlayerShopItem> items;
    private Map<UUID, PlayerShopPlayer> players;
    private Map<UUID, Boolean> shopOpenStatus;
    private Map<UUID, List<String>> tabs; // Tabs por jogador
    private int nextUniqueId;

    public PlayerShopTracker() {
        this.items = new ArrayList<>();
        this.players = new HashMap<>();
        this.shopOpenStatus = new HashMap<>();
        this.tabs = new HashMap<>();
        this.nextUniqueId = 1;
    }

    public PlayerShopItem addItem(PlayerShopItem item) {
        item.setUniqueId(nextUniqueId++);
        items.add(item);
        return item;
    }

    public boolean removeItem(int uniqueId) {
        return items.removeIf(item -> item.getUniqueId() == uniqueId);
    }

    public PlayerShopItem getItem(int uniqueId) {
        return items.stream()
                .filter(item -> item.getUniqueId() == uniqueId)
                .findFirst()
                .orElse(null);
    }

    public List<PlayerShopItem> getAllItems() {
        return new ArrayList<>(items);
    }

    public List<PlayerShopItem> getItemsByOwner(UUID ownerUuid) {
        List<PlayerShopItem> result = new ArrayList<>();
        for (PlayerShopItem item : items) {
            if (item.getOwnerUuid() != null && item.getOwnerUuid().equals(ownerUuid)) {
                result.add(item);
            }
        }
        return result;
    }

    public boolean hasItem(int uniqueId) {
        return items.stream().anyMatch(item -> item.getUniqueId() == uniqueId);
    }

    public int getNextUniqueId() {
        return nextUniqueId;
    }

    public void setNextUniqueId(int nextUniqueId) {
        this.nextUniqueId = nextUniqueId;
    }

    public List<PlayerShopItem> getItems() {
        return items;
    }

    public void setItems(List<PlayerShopItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public Map<UUID, PlayerShopPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(Map<UUID, PlayerShopPlayer> players) {
        this.players = players != null ? new HashMap<>(players) : new HashMap<>();
    }

    public void addOrUpdatePlayer(UUID uuid, String nick) {
        if (uuid != null) {
            PlayerShopPlayer existingPlayer = players.get(uuid);
            if (existingPlayer != null) {
                // Preserva customName e shopIcon existentes, apenas atualiza o nick
                existingPlayer.setNick(nick != null ? nick : "");
            } else {
                // Cria novo jogador apenas se não existir
                players.put(uuid, new PlayerShopPlayer(uuid, nick != null ? nick : ""));
            }
        }
    }

    public PlayerShopPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public List<PlayerShopPlayer> getAllPlayers() {
        return new ArrayList<>(players.values());
    }

    public boolean isShopOpen(UUID uuid) {
        return shopOpenStatus.getOrDefault(uuid, false);
    }

    public void setShopOpen(UUID uuid, boolean open) {
        if (uuid != null) {
            shopOpenStatus.put(uuid, open);
        }
    }

    public Map<UUID, Boolean> getShopOpenStatus() {
        return shopOpenStatus;
    }

    public void setShopOpenStatus(Map<UUID, Boolean> shopOpenStatus) {
        this.shopOpenStatus = shopOpenStatus != null ? new HashMap<>(shopOpenStatus) : new HashMap<>();
    }
    
    // Métodos para gerenciar tabs
    public List<String> getTabs(UUID ownerUuid) {
        return tabs.getOrDefault(ownerUuid, new ArrayList<>());
    }
    
    public void addTab(UUID ownerUuid, String tabName) {
        if (tabName != null && !tabName.isEmpty() && ownerUuid != null) {
            tabs.computeIfAbsent(ownerUuid, k -> new ArrayList<>());
            List<String> ownerTabs = tabs.get(ownerUuid);
            if (!ownerTabs.contains(tabName)) {
                ownerTabs.add(tabName);
            }
        }
    }
    
    public boolean removeTab(UUID ownerUuid, String tabName) {
        if (tabName == null || tabName.isEmpty() || ownerUuid == null) {
            return false;
        }
        List<String> ownerTabs = tabs.get(ownerUuid);
        if (ownerTabs == null) {
            return false;
        }
        // Remove todos os itens dessa tab
        items.removeIf(item -> ownerUuid.equals(item.getOwnerUuid()) && tabName.equals(item.getTab()));
        return ownerTabs.remove(tabName);
    }
    
    public boolean hasTab(UUID ownerUuid, String tabName) {
        if (ownerUuid == null || tabName == null || tabName.isEmpty()) {
            return false;
        }
        List<String> ownerTabs = tabs.get(ownerUuid);
        return ownerTabs != null && ownerTabs.contains(tabName);
    }
    
    public List<PlayerShopItem> getItemsByTab(UUID ownerUuid, String tabName) {
        List<PlayerShopItem> result = new ArrayList<>();
        if (tabName == null || tabName.isEmpty()) {
            // Retorna itens sem tab (compatibilidade)
            for (PlayerShopItem item : items) {
                if (ownerUuid.equals(item.getOwnerUuid()) && (item.getTab() == null || item.getTab().isEmpty())) {
                    result.add(item);
                }
            }
        } else {
            for (PlayerShopItem item : items) {
                if (ownerUuid.equals(item.getOwnerUuid()) && tabName.equals(item.getTab())) {
                    result.add(item);
                }
            }
        }
        return result;
    }
}

