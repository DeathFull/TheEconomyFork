package com.economy.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopTracker {

    private final Map<Integer, ShopItem> items;
    private int nextUniqueId;
    private java.util.List<String> tabs;

    public ShopTracker() {
        this.items = new HashMap<>();
        this.nextUniqueId = 1;
        this.tabs = new java.util.ArrayList<>();
    }

    public void addItem(ShopItem item) {
        if (item.getUniqueId() == 0) {
            item.setUniqueId(nextUniqueId++);
        } else {
            // Atualiza o próximo ID se necessário
            if (item.getUniqueId() >= nextUniqueId) {
                nextUniqueId = item.getUniqueId() + 1;
            }
        }
        items.put(item.getUniqueId(), item);
    }

    public boolean removeItem(int uniqueId) {
        ShopItem removed = items.remove(uniqueId);
        return removed != null;
    }

    public ShopItem getItem(int uniqueId) {
        return items.get(uniqueId);
    }

    public List<ShopItem> getAllItems() {
        return new ArrayList<>(items.values());
    }

    public boolean hasItem(int uniqueId) {
        return items.containsKey(uniqueId);
    }

    public int getNextUniqueId() {
        return nextUniqueId;
    }

    public void setNextUniqueId(int nextUniqueId) {
        this.nextUniqueId = nextUniqueId;
    }

    public java.util.List<String> getTabs() {
        return tabs;
    }

    public void setTabs(java.util.List<String> tabs) {
        this.tabs = tabs != null ? tabs : new java.util.ArrayList<>();
    }

    public void addTab(String tabName) {
        if (tabName != null && !tabName.isEmpty() && !tabs.contains(tabName)) {
            tabs.add(tabName);
        }
    }

    public boolean removeTab(String tabName) {
        if (tabName == null || tabName.isEmpty()) {
            return false;
        }
        // Remove todos os itens dessa tab
        items.values().removeIf(item -> tabName.equals(item.getTab()));
        return tabs.remove(tabName);
    }

    public boolean hasTab(String tabName) {
        return tabName != null && !tabName.isEmpty() && tabs.contains(tabName);
    }

    public List<ShopItem> getItemsByTab(String tabName) {
        List<ShopItem> result = new ArrayList<>();
        if (tabName == null || tabName.isEmpty()) {
            // Retorna itens sem tab (compatibilidade)
            for (ShopItem item : items.values()) {
                if (item.getTab() == null || item.getTab().isEmpty()) {
                    result.add(item);
                }
            }
        } else {
            for (ShopItem item : items.values()) {
                if (tabName.equals(item.getTab())) {
                    result.add(item);
                }
            }
        }
        return result;
    }
}

