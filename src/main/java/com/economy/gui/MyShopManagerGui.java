package com.economy.gui;

import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyShopManagerGui extends InteractiveCustomUIPage<MyShopManagerGui.MyShopManagerGuiData> {

    private final PlayerRef playerRef;
    private String selectedTab = "";

    public MyShopManagerGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, MyShopManagerGuiData.CODEC);
        this.playerRef = playerRef;
    }

    public MyShopManagerGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab) {
        super(playerRef, lifetime, MyShopManagerGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_MyShop_Manager_Gui.ui");
        
        UUID playerUuid = this.playerRef.getUuid();
        
        // Define o título
        String titleText = LanguageManager.getTranslation("gui_myshop_manager_title");
        uiCommandBuilder.set("#Title #ManagerShopTitle.Text", titleText);
        
        // Verifica se há tabs disponíveis antes de configurar o botão AddItem
        List<String> tabs = PlayerShopManager.getInstance().getAllTabs(playerUuid);
        boolean hasTabs = !tabs.isEmpty();
        
        // Define os textos dos botões
        if (hasTabs) {
            // Só configura o botão AddItem se houver tabs
            uiCommandBuilder.set("#Content #ManagerButtons #AddItemButton #AddItemLabel.TextSpans", 
                Message.raw(LanguageManager.getTranslation("gui_shop_manager_add_item")).color(Color.WHITE));
        } else {
            // Remove o botão AddItem se não houver tabs
            try {
                uiCommandBuilder.remove("#Content #ManagerButtons #AddItemButton");
            } catch (Exception e) {
                // Se remove() não existir, apenas não configura o botão
                // O botão ainda aparecerá no UI, mas sem texto e sem funcionalidade
            }
        }
        
        uiCommandBuilder.set("#Content #ManagerButtons #AddTabButton #AddTabLabel.TextSpans", 
            Message.raw(LanguageManager.getTranslation("gui_shop_manager_add_tab")).color(Color.WHITE));
        
        // Verifica se a loja está aberta ou fechada
        boolean isShopOpen = PlayerShopManager.getInstance().isShopOpen(playerUuid);
        String toggleButtonText = isShopOpen 
            ? LanguageManager.getTranslation("gui_myshop_close_shop") 
            : LanguageManager.getTranslation("gui_myshop_open_shop");
        uiCommandBuilder.set("#Content #ManagerButtons #ToggleShopButton #ToggleShopLabel.TextSpans", 
            Message.raw(toggleButtonText).color(Color.WHITE));
        
        uiCommandBuilder.set("#Content #ManagerButtons #RenameShopButton #RenameShopLabel.TextSpans", 
            Message.raw(LanguageManager.getTranslation("gui_myshop_rename_shop")).color(Color.WHITE));
        
        uiCommandBuilder.set("#Content #ManagerButtons #SetIconButton #SetIconLabel.TextSpans", 
            Message.raw(LanguageManager.getTranslation("gui_myshop_set_icon")).color(Color.WHITE));
        
        // Se não houver tab selecionada, seleciona a primeira tab disponível
        if (this.selectedTab == null || this.selectedTab.isEmpty()) {
            if (hasTabs) {
                this.selectedTab = tabs.get(0);
            }
        }
        
        // Constrói as tabs
        this.buildTabs(uiCommandBuilder, uiEventBuilder, playerUuid);
        
        // Constrói os itens
        this.buildShopItems(ref, store, uiCommandBuilder, uiEventBuilder, this.selectedTab, playerUuid);
        
        // Adiciona eventos nos botões apenas se houver tabs
        if (hasTabs) {
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                "#Content #ManagerButtons #AddItemButton", 
                EventData.of("Action", "add_item"));
        }
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#Content #ManagerButtons #AddTabButton", 
            EventData.of("Action", "add_tab"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#Content #ManagerButtons #RenameShopButton", 
            EventData.of("Action", "rename_shop"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#Content #ManagerButtons #ToggleShopButton", 
            EventData.of("Action", "toggle_shop"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#Content #ManagerButtons #SetIconButton", 
            EventData.of("Action", "set_icon"));
    }

    private void buildTabs(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, UUID playerUuid) {
        List<String> tabs = PlayerShopManager.getInstance().getAllTabs(playerUuid);
        
        int maxTabs = Math.min(tabs.size(), 7);
        commandBuilder.clear("#Content #ShopTabs");
        
        if (maxTabs == 0) {
            return;
        }
        
        int currentIndex = 0;
        for (int i = 0; i < maxTabs; i++) {
            String tabName = tabs.get(i);
            
            if (i > 0) {
                commandBuilder.appendInline("#Content #ShopTabs", "Label { Anchor: (Width: 3); }");
                currentIndex++;
            }
            
            commandBuilder.append("#Content #ShopTabs", "Pages/EconomySystem_Shop_TabButton.ui");
            String tabButtonPath = "#Content #ShopTabs[" + currentIndex + "]";
            
            commandBuilder.set(tabButtonPath + " #TabLabel.TextSpans", Message.raw(tabName).color(new Color(255, 255, 255)));
            
            // Clique esquerdo: muda de tab
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, tabButtonPath, 
                EventData.of("Action", "tab:" + tabName));
            
            // Clique direito: remove tab
            try {
                CustomUIEventBindingType rightClickType = CustomUIEventBindingType.valueOf("RightClicking");
                eventBuilder.addEventBinding(rightClickType, tabButtonPath, 
                    EventData.of("Action", "remove_tab:" + tabName));
            } catch (Exception e) {
                try {
                    CustomUIEventBindingType secondaryType = CustomUIEventBindingType.valueOf("SecondaryActivating");
                    eventBuilder.addEventBinding(secondaryType, tabButtonPath, 
                        EventData.of("Action", "remove_tab:" + tabName));
                } catch (Exception e2) {
                    // Ignora se não existir
                }
            }
            
            currentIndex++;
        }
    }

    private void buildShopItems(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                                @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, 
                                String selectedTab, UUID playerUuid) {
        List<PlayerShopItem> items;
        if (selectedTab == null || selectedTab.isEmpty()) {
            items = new java.util.ArrayList<>();
        } else {
            items = PlayerShopManager.getInstance().getItemsByTab(playerUuid, selectedTab);
        }
        
        commandBuilder.clear("#Content #ShopContent");
        
        if (items.isEmpty()) {
            commandBuilder.append("#Content #ShopContent", "Pages/EconomySystem_Shop_EmptyMessage.ui");
            commandBuilder.set("#Content #ShopContent[0] #EmptyMessageLabel.TextSpans", 
                Message.raw(LanguageManager.getTranslation("gui_shop_empty")).color(new Color(204, 204, 204)));
            return;
        }
        
        int rowIndex = 0;
        int cardsInCurrentRow = 0;
        
        for (PlayerShopItem item : items) {
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#Content #ShopContent", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            
            commandBuilder.append("#Content #ShopContent[" + rowIndex + "]", "Pages/EconomySystem_Shop_ItemCard.ui");
            
            commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", item.getItemId());
            
            Message itemName;
            com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(item.getItemId());
            if (itemConfig != null) {
                itemName = Message.translation(itemConfig.getTranslationKey());
            } else {
                itemName = Message.translation("item." + item.getItemId());
            }
            
            commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", itemName);
            
            // Define o preço abaixo do nome do item (igual ao /shop)
            StringBuilder priceTextBuilder = new StringBuilder();
            if (item.getPriceBuy() > 0) {
                priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_buy_label"))
                    .append(com.economy.util.CurrencyFormatter.formatNumberOnlyShort(item.getPriceBuy()));
            }
            if (item.getPriceSell() > 0) {
                if (priceTextBuilder.length() > 0) {
                    priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_separator"));
                }
                priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_sell_label"))
                    .append(com.economy.util.CurrencyFormatter.formatNumberOnlyShort(item.getPriceSell()));
            }
            
            if (priceTextBuilder.length() > 0) {
                commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.TextSpans", Message.raw(priceTextBuilder.toString()));
            } else {
                commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.Visible", false);
            }
            
            // Mostra informações do item no tooltip
            com.economy.util.MessageHelper.ML tooltip = com.economy.util.MessageHelper.multiLine();
            tooltip.append(itemName.bold(true).color(new Color(85, 255, 85)))
                    .nl()
                    .separator();
            
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_unique_id") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(String.valueOf(item.getUniqueId())).color(new Color(255, 255, 255)))
                    .nl();
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_quantity") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(String.valueOf(item.getQuantity())).color(new Color(255, 255, 255)))
                    .nl();
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_stock") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(String.valueOf(item.getStock())).color(new Color(255, 255, 255)))
                    .nl();
            
            tooltip.separator();
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_buy") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(com.economy.util.CurrencyFormatter.format(item.getPriceBuy())).color(new Color(255, 85, 85)).bold(true))
                    .nl();
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_sell") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(com.economy.util.CurrencyFormatter.format(item.getPriceSell())).color(new Color(85, 255, 85)).bold(true))
                    .nl();
            
            tooltip.separator();
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_manager_remove_item_hint")).color(new Color(255, 85, 85)));
            tooltip.nl();
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_manager_edit_item_hint")).color(new Color(85, 255, 255)));
            
            commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            
            // Clique esquerdo para remover item (com confirmação)
            // Clique direito para editar preço
            String itemPath = "#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "]";
            String uniqueIdStr = String.valueOf(item.getUniqueId());
            
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, itemPath, 
                EventData.of("Action", "remove_item:" + uniqueIdStr));
            
            // Clique direito para editar
            try {
                CustomUIEventBindingType rightClickType = CustomUIEventBindingType.valueOf("RightClicking");
                eventBuilder.addEventBinding(rightClickType, itemPath, 
                    EventData.of("Action", "edit_item:" + uniqueIdStr));
            } catch (Exception e) {
                try {
                    CustomUIEventBindingType secondaryType = CustomUIEventBindingType.valueOf("SecondaryActivating");
                    eventBuilder.addEventBinding(secondaryType, itemPath, 
                        EventData.of("Action", "edit_item:" + uniqueIdStr));
                } catch (Exception e2) {
                    // Ignora se não existir
                }
            }
            
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 7) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull MyShopManagerGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            
            UUID playerUuid = this.playerRef.getUuid();
            World world = store.getExternalData().getWorld();
            if (world == null) return;
            
            String[] parts = data.action.split(":");
            if (parts.length < 1) {
                return;
            }
            
            String actionType = parts[0];
            
            if ("tab".equals(actionType) && parts.length >= 2) {
                String tabName = parts[1];
                this.selectedTab = tabName;
                player.getPageManager().openCustomPage(ref, store, 
                    new MyShopManagerGui(this.playerRef, CustomPageLifetime.CanDismiss, tabName));
                return;
            }
            
            if ("add_item".equals(actionType)) {
                String tabToUse = this.selectedTab;
                if (tabToUse == null || tabToUse.isEmpty()) {
                    List<String> tabs = PlayerShopManager.getInstance().getAllTabs(playerUuid);
                    if (tabs.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        return;
                    }
                    tabToUse = tabs.get(0);
                }
                
                player.getPageManager().openCustomPage(ref, store, 
                    new MyShopManagerAddItemGui(this.playerRef, CustomPageLifetime.CanDismiss, tabToUse));
                return;
            }
            
            if ("add_tab".equals(actionType)) {
                player.getPageManager().openCustomPage(ref, store, 
                    new MyShopManagerAddTabGui(this.playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
            
            if ("rename_shop".equals(actionType)) {
                player.getPageManager().openCustomPage(ref, store, 
                    new MyShopRenameGui(this.playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
            
            if ("toggle_shop".equals(actionType)) {
                world.execute(() -> {
                    boolean isOpen = PlayerShopManager.getInstance().isShopOpen(playerUuid);
                    PlayerShopManager.getInstance().setShopOpen(playerUuid, !isOpen);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    if (!isOpen) {
                        placeholders.put("action", LanguageManager.getTranslation("chat_myshop_opened"));
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_status_changed", Color.GREEN, placeholders));
                    } else {
                        placeholders.put("action", LanguageManager.getTranslation("chat_myshop_closed"));
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_status_changed", Color.GREEN, placeholders));
                    }
                    
                    // Recarrega a GUI
                    player.getPageManager().openCustomPage(ref, store, 
                        new MyShopManagerGui(this.playerRef, CustomPageLifetime.CanDismiss, this.selectedTab));
                });
                return;
            }
            
            if ("set_icon".equals(actionType)) {
                world.execute(() -> {
                    var inventory = player.getInventory();
                    if (inventory == null) {
                        player.sendMessage(LanguageManager.getMessage("chat_iteminfo_error_inventory", Color.RED));
                        return;
                    }
                    
                    var activeItem = inventory.getActiveHotbarItem();
                    if (activeItem == null || activeItem.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_iteminfo_no_item", Color.YELLOW));
                        return;
                    }
                    
                    String itemId = activeItem.getItemId();
                    if (itemId == null || itemId.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, new HashMap<>()));
                        return;
                    }
                    
                    // Verifica se o item existe
                    if (!com.economy.util.ItemManager.hasItem(itemId)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemId);
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        return;
                    }
                    
                    // Define o ícone da loja
                    boolean success = PlayerShopManager.getInstance().setShopIcon(playerUuid, itemId);
                    if (success) {
                        String itemName = com.economy.util.ItemManager.getItemName(itemId);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("item", itemName);
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_icon_set", Color.GREEN, placeholders));
                    } else {
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_icon_set_error", Color.RED));
                    }
                    
                    // Recarrega a GUI
                    player.getPageManager().openCustomPage(ref, store, 
                        new MyShopManagerGui(this.playerRef, CustomPageLifetime.CanDismiss, this.selectedTab));
                });
                return;
            }
            
            if ("remove_tab".equals(actionType) && parts.length >= 2) {
                String tabName = parts[1];
                player.getPageManager().openCustomPage(ref, store, 
                    new MyShopManagerRemoveTabConfirmationGui(this.playerRef, CustomPageLifetime.CanDismiss, tabName, this.selectedTab, playerUuid));
                return;
            }
            
            if ("remove_item".equals(actionType) && parts.length >= 2) {
                try {
                    int uniqueId = Integer.parseInt(parts[1]);
                    PlayerShopItem shopItem = PlayerShopManager.getInstance().getItem(uniqueId);
                    if (shopItem != null && shopItem.getOwnerUuid() != null && shopItem.getOwnerUuid().equals(playerUuid)) {
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerRemoveItemConfirmationGui(this.playerRef, CustomPageLifetime.CanDismiss, shopItem, this.selectedTab));
                    }
                } catch (NumberFormatException e) {
                    // Ignora
                }
                return;
            }
            
            if ("edit_item".equals(actionType) && parts.length >= 2) {
                try {
                    int uniqueId = Integer.parseInt(parts[1]);
                    PlayerShopItem shopItem = PlayerShopManager.getInstance().getItem(uniqueId);
                    if (shopItem != null && shopItem.getOwnerUuid() != null && shopItem.getOwnerUuid().equals(playerUuid)) {
                        // Abre GUI para editar preço do item
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerEditItemGui(this.playerRef, CustomPageLifetime.CanDismiss, shopItem, this.selectedTab));
                    }
                } catch (NumberFormatException e) {
                    // Ignora
                }
                return;
            }
        }
    }

    public static class MyShopManagerGuiData {
        private String action;
        private String selectedTab;
        
        public static final BuilderCodec<MyShopManagerGuiData> CODEC = BuilderCodec.<MyShopManagerGuiData>builder(MyShopManagerGuiData.class, MyShopManagerGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("SelectedTab", Codec.STRING),
                        (data, value, extraInfo) -> data.selectedTab = value,
                        (data, extraInfo) -> data.selectedTab != null ? data.selectedTab : "").add()
                .build();
    }
}

