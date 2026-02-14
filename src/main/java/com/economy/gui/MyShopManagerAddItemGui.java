package com.economy.gui;

import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.InventoryHelper;
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

public class MyShopManagerAddItemGui extends InteractiveCustomUIPage<MyShopManagerAddItemGui.AddItemGuiData> {

    private final PlayerRef playerRef;
    private final String selectedTab;
    private String currentPriceBuy = "0";
    private String currentPriceSell = "0";
    private String selectedHotbarItemId = null;
    private int selectedHotbarQuantity = 1;
    private int selectedHotbarSlot = -1;
    private double selectedHotbarDurability = 0.0;
    private double selectedHotbarMaxDurability = 0.0;

    public MyShopManagerAddItemGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab) {
        super(playerRef, lifetime, AddItemGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_MyShop_Manager_AddItem.ui");
        
        Player player = store.getComponent(ref, Player.getComponentType());
        int defaultQuantity = 1;
        String itemNameText = "";
        
        // Pega o item da mão para preencher como padrão
        if (player != null) {
            var inventory = player.getInventory();
            if (inventory != null) {
                var activeItem = inventory.getActiveHotbarItem();
                if (activeItem != null && !activeItem.isEmpty()) {
                    defaultQuantity = activeItem.getQuantity();
                    String itemId = activeItem.getItemId();
                    if (itemId != null && !itemId.isEmpty()) {
                        itemNameText = ItemManager.getItemName(itemId);
                    }
                }
            }
        }
        
        // Define o título com o nome do item (usa o item selecionado da hotbar se houver)
        String displayItemName = itemNameText;
        int displayQuantity = defaultQuantity;
        if (this.selectedHotbarItemId != null && !this.selectedHotbarItemId.isEmpty()) {
            displayItemName = ItemManager.getItemName(this.selectedHotbarItemId);
            displayQuantity = this.selectedHotbarQuantity;
        }
        
        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("item", displayItemName);
        titlePlaceholders.put("quantity", String.valueOf(displayQuantity));
        String titleText = LanguageManager.getTranslation("gui_shop_manager_add_item_title", titlePlaceholders);
        if (titleText == null || titleText.isEmpty() || titleText.equals("gui_shop_manager_add_item_title")) {
            titleText = LanguageManager.getTranslation("gui_shop_manager_add_item_title_simple");
        }
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define os labels
        uiCommandBuilder.set("#PriceBuyLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_buy"));
        uiCommandBuilder.set("#PriceSellLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_sell"));
        uiCommandBuilder.set("#HotbarItemsLabel.Text", LanguageManager.getTranslation("gui_shop_manager_hotbar_items"));
        
        // Define valores padrão
        this.currentPriceBuy = "0";
        this.currentPriceSell = "0";
        
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
        
        // Captura mudanças nos campos de texto
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
            commandBuilder.set("#HotbarButton" + i + " #QuantityLabel.Visible", false);
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
                            
                            // Define a quantidade do item
                            commandBuilder.set("#" + buttonId + " #QuantityLabel.Text", String.valueOf(quantity));
                            commandBuilder.set("#" + buttonId + " #QuantityLabel.Visible", true);
                            
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
                    int slot = Integer.parseInt(parts[1]);
                    String itemId = parts[2];
                    int quantity = Integer.parseInt(parts[3]);
                    
                    // Captura a durabilidade e durabilidade máxima do item
                    double durability = 0.0;
                    double maxDurability = 0.0;
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        var inventory = player.getInventory();
                        if (inventory != null) {
                            var hotbar = inventory.getHotbar();
                            if (hotbar != null) {
                                try {
                                    com.hypixel.hytale.server.core.inventory.ItemStack hotbarItem = hotbar.getItemStack((short) slot);
                                    if (hotbarItem != null && !hotbarItem.isEmpty()) {
                                        try {
                                            java.lang.reflect.Method getDurabilityMethod = hotbarItem.getClass().getMethod("getDurability");
                                            getDurabilityMethod.setAccessible(true);
                                            Object durabilityObj = getDurabilityMethod.invoke(hotbarItem);
                                            if (durabilityObj != null) {
                                                if (durabilityObj instanceof Number) {
                                                    durability = ((Number) durabilityObj).doubleValue();
                                                } else {
                                                    durability = Double.parseDouble(durabilityObj.toString());
                                                }
                                            }
                                            
                                            // Captura a durabilidade máxima
                                            java.lang.reflect.Method getMaxDurabilityMethod = hotbarItem.getClass().getMethod("getMaxDurability");
                                            getMaxDurabilityMethod.setAccessible(true);
                                            Object maxDurabilityObj = getMaxDurabilityMethod.invoke(hotbarItem);
                                            if (maxDurabilityObj != null) {
                                                if (maxDurabilityObj instanceof Number) {
                                                    maxDurability = ((Number) maxDurabilityObj).doubleValue();
                                                } else {
                                                    maxDurability = Double.parseDouble(maxDurabilityObj.toString());
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Durabilidade não disponível, mantém 0.0
                                        }
                                    }
                                } catch (Exception e) {
                                    // Ignora erros ao obter durabilidade
                                }
                            }
                        }
                    }
                    
                    // Atualiza os campos
                    this.selectedHotbarItemId = itemId;
                    this.selectedHotbarQuantity = quantity;
                    this.selectedHotbarSlot = slot;
                    this.selectedHotbarDurability = durability;
                    this.selectedHotbarMaxDurability = maxDurability;
                    
                    // Atualiza os campos na GUI recriando a página
                    World world = store.getExternalData().getWorld();
                    if (world != null && player != null) {
                        final Player finalPlayer = player;
                        final double finalDurability = durability;
                        final double finalMaxDurability = maxDurability;
                        final String finalItemId = itemId;
                        final int finalQuantity = quantity;
                        final int finalSlot = slot;
                        world.execute(() -> {
                            try {
                                // Recria a página com os valores atualizados
                                MyShopManagerAddItemGui newGui = new MyShopManagerAddItemGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab);
                                newGui.selectedHotbarItemId = finalItemId;
                                newGui.selectedHotbarQuantity = finalQuantity;
                                newGui.selectedHotbarSlot = finalSlot;
                                newGui.selectedHotbarDurability = finalDurability;
                                newGui.selectedHotbarMaxDurability = finalMaxDurability;
                                finalPlayer.getPageManager().openCustomPage(ref, store, newGui);
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
            // Processa a adição do item
            world.execute(() -> {
                try {
                    UUID playerUuid = this.playerRef.getUuid();
                    
                    // Valida a tab
                    if (selectedTab == null || selectedTab.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        // Fecha a janela e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                        return;
                    }
                    
                    if (!PlayerShopManager.getInstance().hasTab(playerUuid, selectedTab)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", selectedTab);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, placeholders));
                        // Fecha a janela e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                        return;
                    }
                    
                    // Obtém o item (da hotbar selecionada ou da mão)
                    String itemId = null;
                    int handQuantity = 1;
                    
                    // Se um item da hotbar foi selecionado, usa ele
                    if (this.selectedHotbarItemId != null && !this.selectedHotbarItemId.isEmpty()) {
                        itemId = this.selectedHotbarItemId;
                        handQuantity = this.selectedHotbarQuantity;
                    } else {
                        // Caso contrário, usa o item da mão
                        var inventory = player.getInventory();
                        if (inventory == null) {
                            player.sendMessage(LanguageManager.getMessage("chat_iteminfo_error_inventory", Color.RED));
                            return;
                        }
                        
                        var activeItem = inventory.getActiveHotbarItem();
                        if (activeItem == null || activeItem.isEmpty()) {
                            player.sendMessage(LanguageManager.getMessage("chat_iteminfo_no_item", Color.YELLOW));
                            // Fecha a janela
                            player.getPageManager().openCustomPage(ref, store, 
                                new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                            return;
                        }
                        
                        itemId = activeItem.getItemId();
                        handQuantity = activeItem.getQuantity();
                    }
                    
                    // Verifica se o item existe
                    if (itemId == null || itemId.isEmpty() || !ItemManager.hasItem(itemId)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemId != null ? itemId : "null");
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        // Fecha a janela
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                        return;
                    }
                    
                    // Usa sempre a quantidade da mão (não editável)
                    int quantity = handQuantity;
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
                    
                    // Obtém a durabilidade e durabilidade máxima do item
                    double durability = 0.0;
                    double maxDurability = 0.0;
                    // Se um item da hotbar foi selecionado, captura a durabilidade diretamente do slot antes de remover
                    if (this.selectedHotbarItemId != null && this.selectedHotbarSlot >= 0) {
                        try {
                            var inventory = player.getInventory();
                            if (inventory != null) {
                                var hotbar = inventory.getHotbar();
                                if (hotbar != null) {
                                    // Obtém o item do slot antes de remover para garantir que temos a durabilidade atual
                                    com.hypixel.hytale.server.core.inventory.ItemStack hotbarItem = hotbar.getItemStack((short) this.selectedHotbarSlot);
                                    if (hotbarItem != null && !hotbarItem.isEmpty()) {
                                        try {
                                            java.lang.reflect.Method getDurabilityMethod = hotbarItem.getClass().getMethod("getDurability");
                                            getDurabilityMethod.setAccessible(true);
                                            Object durabilityObj = getDurabilityMethod.invoke(hotbarItem);
                                            if (durabilityObj != null) {
                                                if (durabilityObj instanceof Number) {
                                                    durability = ((Number) durabilityObj).doubleValue();
                                                } else {
                                                    durability = Double.parseDouble(durabilityObj.toString());
                                                }
                                            }
                                            
                                            // Captura a durabilidade máxima
                                            java.lang.reflect.Method getMaxDurabilityMethod = hotbarItem.getClass().getMethod("getMaxDurability");
                                            getMaxDurabilityMethod.setAccessible(true);
                                            Object maxDurabilityObj = getMaxDurabilityMethod.invoke(hotbarItem);
                                            if (maxDurabilityObj != null) {
                                                if (maxDurabilityObj instanceof Number) {
                                                    maxDurability = ((Number) maxDurabilityObj).doubleValue();
                                                } else {
                                                    maxDurability = Double.parseDouble(maxDurabilityObj.toString());
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Se falhar, usa a durabilidade já capturada
                                            durability = this.selectedHotbarDurability;
                                            maxDurability = this.selectedHotbarMaxDurability;
                                        }
                                    } else {
                                        // Se o item não existe mais no slot, usa a durabilidade já capturada
                                        durability = this.selectedHotbarDurability;
                                        maxDurability = this.selectedHotbarMaxDurability;
                                    }
                                } else {
                                    durability = this.selectedHotbarDurability;
                                    maxDurability = this.selectedHotbarMaxDurability;
                                }
                            } else {
                                durability = this.selectedHotbarDurability;
                                maxDurability = this.selectedHotbarMaxDurability;
                            }
                        } catch (Exception e) {
                            // Se falhar, usa a durabilidade já capturada
                            durability = this.selectedHotbarDurability;
                            maxDurability = this.selectedHotbarMaxDurability;
                        }
                    } else {
                        // Caso contrário, tenta obter a durabilidade do item da mão
                        try {
                            var inventory2 = player.getInventory();
                            if (inventory2 != null) {
                                var itemToCheck = inventory2.getActiveHotbarItem();
                                if (itemToCheck != null && !itemToCheck.isEmpty()) {
                                    java.lang.reflect.Method getDurabilityMethod = itemToCheck.getClass().getMethod("getDurability");
                                    getDurabilityMethod.setAccessible(true);
                                    Object durabilityObj = getDurabilityMethod.invoke(itemToCheck);
                                    if (durabilityObj != null) {
                                        if (durabilityObj instanceof Number) {
                                            durability = ((Number) durabilityObj).doubleValue();
                                        } else {
                                            durability = Double.parseDouble(durabilityObj.toString());
                                        }
                                    }
                                    
                                    // Captura a durabilidade máxima
                                    java.lang.reflect.Method getMaxDurabilityMethod = itemToCheck.getClass().getMethod("getMaxDurability");
                                    getMaxDurabilityMethod.setAccessible(true);
                                    Object maxDurabilityObj = getMaxDurabilityMethod.invoke(itemToCheck);
                                    if (maxDurabilityObj != null) {
                                        if (maxDurabilityObj instanceof Number) {
                                            maxDurability = ((Number) maxDurabilityObj).doubleValue();
                                        } else {
                                            maxDurability = Double.parseDouble(maxDurabilityObj.toString());
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Durabilidade não disponível, mantém 0.0
                        }
                    }
                    
                    // Remove o item do inventário
                    boolean itemRemoved = false;
                    // Se um item da hotbar foi selecionado, remove diretamente do slot
                    if (this.selectedHotbarItemId != null && this.selectedHotbarSlot >= 0) {
                        try {
                            var inventory = player.getInventory();
                            if (inventory != null) {
                                var hotbar = inventory.getHotbar();
                                if (hotbar != null) {
                                    // Remove o item do slot específico da hotbar
                                    hotbar.setItemStackForSlot((short) this.selectedHotbarSlot, null);
                                    itemRemoved = true;
                                }
                            }
                        } catch (Exception e) {
                            // Se falhar, tenta o método genérico
                            itemRemoved = false;
                        }
                    }
                    
                    // Se não removeu pelo slot específico, usa o método genérico
                    if (!itemRemoved) {
                        itemRemoved = InventoryHelper.removeItem(player, itemId, quantity);
                    }
                    
                    if (!itemRemoved) {
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_error_remove_item", Color.RED));
                        // Fecha a janela e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                        return;
                    }
                    
                    // Atualiza informações do jogador
                    PlayerShopManager.getInstance().updatePlayerInfo(playerUuid, player.getDisplayName());
                    
                    // Adiciona o item à loja do jogador
                    PlayerShopItem shopItem = PlayerShopManager.getInstance().addOrUpdateItem(
                        itemId, quantity, priceBuy, priceSell, playerUuid, durability, maxDurability, quantity, selectedTab);
                    
                    // Obtém o nome do item traduzido
                    String itemName = ItemManager.getItemName(itemId);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", itemName);
                    placeholders.put("quantity", String.valueOf(quantity));
                    placeholders.put("price", CurrencyFormatter.format(priceBuy));
                    placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                    player.sendMessage(LanguageManager.getMessage("chat_myshop_item_added", Color.GREEN, placeholders));
                    
                    // Volta para a GUI de gerenciamento
                    player.getPageManager().openCustomPage(ref, store, 
                        new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_add", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
    }

    public static class AddItemGuiData {
        private String action;
        private String priceBuy;
        private String priceSell;
        private String priceBuyField;
        private String priceSellField;
        
        public static final BuilderCodec<AddItemGuiData> CODEC = BuilderCodec.<AddItemGuiData>builder(AddItemGuiData.class, AddItemGuiData::new)
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

