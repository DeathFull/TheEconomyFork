package com.economy.gui;

import com.economy.shop.ShopItem;
import com.economy.shop.ShopManager;
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

public class ShopManagerAddCashGui extends InteractiveCustomUIPage<ShopManagerAddCashGui.AddCashGuiData> {

    private final PlayerRef playerRef;
    private final String selectedTab;
    private final int shopId;
    private String currentItemId = "";
    private String currentQuantity = "1";
    private String currentPriceBuy = "0";
    private int selectedHotbarSlot = -1;

    public ShopManagerAddCashGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab, int shopId) {
        super(playerRef, lifetime, AddCashGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        this.shopId = shopId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Manager_AddItem.ui");
        
        Player player = store.getComponent(ref, Player.getComponentType());
        String defaultItemId = "";
        int defaultQuantity = 1;
        
        // Tenta pegar o item da mão para preencher como padrão
        if (player != null) {
            var inventory = player.getInventory();
            if (inventory != null) {
                var activeItem = inventory.getActiveHotbarItem();
                if (activeItem != null && !activeItem.isEmpty()) {
                    defaultItemId = activeItem.getItemId();
                    if (defaultItemId != null && !defaultItemId.isEmpty()) {
                        defaultQuantity = activeItem.getQuantity();
                    }
                }
            }
        }
        
        // Define o título
        String titleText = LanguageManager.getTranslation("gui_shop_manager_add_cash_title");
        if (titleText == null || titleText.isEmpty() || titleText.equals("gui_shop_manager_add_cash_title")) {
            titleText = LanguageManager.getTranslation("gui_shop_manager_add_cash_title_simple");
        }
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define os labels
        uiCommandBuilder.set("#ItemIdLabel.Text", LanguageManager.getTranslation("gui_shop_manager_item_id"));
        uiCommandBuilder.set("#QuantityLabel.Text", LanguageManager.getTranslation("gui_shop_manager_quantity"));
        uiCommandBuilder.set("#PriceBuyLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_buy_cash"));
        uiCommandBuilder.set("#HotbarItemsLabel.Text", LanguageManager.getTranslation("gui_shop_manager_hotbar_items"));
        
        // Oculta o campo de venda (Cash items não têm venda)
        uiCommandBuilder.set("#PriceSellLabel.Visible", false);
        uiCommandBuilder.set("#PriceSellField.Visible", false);
        
        // Define valores padrão
        if (this.currentItemId == null || this.currentItemId.isEmpty()) {
            this.currentItemId = defaultItemId != null ? defaultItemId : "";
        }
        if (this.currentQuantity == null || this.currentQuantity.isEmpty()) {
            this.currentQuantity = String.valueOf(defaultQuantity);
        }
        if (this.currentPriceBuy == null || this.currentPriceBuy.isEmpty()) {
            this.currentPriceBuy = "0";
        }
        
        uiCommandBuilder.set("#ItemIdField.Value", this.currentItemId);
        uiCommandBuilder.set("#QuantityField.Value", this.currentQuantity);
        uiCommandBuilder.set("#PriceBuyField.Value", this.currentPriceBuy);
        
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
        
        // Captura mudanças nos campos de texto
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemIdField", 
            EventData.of("@ItemIdField", "#ItemIdField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#QuantityField", 
            EventData.of("@QuantityField", "#QuantityField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceBuyField", 
            EventData.of("@PriceBuyField", "#PriceBuyField.Value"), false);
        
        // Carrega os itens da hotbar
        buildHotbarItems(ref, store, uiCommandBuilder, uiEventBuilder);
    }
    
    private void buildHotbarItems(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                                  @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        var inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        
        // Ocultar todos os botões inicialmente
        for (int i = 0; i < 9; i++) {
            commandBuilder.set("#HotbarButton" + i + ".Visible", false);
            commandBuilder.set("#HotbarButton" + i + " #ItemIcon.Visible", false);
        }
        
        try {
            com.hypixel.hytale.server.core.inventory.container.ItemContainer hotbar = inventory.getHotbar();
            if (hotbar == null) {
                return;
            }
            
            for (int slot = 0; slot < 9; slot++) {
                try {
                    com.hypixel.hytale.server.core.inventory.ItemStack hotbarItem = hotbar.getItemStack((short) slot);
                    
                    if (hotbarItem != null && !hotbarItem.isEmpty()) {
                        String itemId = hotbarItem.getItemId();
                        int quantity = hotbarItem.getQuantity();
                        
                        if (itemId != null && !itemId.isEmpty() && !itemId.equals("Empty")) {
                            String buttonId = "HotbarButton" + slot;
                            
                            commandBuilder.set("#" + buttonId + " #ItemIcon.ItemId", itemId);
                            commandBuilder.set("#" + buttonId + " #ItemIcon.Visible", true);
                            commandBuilder.set("#" + buttonId + ".Visible", true);
                            
                            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                                "#" + buttonId, 
                                EventData.of("Action", "selectHotbar:" + slot + ":" + itemId + ":" + quantity));
                        }
                    }
                } catch (Exception e) {
                    // Ignora slots vazios
                }
            }
        } catch (Exception e) {
            // Ignora erros
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull AddCashGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        // Atualiza valores quando os campos mudam
        if (data.itemIdField != null) {
            this.currentItemId = data.itemIdField;
        }
        if (data.quantityField != null) {
            this.currentQuantity = data.quantityField;
        }
        if (data.priceBuyField != null) {
            this.currentPriceBuy = data.priceBuyField;
        }
        
        // Processa seleção de item da hotbar
        if (data.action != null && data.action.startsWith("selectHotbar:")) {
            String[] parts = data.action.split(":");
            if (parts.length >= 4) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    String itemId = parts[2];
                    int quantity = Integer.parseInt(parts[3]);
                    
                    this.currentItemId = itemId;
                    this.currentQuantity = String.valueOf(quantity);
                    this.selectedHotbarSlot = slot;
                    
                    World world = store.getExternalData().getWorld();
                    if (world != null) {
                        world.execute(() -> {
                            try {
                                Player player = store.getComponent(ref, Player.getComponentType());
                                if (player != null) {
                                    ShopManagerAddCashGui newGui = new ShopManagerAddCashGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId);
                                    newGui.currentItemId = itemId;
                                    newGui.currentQuantity = String.valueOf(quantity);
                                    newGui.currentPriceBuy = this.currentPriceBuy;
                                    newGui.selectedHotbarSlot = slot;
                                    player.getPageManager().openCustomPage(ref, store, newGui);
                                }
                            } catch (Exception e) {
                                // Ignora erros
                            }
                        });
                    }
                } catch (Exception e) {
                    // Ignora erros
                }
            }
            return;
        }
        
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
            player.getPageManager().openCustomPage(ref, store, 
                new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            world.execute(() -> {
                try {
                    // Verifica permissão
                    if (!com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD)) {
                        player.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
                        return;
                    }
                    
                    // Valida a tab
                    if (selectedTab == null || selectedTab.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    if (!ShopManager.getInstance().hasTab(selectedTab, this.shopId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", selectedTab);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, placeholders));
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    // Valida campos
                    String itemId = this.currentItemId != null ? this.currentItemId.trim() : "";
                    if (itemId.isEmpty() || !ItemManager.hasItem(itemId)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemId);
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        return;
                    }
                    
                    int quantity = 1;
                    try {
                        quantity = Integer.parseInt(this.currentQuantity);
                        if (quantity < 1) {
                            quantity = 1;
                        }
                    } catch (NumberFormatException e) {
                        quantity = 1;
                    }
                    
                    int priceBuy = 0;
                    try {
                        String priceBuyStr = this.currentPriceBuy != null ? this.currentPriceBuy : "0";
                        priceBuy = Integer.parseInt(priceBuyStr);
                        if (priceBuy < 0) {
                            priceBuy = 0;
                        }
                    } catch (NumberFormatException e) {
                        priceBuy = 0;
                    }
                    
                    // Adiciona o item à loja (só compra, sem venda)
                    ShopItem shopItem = ShopManager.getInstance().addItem(itemId, quantity, 0.0, (double)priceBuy, selectedTab, this.shopId);
                    
                    // Configura para usar Cash
                    shopItem.setUseCash(true);
                    
                    // Atualiza o item
                    ShopManager.getInstance().updateItem(shopItem, this.shopId);
                    
                    String itemName = ItemManager.getItemName(itemId);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", itemName);
                    placeholders.put("quantity", String.valueOf(quantity));
                    placeholders.put("price", String.valueOf(priceBuy));
                    placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                    player.sendMessage(LanguageManager.getMessage("chat_shop_cash_added", Color.GREEN, placeholders));
                    
                    // Volta para a GUI de gerenciamento
                    player.getPageManager().openCustomPage(ref, store, 
                        new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_add", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
    }

    public static class AddCashGuiData {
        private String action;
        private String itemId;
        private String quantity;
        private String priceBuy;
        private String itemIdField;
        private String quantityField;
        private String priceBuyField;
        
        public static final BuilderCodec<AddCashGuiData> CODEC = BuilderCodec.<AddCashGuiData>builder(AddCashGuiData.class, AddCashGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("ItemId", Codec.STRING),
                        (data, value, extraInfo) -> data.itemId = value,
                        (data, extraInfo) -> data.itemId != null ? data.itemId : "").add()
                .append(new KeyedCodec<>("Quantity", Codec.STRING),
                        (data, value, extraInfo) -> data.quantity = value,
                        (data, extraInfo) -> data.quantity != null ? data.quantity : "").add()
                .append(new KeyedCodec<>("PriceBuy", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuy = value,
                        (data, extraInfo) -> data.priceBuy != null ? data.priceBuy : "").add()
                .append(new KeyedCodec<>("@ItemIdField", Codec.STRING),
                        (data, value, extraInfo) -> data.itemIdField = value,
                        (data, extraInfo) -> data.itemIdField != null ? data.itemIdField : "").add()
                .append(new KeyedCodec<>("@QuantityField", Codec.STRING),
                        (data, value, extraInfo) -> data.quantityField = value,
                        (data, extraInfo) -> data.quantityField != null ? data.quantityField : "").add()
                .append(new KeyedCodec<>("@PriceBuyField", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuyField = value,
                        (data, extraInfo) -> data.priceBuyField != null ? data.priceBuyField : "").add()
                .build();
    }
}

