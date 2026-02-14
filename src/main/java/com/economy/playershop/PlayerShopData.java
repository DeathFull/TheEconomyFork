package com.economy.playershop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerShopData {
    private UUID uuid;
    private String nick;
    private boolean isOpen;
    private List<PlayerShopItem> items;

    public PlayerShopData() {
        this.uuid = null;
        this.nick = "";
        this.isOpen = false;
        this.items = new ArrayList<>();
    }

    public PlayerShopData(UUID uuid, String nick, boolean isOpen) {
        this.uuid = uuid;
        this.nick = nick != null ? nick : "";
        this.isOpen = isOpen;
        this.items = new ArrayList<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick != null ? nick : "";
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public List<PlayerShopItem> getItems() {
        return items;
    }

    public void setItems(List<PlayerShopItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }
}

