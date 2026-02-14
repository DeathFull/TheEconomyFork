package com.economy.shop;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;

public class ShopItem {

    public static final BuilderCodec<ShopItem> CODEC = BuilderCodec.builder(ShopItem.class, ShopItem::new)
            .append(new KeyedCodec<>("UniqueId", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.uniqueId = value != null ? value.intValue() : 0,
                    (item, extraInfo) -> (double) item.uniqueId).add()
            .append(new KeyedCodec<>("ItemId", Codec.STRING),
                    (item, value, extraInfo) -> item.itemId = value != null ? value : "",
                    (item, extraInfo) -> item.itemId).add()
            .append(new KeyedCodec<>("Quantity", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.quantity = value != null ? value.intValue() : 1,
                    (item, extraInfo) -> (double) item.quantity).add()
            .append(new KeyedCodec<>("PriceSell", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.priceSell = value != null ? value : 0.0,
                    (item, extraInfo) -> item.priceSell).add()
            .append(new KeyedCodec<>("PriceBuy", Codec.DOUBLE),
                    (item, value, extraInfo) -> item.priceBuy = value != null ? value : 0.0,
                    (item, extraInfo) -> item.priceBuy).add()
            .append(new KeyedCodec<>("Tab", Codec.STRING),
                    (item, value, extraInfo) -> item.tab = value != null ? value : "",
                    (item, extraInfo) -> item.tab != null ? item.tab : "").add()
            .append(new KeyedCodec<>("IsConsoleCommand", Codec.BOOLEAN),
                    (item, value, extraInfo) -> item.isConsoleCommand = value != null ? value : false,
                    (item, extraInfo) -> item.isConsoleCommand).add()
            .append(new KeyedCodec<>("ConsoleCommand", Codec.STRING),
                    (item, value, extraInfo) -> item.consoleCommand = value != null ? value : "",
                    (item, extraInfo) -> item.consoleCommand != null ? item.consoleCommand : "").add()
            .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                    (item, value, extraInfo) -> item.displayName = value != null ? value : "",
                    (item, extraInfo) -> item.displayName != null ? item.displayName : "").add()
            .append(new KeyedCodec<>("UseCash", Codec.BOOLEAN),
                    (item, value, extraInfo) -> item.useCash = value != null ? value : false,
                    (item, extraInfo) -> item.useCash).add()
            .build();

    private int uniqueId;
    private String itemId;
    private int quantity;
    private double priceSell;
    private double priceBuy;
    private String tab;
    private boolean isConsoleCommand; // Se true, é um comando console em vez de item
    private String consoleCommand; // O comando a ser executado (pode conter {playername} e {quanty})
    private String displayName; // Nome personalizado para exibir na loja (para comandos console)
    private boolean useCash; // Se true, usa Cash em vez de Dinheiro para compra

    public ShopItem() {
        this.uniqueId = 0;
        this.itemId = "";
        this.quantity = 1;
        this.priceSell = 0.0;
        this.priceBuy = 0.0;
        this.tab = "";
        this.isConsoleCommand = false;
        this.consoleCommand = "";
        this.displayName = "";
        this.useCash = false;
    }

    public ShopItem(int uniqueId, String itemId, int quantity, double priceSell, double priceBuy) {
        this.uniqueId = uniqueId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceSell = priceSell;
        this.priceBuy = priceBuy;
        this.tab = "";
    }

    public ShopItem(int uniqueId, String itemId, int quantity, double priceSell, double priceBuy, String tab) {
        this.uniqueId = uniqueId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceSell = priceSell;
        this.priceBuy = priceBuy;
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

    public double getPriceSell() {
        return priceSell;
    }

    public void setPriceSell(double priceSell) {
        this.priceSell = priceSell;
    }

    public double getPriceBuy() {
        return priceBuy;
    }

    public void setPriceBuy(double priceBuy) {
        this.priceBuy = priceBuy;
    }

    public String getTab() {
        return tab != null ? tab : "";
    }

    public void setTab(String tab) {
        this.tab = tab != null ? tab : "";
    }
    
    public boolean isConsoleCommand() {
        return isConsoleCommand;
    }
    
    public void setConsoleCommand(boolean isConsoleCommand) {
        this.isConsoleCommand = isConsoleCommand;
    }
    
    public String getConsoleCommand() {
        return consoleCommand != null ? consoleCommand : "";
    }
    
    public void setConsoleCommand(String consoleCommand) {
        this.consoleCommand = consoleCommand != null ? consoleCommand : "";
    }
    
    public String getDisplayName() {
        return displayName != null ? displayName : "";
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
    }
    
    public boolean isUseCash() {
        return useCash;
    }
    
    public void setUseCash(boolean useCash) {
        this.useCash = useCash;
    }
}

