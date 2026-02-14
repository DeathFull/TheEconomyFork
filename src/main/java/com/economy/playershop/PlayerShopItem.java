package com.economy.playershop;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;

import java.util.UUID;

public class PlayerShopItem {

    public static final BuilderCodec<PlayerShopItem> CODEC = BuilderCodec.builder(PlayerShopItem.class, PlayerShopItem::new)
            .append(new KeyedCodec<>("UniqueId", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.uniqueId = value != null ? value.intValue() : 0,
                    (item, extraInfo) -> (double) item.uniqueId).add()
            .append(new KeyedCodec<>("ItemId", Codec.STRING),
                    (item, value, extraInfo) -> item.itemId = value != null ? value : "",
                    (item, extraInfo) -> item.itemId).add()
            .append(new KeyedCodec<>("Quantity", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.quantity = value != null ? value.intValue() : 1,
                    (item, extraInfo) -> (double) item.quantity).add()
            .append(new KeyedCodec<>("PriceBuy", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.priceBuy = value != null ? value : 0.0,
                    (item, extraInfo) -> item.priceBuy).add()
            .append(new KeyedCodec<>("PriceSell", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.priceSell = value != null ? value : 0.0,
                    (item, extraInfo) -> item.priceSell).add()
            .append(new KeyedCodec<>("OwnerUuid", Codec.STRING),
                    (item, value, extraInfo) -> {
                        if (value != null && !value.isEmpty()) {
                            try {
                                item.ownerUuid = UUID.fromString(value);
                            } catch (Exception e) {
                                item.ownerUuid = null;
                            }
                        } else {
                            item.ownerUuid = null;
                        }
                    },
                    (item, extraInfo) -> item.ownerUuid != null ? item.ownerUuid.toString() : "").add()
            .append(new KeyedCodec<Double>("Durability", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.durability = value != null ? value : 0.0,
                    (item, extraInfo) -> item.durability).add()
            .append(new KeyedCodec<Double>("MaxDurability", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.maxDurability = value != null ? value : 0.0,
                    (item, extraInfo) -> item.maxDurability).add()
            .append(new KeyedCodec<Double>("Stock", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.stock = value != null ? value.intValue() : 0,
                    (item, extraInfo) -> (double) item.stock).add()
            .append(new KeyedCodec<>("Tab", Codec.STRING),
                    (item, value, extraInfo) -> item.tab = value != null ? value : "",
                    (item, extraInfo) -> item.tab != null ? item.tab : "").add()
            .build();

    private int uniqueId;
    private String itemId;
    private int quantity;
    private double priceBuy;
    private double priceSell;
    private UUID ownerUuid;
    private double durability;
    private double maxDurability;
    private int stock;
    private String tab;

    public PlayerShopItem() {
        this.uniqueId = 0;
        this.itemId = "";
        this.quantity = 1;
        this.priceBuy = 0.0;
        this.priceSell = 0.0;
        this.ownerUuid = null;
        this.durability = 0.0;
        this.maxDurability = 0.0;
        this.stock = 0;
        this.tab = "";
    }

    public PlayerShopItem(int uniqueId, String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, int stock) {
        this.uniqueId = uniqueId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceBuy = priceBuy;
        this.priceSell = priceSell;
        this.ownerUuid = ownerUuid;
        this.durability = durability;
        this.maxDurability = 0.0;
        this.stock = stock;
        this.tab = "";
    }
    
    public PlayerShopItem(int uniqueId, String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, int stock, String tab) {
        this.uniqueId = uniqueId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceBuy = priceBuy;
        this.priceSell = priceSell;
        this.ownerUuid = ownerUuid;
        this.durability = durability;
        this.maxDurability = 0.0;
        this.stock = stock;
        this.tab = tab != null ? tab : "";
    }
    
    public PlayerShopItem(int uniqueId, String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, double maxDurability, int stock, String tab) {
        this.uniqueId = uniqueId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceBuy = priceBuy;
        this.priceSell = priceSell;
        this.ownerUuid = ownerUuid;
        this.durability = durability;
        this.maxDurability = maxDurability;
        this.stock = stock;
        this.tab = tab != null ? tab : "";
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceBuy() {
        return priceBuy;
    }

    public void setPriceBuy(double priceBuy) {
        this.priceBuy = priceBuy;
    }

    public double getPriceSell() {
        return priceSell;
    }

    public void setPriceSell(double priceSell) {
        this.priceSell = priceSell;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public double getDurability() {
        return durability;
    }

    public void setDurability(double durability) {
        this.durability = durability;
    }

    public double getMaxDurability() {
        return maxDurability;
    }

    public void setMaxDurability(double maxDurability) {
        this.maxDurability = maxDurability;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
    
    public String getTab() {
        return tab != null ? tab : "";
    }
    
    public void setTab(String tab) {
        this.tab = tab != null ? tab : "";
    }
}

