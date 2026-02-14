package com.economy.gui;

import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.ItemManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyShopManagerEditItemGui extends InteractiveCustomUIPage<MyShopManagerEditItemGui.EditItemGuiData> {

    private final PlayerRef playerRef;
    private final String selectedTab;
    private final PlayerShopItem shopItem;
    private String currentPriceBuy;
    private String currentPriceSell;

    public MyShopManagerEditItemGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                    PlayerShopItem shopItem, String selectedTab) {
        super(playerRef, lifetime, EditItemGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopItem = shopItem;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        // Carrega os valores atuais do item
        this.currentPriceBuy = String.valueOf(shopItem != null ? shopItem.getPriceBuy() : 0.0);
        this.currentPriceSell = String.valueOf(shopItem != null ? shopItem.getPriceSell() : 0.0);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_MyShop_Manager_EditItem.ui");
        
        // Define o título com o nome do item
        String itemName = "";
        if (shopItem != null) {
            itemName = ItemManager.getItemName(shopItem.getItemId());
        }
        
        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("item", itemName);
        titlePlaceholders.put("quantity", String.valueOf(shopItem != null ? shopItem.getQuantity() : 1));
        String titleText = LanguageManager.getTranslation("gui_shop_manager_edit_item_title", titlePlaceholders);
        if (titleText == null || titleText.isEmpty() || titleText.equals("gui_shop_manager_edit_item_title")) {
            titleText = LanguageManager.getTranslation("gui_shop_manager_edit_item_title_simple");
        }
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define os labels
        uiCommandBuilder.set("#PriceBuyLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_buy"));
        uiCommandBuilder.set("#PriceSellLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_sell"));
        
        uiCommandBuilder.set("#PriceBuyField.Value", this.currentPriceBuy);
        uiCommandBuilder.set("#PriceSellField.Value", this.currentPriceSell);
        
        // Define os textos dos botões
        String confirmText = LanguageManager.getTranslation("gui_shop_button_confirm");
        String cancelText = LanguageManager.getTranslation("gui_shop_button_cancel");
        uiCommandBuilder.set("#ConfirmButton.Text", confirmText);
        uiCommandBuilder.set("#CancelButton.Text", cancelText);
        
        // Configura os botões
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton", 
            EventData.of("Action", "confirm"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", 
            EventData.of("Action", "cancel"));
        
        // Captura mudanças nos campos de preço
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceBuyField", 
            EventData.of("@PriceBuyField", "#PriceBuyField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceSellField", 
            EventData.of("@PriceSellField", "#PriceSellField.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull EditItemGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        // Atualiza os valores das variáveis de instância quando os campos mudam
        if (data.priceBuyField != null) {
            this.currentPriceBuy = data.priceBuyField;
        }
        if (data.priceSellField != null) {
            this.currentPriceSell = data.priceSellField;
        }
        
        // Ignora eventos de mudança de valor dos campos de texto
        if (data.action == null || (!"confirm".equals(data.action) && !"cancel".equals(data.action))) {
            return;
        }
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }
        
        if ("cancel".equals(data.action)) {
            // Volta para a GUI de gerenciamento
            player.getPageManager().openCustomPage(ref, store, 
                new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Processa a atualização do preço
            world.execute(() -> {
                try {
                    UUID playerUuid = this.playerRef.getUuid();
                    
                    if (shopItem == null || !shopItem.getOwnerUuid().equals(playerUuid)) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED, new HashMap<>()));
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                        return;
                    }
                    
                    // Obtém os novos preços
                    double priceBuy = 0.0;
                    double priceSell = 0.0;
                    
                    String priceBuyStr = this.currentPriceBuy != null ? this.currentPriceBuy : "0";
                    String priceSellStr = this.currentPriceSell != null ? this.currentPriceSell : "0";
                    
                    if (!priceBuyStr.isEmpty()) {
                        try {
                            priceBuy = Double.parseDouble(priceBuyStr);
                            if (priceBuy < 0) {
                                priceBuy = 0.0;
                            }
                        } catch (NumberFormatException e) {
                            priceBuy = shopItem.getPriceBuy();
                        }
                    }
                    
                    if (!priceSellStr.isEmpty()) {
                        try {
                            priceSell = Double.parseDouble(priceSellStr);
                            if (priceSell < 0) {
                                priceSell = 0.0;
                            }
                        } catch (NumberFormatException e) {
                            priceSell = shopItem.getPriceSell();
                        }
                    }
                    
                    // Atualiza os preços do item
                    shopItem.setPriceBuy(priceBuy);
                    shopItem.setPriceSell(priceSell);
                    
                    // Salva a atualização
                    boolean updated = PlayerShopManager.getInstance().updateItemPrice(
                        shopItem.getUniqueId(), priceBuy, priceSell);
                    
                    if (!updated) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED, new HashMap<>()));
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                        return;
                    }
                    
                    String itemName = ItemManager.getItemName(shopItem.getItemId());
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", itemName);
                    placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                    placeholders.put("pricebuy", CurrencyFormatter.format(priceBuy));
                    placeholders.put("pricesell", CurrencyFormatter.format(priceSell));
                    player.sendMessage(LanguageManager.getMessage("chat_shop_item_updated", Color.GREEN, placeholders));
                    
                    // Volta para a GUI de gerenciamento
                    player.getPageManager().openCustomPage(ref, store, 
                        new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_update", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
    }

    public static class EditItemGuiData {
        private String action;
        private String priceBuy;
        private String priceSell;
        private String priceBuyField;
        private String priceSellField;
        
        public static final BuilderCodec<EditItemGuiData> CODEC = BuilderCodec.<EditItemGuiData>builder(EditItemGuiData.class, EditItemGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("PriceBuy", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuy = value,
                        (data, extraInfo) -> data.priceBuy != null ? data.priceBuy : "").add()
                .append(new KeyedCodec<>("PriceSell", Codec.STRING),
                        (data, value, extraInfo) -> data.priceSell = value,
                        (data, extraInfo) -> data.priceSell != null ? data.priceSell : "").add()
                .append(new KeyedCodec<>("@PriceBuyField", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuyField = value,
                        (data, extraInfo) -> data.priceBuyField != null ? data.priceBuyField : "").add()
                .append(new KeyedCodec<>("@PriceSellField", Codec.STRING),
                        (data, value, extraInfo) -> data.priceSellField = value,
                        (data, extraInfo) -> data.priceSellField != null ? data.priceSellField : "").add()
                .build();
    }
}

