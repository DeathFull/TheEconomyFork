package com.economy.gui;

import com.economy.shop.ShopManager;
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

public class ShopManagerAddItemGui extends InteractiveCustomUIPage<ShopManagerAddItemGui.AddItemGuiData> {

    private final PlayerRef playerRef;
    private final String selectedTab;
    private final int shopId; // ID da loja (0 para /shop, 1+ para NPCs)
    private String currentItemId = "";
    private String currentQuantity = "1";
    private String currentPriceBuy = "0";
    private String currentPriceSell = "0";

    public ShopManagerAddItemGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab) {
        this(playerRef, lifetime, selectedTab, 0);
    }
    
    public ShopManagerAddItemGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab, int shopId) {
        super(playerRef, lifetime, AddItemGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        this.shopId = shopId;
        com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem")
            .at(java.util.logging.Level.INFO).log("ShopManagerAddItemGui created with shopId: %d, selectedTab: %s", this.shopId, this.selectedTab);
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
        
        // Define o título - se há um item selecionado, mostra o nome do item
        String titleText;
        if (this.currentItemId != null && !this.currentItemId.isEmpty()) {
            String itemName = ItemManager.getItemName(this.currentItemId);
            if (itemName == null || itemName.isEmpty()) {
                itemName = this.currentItemId;
            }
            titleText = itemName + " x" + this.currentQuantity;
        } else {
            titleText = LanguageManager.getTranslation("gui_shop_manager_add_item_title_simple");
        }
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define os labels
        uiCommandBuilder.set("#ItemIdLabel.Text", LanguageManager.getTranslation("gui_shop_manager_item_id"));
        uiCommandBuilder.set("#QuantityLabel.Text", LanguageManager.getTranslation("gui_shop_manager_quantity"));
        uiCommandBuilder.set("#PriceBuyLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_buy"));
        uiCommandBuilder.set("#PriceSellLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_sell"));
        uiCommandBuilder.set("#HotbarItemsLabel.Text", LanguageManager.getTranslation("gui_shop_manager_hotbar_items"));
        
        // Define valores padrão (apenas se ainda não foram definidos, para preservar valores da hotbar)
        if (this.currentItemId == null || this.currentItemId.isEmpty()) {
            this.currentItemId = defaultItemId != null ? defaultItemId : "";
        }
        if (this.currentQuantity == null || this.currentQuantity.isEmpty()) {
            this.currentQuantity = String.valueOf(defaultQuantity);
        }
        if (this.currentPriceBuy == null || this.currentPriceBuy.isEmpty()) {
            this.currentPriceBuy = "0";
        }
        if (this.currentPriceSell == null || this.currentPriceSell.isEmpty()) {
            this.currentPriceSell = "0";
        }
        
        uiCommandBuilder.set("#ItemIdField.Value", this.currentItemId);
        uiCommandBuilder.set("#QuantityField.Value", this.currentQuantity);
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
        
        // Captura mudanças nos campos de texto (sem recriar a GUI)
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemIdField", 
            EventData.of("@ItemIdField", "#ItemIdField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#QuantityField", 
            EventData.of("@QuantityField", "#QuantityField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceBuyField", 
            EventData.of("@PriceBuyField", "#PriceBuyField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceSellField", 
            EventData.of("@PriceSellField", "#PriceSellField.Value"), false);
        
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
        
        // Obtém a hotbar usando a API correta
        try {
            com.hypixel.hytale.server.core.inventory.container.ItemContainer hotbar = inventory.getHotbar();
            if (hotbar == null) {
                return;
            }
            
            // Itera sobre todos os slots da hotbar (0-8)
            for (int slot = 0; slot < 9; slot++) {
                try {
                    // Obtém o item do slot usando getItemStack(short) - API correta
                    com.hypixel.hytale.server.core.inventory.ItemStack hotbarItem = hotbar.getItemStack((short) slot);
                    
                    // Verifica se há um item válido no slot
                    if (hotbarItem != null && !hotbarItem.isEmpty()) {
                        String itemId = hotbarItem.getItemId();
                        int quantity = hotbarItem.getQuantity();
                        
                        // Valida o itemId (deve existir e não ser "Empty")
                        if (itemId != null && !itemId.isEmpty() && !itemId.equals("Empty")) {
                            // Configura o botão para este item
                            String buttonId = "HotbarButton" + slot;
                            
                            // Define o ItemId do ícone
                            commandBuilder.set("#" + buttonId + " #ItemIcon.ItemId", itemId);
                            commandBuilder.set("#" + buttonId + " #ItemIcon.Visible", true);
                            commandBuilder.set("#" + buttonId + ".Visible", true);
                            
                            // Adiciona evento de clique para atualizar o campo ItemId e Quantity
                            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                                "#" + buttonId, 
                                EventData.of("Action", "selectHotbar:" + slot + ":" + itemId + ":" + quantity));
                        }
                    }
                } catch (Exception e) {
                    // Ignora slots vazios ou erros
                }
            }
        } catch (Exception e) {
            // Ignora erros ao acessar hotbar
        }
    }
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull AddItemGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        // Atualiza os valores das variáveis de instância quando os campos mudam
        if (data.itemIdField != null) {
            this.currentItemId = data.itemIdField;
        }
        if (data.quantityField != null) {
            this.currentQuantity = data.quantityField;
        }
        if (data.priceBuyField != null) {
            this.currentPriceBuy = data.priceBuyField;
        }
        if (data.priceSellField != null) {
            this.currentPriceSell = data.priceSellField;
        }
        
        // Processa seleção de item da hotbar
        if (data.action != null && data.action.startsWith("selectHotbar:")) {
            String[] parts = data.action.split(":");
            if (parts.length >= 4) {
                try {
                    String itemId = parts[2];
                    
                    // Atualiza apenas o ItemId, mantém a quantidade atual
                    this.currentItemId = itemId;
                    // Preserva a quantidade existente (não muda)
                    
                    // Atualiza os campos na GUI recriando a página
                    World world = store.getExternalData().getWorld();
                    if (world != null) {
                        world.execute(() -> {
                            try {
                                Player player = store.getComponent(ref, Player.getComponentType());
                                if (player != null) {
                                    // Recria a página com os valores atualizados (mantém a quantidade e shopId)
                                    ShopManagerAddItemGui newGui = new ShopManagerAddItemGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId);
                                    newGui.currentItemId = itemId;
                                    // Preserva a quantidade atual
                                    if (this.currentQuantity != null && !this.currentQuantity.isEmpty()) {
                                        newGui.currentQuantity = this.currentQuantity;
                                    }
                                    newGui.currentPriceBuy = this.currentPriceBuy;
                                    newGui.currentPriceSell = this.currentPriceSell;
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
        
        // Ignora eventos de mudança de valor dos campos de texto (apenas atualiza os valores)
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
                new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Processa a adição do item
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
                        // Fecha a janela e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    if (!ShopManager.getInstance().hasTab(selectedTab, this.shopId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", selectedTab);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, placeholders));
                        // Fecha a janela e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    // Obtém o Item ID do campo (usa a variável de instância que é atualizada pelo ValueChanged)
                    String itemId = this.currentItemId != null ? this.currentItemId.trim() : "";
                    
                    if (itemId.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_manager_item_id_empty", Color.RED));
                        return;
                    }
                    
                    // Obtém os valores dos campos (usa as variáveis de instância)
                    int quantity = 1;
                    double priceBuy = 0.0;
                    double priceSell = 0.0;
                    
                    String quantityStr = this.currentQuantity != null ? this.currentQuantity : "1";
                    String priceBuyStr = this.currentPriceBuy != null ? this.currentPriceBuy : "0";
                    String priceSellStr = this.currentPriceSell != null ? this.currentPriceSell : "0";
                    
                    if (!quantityStr.isEmpty()) {
                        try {
                            quantity = Integer.parseInt(quantityStr);
                            if (quantity <= 0) {
                                quantity = 1;
                            }
                        } catch (NumberFormatException e) {
                            quantity = 1;
                        }
                    }
                    
                    if (!priceBuyStr.isEmpty()) {
                        try {
                            priceBuy = Double.parseDouble(priceBuyStr);
                            if (priceBuy < 0) {
                                priceBuy = 0.0;
                            }
                        } catch (NumberFormatException e) {
                            priceBuy = 0.0;
                        }
                    }
                    
                    if (!priceSellStr.isEmpty()) {
                        try {
                            priceSell = Double.parseDouble(priceSellStr);
                            if (priceSell < 0) {
                                priceSell = 0.0;
                            }
                        } catch (NumberFormatException e) {
                            priceSell = 0.0;
                        }
                    }
                    
                    // Verifica se o item existe
                    if (!ItemManager.hasItem(itemId)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemId);
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        // Fecha a janela e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    // Durabilidade padrão (0.0 para itens novos)
                    double durability = 0.0;
                    
                    // Adiciona o item à loja
                    com.economy.shop.ShopItem shopItem = ShopManager.getInstance().addItem(itemId, quantity, priceSell, priceBuy, selectedTab, this.shopId);
                    
                    // Obtém o nome do item traduzido
                    String itemName = ItemManager.getItemName(itemId);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", itemName);
                    placeholders.put("itemid", itemId);
                    placeholders.put("quantity", String.valueOf(quantity));
                    placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                    placeholders.put("pricesell", CurrencyFormatter.format(priceSell));
                    placeholders.put("pricebuy", CurrencyFormatter.format(priceBuy));
                    player.sendMessage(LanguageManager.getMessage("chat_shop_item_added", Color.GREEN, placeholders));
                    
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
    
    private String getItemNameText(String itemId) {
        Message itemName;
        String itemNameText = itemId;

        com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = ItemManager.getItem(itemId);
        if (itemConfig != null) {
            itemName = Message.translation(itemConfig.getTranslationKey());
            itemNameText = extractTextFromMessage(itemName);
        } else {
            itemName = Message.translation("item." + itemId);
            itemNameText = extractTextFromMessage(itemName);
        }
        
        if (itemNameText == null || itemNameText.isEmpty() || itemNameText.startsWith("item.") || itemNameText.contains("com.hypixel")) {
            itemNameText = formatItemId(itemId);
        }
        return itemNameText;
    }
    
    private String extractTextFromMessage(Message message) {
        if (message == null) {
            return null;
        }
        
        try {
            try {
                java.lang.reflect.Method getTextMethod = message.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(message);
                if (textObj != null) {
                    String text = textObj.toString();
                    if (text != null && !text.isEmpty() && !text.startsWith("item.") && !text.contains("com.hypixel")) {
                        return text;
                    }
                }
            } catch (Exception e) {
                // Continua
            }
            
            try {
                java.lang.reflect.Method getStringMethod = message.getClass().getMethod("getString");
                Object textObj = getStringMethod.invoke(message);
                if (textObj != null) {
                    String text = textObj.toString();
                    if (text != null && !text.isEmpty() && !text.startsWith("item.") && !text.contains("com.hypixel")) {
                        return text;
                    }
                }
            } catch (Exception e) {
                // Continua
            }
        } catch (Exception e) {
            // Ignora
        }
        
        return null;
    }
    
    private String formatItemId(String itemId) {
        String[] parts = itemId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (formatted.length() > 0) {
                    formatted.append(" ");
                }
                formatted.append(part.substring(0, 1).toUpperCase())
                         .append(part.substring(1).toLowerCase());
            }
        }
        return formatted.toString();
    }

    public static class AddItemGuiData {
        private String action;
        private String itemId;
        private String quantity;
        private String priceBuy;
        private String priceSell;
        private String itemIdField;
        private String quantityField;
        private String priceBuyField;
        private String priceSellField;
        
        public static final BuilderCodec<AddItemGuiData> CODEC = BuilderCodec.<AddItemGuiData>builder(AddItemGuiData.class, AddItemGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("Quantity", Codec.STRING),
                        (data, value, extraInfo) -> data.quantity = value,
                        (data, extraInfo) -> data.quantity != null ? data.quantity : "").add()
                .append(new KeyedCodec<>("PriceBuy", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuy = value,
                        (data, extraInfo) -> data.priceBuy != null ? data.priceBuy : "").add()
                .append(new KeyedCodec<>("PriceSell", Codec.STRING),
                        (data, value, extraInfo) -> data.priceSell = value,
                        (data, extraInfo) -> data.priceSell != null ? data.priceSell : "").add()
                .append(new KeyedCodec<>("ItemId", Codec.STRING),
                        (data, value, extraInfo) -> data.itemId = value,
                        (data, extraInfo) -> data.itemId != null ? data.itemId : "").add()
                .append(new KeyedCodec<>("@ItemIdField", Codec.STRING),
                        (data, value, extraInfo) -> data.itemIdField = value,
                        (data, extraInfo) -> data.itemIdField != null ? data.itemIdField : "").add()
                .append(new KeyedCodec<>("@QuantityField", Codec.STRING),
                        (data, value, extraInfo) -> data.quantityField = value,
                        (data, extraInfo) -> data.quantityField != null ? data.quantityField : "").add()
                .append(new KeyedCodec<>("@PriceBuyField", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuyField = value,
                        (data, extraInfo) -> data.priceBuyField != null ? data.priceBuyField : "").add()
                .append(new KeyedCodec<>("@PriceSellField", Codec.STRING),
                        (data, value, extraInfo) -> data.priceSellField = value,
                        (data, extraInfo) -> data.priceSellField != null ? data.priceSellField : "").add()
                .build();
    }
}

