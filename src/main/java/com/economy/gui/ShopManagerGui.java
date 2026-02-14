package com.economy.gui;

import com.economy.shop.ShopItem;
import com.economy.shop.ShopManager;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;

public class ShopManagerGui extends InteractiveCustomUIPage<ShopManagerGui.ShopManagerGuiData> {

    private final PlayerRef playerRef;
    private String selectedTab = "";
    private int shopId; // ID da loja (0 para /shop, 1+ para NPCs)

    public ShopManagerGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        this(playerRef, lifetime, "", 0);
    }

    public ShopManagerGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab) {
        this(playerRef, lifetime, selectedTab, 0);
    }
    
    public ShopManagerGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, int shopId) {
        this(playerRef, lifetime, "", shopId);
    }
    
    public ShopManagerGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab, int shopId) {
        super(playerRef, lifetime, ShopManagerGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        this.shopId = shopId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Manager_Gui.ui");
        
        // Define o título
        String titleText = LanguageManager.getTranslation("gui_shop_manager_title");
        uiCommandBuilder.set("#Title #ManagerShopTitle.Text", titleText);
        
        // Verifica se há tabs disponíveis antes de configurar o botão AddItem
        List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
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
        
        if (hasTabs) {
            // Só configura o botão AddConsole se houver tabs
            uiCommandBuilder.set("#Content #ManagerButtons #AddConsoleButton #AddConsoleLabel.TextSpans", 
                Message.raw(LanguageManager.getTranslation("gui_shop_manager_add_console")).color(Color.WHITE));
            // Só configura o botão AddCash se houver tabs
            uiCommandBuilder.set("#Content #ManagerButtons #AddCashButton #AddCashLabel.TextSpans", 
                Message.raw(LanguageManager.getTranslation("gui_shop_manager_add_cash")).color(Color.WHITE));
        } else {
            // Remove os botões se não houver tabs
            try {
                uiCommandBuilder.remove("#Content #ManagerButtons #AddConsoleButton");
                uiCommandBuilder.remove("#Content #ManagerButtons #AddCashButton");
            } catch (Exception e) {
                // Se remove() não existir, apenas não configura o botão
            }
        }
        
        uiCommandBuilder.set("#Content #ManagerButtons #AddTabButton #AddTabLabel.TextSpans", 
            Message.raw(LanguageManager.getTranslation("gui_shop_manager_add_tab")).color(Color.WHITE));
        
        // Se não houver tab selecionada, seleciona a primeira tab disponível
        if (this.selectedTab == null || this.selectedTab.isEmpty()) {
            if (hasTabs) {
                this.selectedTab = tabs.get(0);
            }
        }
        
        // Constrói as tabs
        this.buildTabs(uiCommandBuilder, uiEventBuilder);
        
        // Constrói os itens
        this.buildShopItems(ref, store, uiCommandBuilder, uiEventBuilder, this.selectedTab);
        
        // Adiciona eventos nos botões apenas se houver tabs
        if (hasTabs) {
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                "#Content #ManagerButtons #AddItemButton", 
                EventData.of("Action", "add_item"));
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                "#Content #ManagerButtons #AddConsoleButton", 
                EventData.of("Action", "add_console"));
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                "#Content #ManagerButtons #AddCashButton", 
                EventData.of("Action", "add_cash"));
        }
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
            "#Content #ManagerButtons #AddTabButton", 
            EventData.of("Action", "add_tab"));
    }

    private void buildTabs(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
        
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
                                String selectedTab) {
        List<ShopItem> items;
        if (selectedTab == null || selectedTab.isEmpty()) {
            items = new java.util.ArrayList<>();
        } else {
            items = ShopManager.getInstance().getItemsByTab(selectedTab, this.shopId);
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
        
        for (ShopItem item : items) {
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#Content #ShopContent", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            
            commandBuilder.append("#Content #ShopContent[" + rowIndex + "]", "Pages/EconomySystem_Shop_ItemCard.ui");
            
            commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", item.getItemId());
            
            Message itemName;
            // Se for comando console, usa o displayName; caso contrário, usa o nome do item
            if (item.isConsoleCommand() && item.getDisplayName() != null && !item.getDisplayName().isEmpty()) {
                itemName = Message.raw(item.getDisplayName());
            } else {
                com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(item.getItemId());
                if (itemConfig != null) {
                    itemName = Message.translation(itemConfig.getTranslationKey());
                } else {
                    itemName = Message.translation("item." + item.getItemId());
                }
            }
            
            commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", itemName);
            
            // Define o preço abaixo do nome do item (igual ao /shop)
            // Se for Cash, mostra "10 Cash" em vez de "B: 10"
            // Só mostra preços que são maiores que 0
            // Usa formatação resumida para valores grandes (1k, 1kk, 1kkk)
            StringBuilder priceTextBuilder = new StringBuilder();
            if (item.getPriceBuy() > 0) {
                if (item.isUseCash()) {
                    // Para Cash, mostra "10 Cash" em vez de "B: 10"
                    priceTextBuilder.append(com.economy.util.CurrencyFormatter.formatNumberOnlyShort(item.getPriceBuy()))
                        .append(" ")
                        .append(LanguageManager.getTranslation("gui_shop_manager_payment_cash"));
                } else {
                    priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_buy_label"))
                        .append(com.economy.util.CurrencyFormatter.formatNumberOnlyShort(item.getPriceBuy()));
                }
            }
            if (item.getPriceSell() > 0) {
                if (priceTextBuilder.length() > 0) {
                    priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_separator"));
                }
                priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_sell_label"))
                    .append(com.economy.util.CurrencyFormatter.formatNumberOnlyShort(item.getPriceSell()));
            }
            
            // Só define o texto se houver pelo menos um preço > 0
            if (priceTextBuilder.length() > 0) {
                commandBuilder.set("#Content #ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.TextSpans", Message.raw(priceTextBuilder.toString()));
            } else {
                // Se ambos forem 0, oculta o label de preço
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
            
            // Sempre mostra os preços (mesmo se forem 0)
            tooltip.separator();
            String buyLabel = item.isUseCash() 
                ? LanguageManager.getTranslation("gui_shop_manager_payment_cash") 
                : LanguageManager.getTranslation("gui_shop_tooltip_buy");
            tooltip.append(Message.raw(buyLabel + ": ").color(new Color(255, 170, 0)).bold(true))
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
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ShopManagerGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            
            String[] parts = data.action.split(":");
            if (parts.length < 1) {
                return;
            }
            
            String actionType = parts[0];
            
            if ("tab".equals(actionType) && parts.length >= 2) {
                String tabName = parts[1];
                this.selectedTab = tabName;
                player.getPageManager().openCustomPage(ref, store, 
                    new ShopManagerGui(this.playerRef, CustomPageLifetime.CanDismiss, tabName, this.shopId));
                return;
            }
            
            if ("add_item".equals(actionType)) {
                // Garante que há uma tab selecionada
                String tabToUse = this.selectedTab;
                if (tabToUse == null || tabToUse.isEmpty()) {
                    List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
                    if (tabs.isEmpty()) {
                        // Se não houver tabs, mostra mensagem de erro
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        return;
                    }
                    tabToUse = tabs.get(0);
                }
                
                // Abre GUI para adicionar item na tab selecionada
                player.getPageManager().openCustomPage(ref, store, 
                    new ShopManagerAddItemGui(this.playerRef, CustomPageLifetime.CanDismiss, tabToUse, this.shopId));
                return;
            }
            
            if ("add_console".equals(actionType)) {
                // Garante que há uma tab selecionada
                String tabToUse = this.selectedTab;
                if (tabToUse == null || tabToUse.isEmpty()) {
                    List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
                    if (tabs.isEmpty()) {
                        // Se não houver tabs, mostra mensagem de erro
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        return;
                    }
                    tabToUse = tabs.get(0);
                }
                
                // Abre GUI para adicionar comando console
                player.getPageManager().openCustomPage(ref, store, 
                    new com.economy.gui.ShopManagerAddConsoleGui(this.playerRef, CustomPageLifetime.CanDismiss, tabToUse, this.shopId));
                return;
            }
            
            if ("add_cash".equals(actionType)) {
                // Garante que há uma tab selecionada
                String tabToUse = this.selectedTab;
                if (tabToUse == null || tabToUse.isEmpty()) {
                    List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
                    if (tabs.isEmpty()) {
                        // Se não houver tabs, mostra mensagem de erro
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        return;
                    }
                    tabToUse = tabs.get(0);
                }
                
                // Abre GUI para adicionar item Cash
                player.getPageManager().openCustomPage(ref, store, 
                    new com.economy.gui.ShopManagerAddCashGui(this.playerRef, CustomPageLifetime.CanDismiss, tabToUse, this.shopId));
                return;
            }
            
            if ("add_tab".equals(actionType)) {
                // Abre GUI para adicionar tab
                player.getPageManager().openCustomPage(ref, store, 
                    new ShopManagerAddTabGui(this.playerRef, CustomPageLifetime.CanDismiss, this.shopId));
                return;
            }
            
            if ("remove_tab".equals(actionType) && parts.length >= 2) {
                String tabName = parts[1];
                // Abre confirmação para remover tab
                player.getPageManager().openCustomPage(ref, store, 
                    new ShopManagerRemoveTabConfirmationGui(this.playerRef, CustomPageLifetime.CanDismiss, tabName, this.selectedTab, this.shopId));
                return;
            }
            
            if ("remove_item".equals(actionType) && parts.length >= 2) {
                try {
                    int uniqueId = Integer.parseInt(parts[1]);
                    ShopItem shopItem = ShopManager.getInstance().getItem(uniqueId, this.shopId);
                    if (shopItem != null) {
                        // Abre confirmação para remover item
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerRemoveItemConfirmationGui(this.playerRef, CustomPageLifetime.CanDismiss, shopItem, this.selectedTab, this.shopId));
                    }
                } catch (NumberFormatException e) {
                    // Ignora
                }
                return;
            }
            
            if ("edit_item".equals(actionType) && parts.length >= 2) {
                try {
                    int uniqueId = Integer.parseInt(parts[1]);
                    ShopItem shopItem = ShopManager.getInstance().getItem(uniqueId, this.shopId);
                    if (shopItem != null) {
                        // Abre GUI para editar preço do item
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerEditItemGui(this.playerRef, CustomPageLifetime.CanDismiss, shopItem, this.selectedTab, this.shopId));
                    }
                } catch (NumberFormatException e) {
                    // Ignora
                }
                return;
            }
        }
    }

    public static class ShopManagerGuiData {
        private String action;
        private String selectedTab;
        
        public static final BuilderCodec<ShopManagerGuiData> CODEC = BuilderCodec.<ShopManagerGuiData>builder(ShopManagerGuiData.class, ShopManagerGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("SelectedTab", Codec.STRING),
                        (data, value, extraInfo) -> data.selectedTab = value,
                        (data, extraInfo) -> data.selectedTab != null ? data.selectedTab : "").add()
                .build();
    }
}

