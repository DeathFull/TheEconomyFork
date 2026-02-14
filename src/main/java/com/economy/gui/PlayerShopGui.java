package com.economy.gui;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.LanguageManager;
import com.economy.util.MessageHelper;
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
import java.util.List;
import java.util.UUID;

public class PlayerShopGui extends InteractiveCustomUIPage<PlayerShopGui.PlayerShopGuiData> {

    private final PlayerRef playerRef;
    private final UUID shopOwnerUuid;
    private String selectedTab = "";

    public PlayerShopGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, UUID shopOwnerUuid) {
        super(playerRef, lifetime, PlayerShopGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopOwnerUuid = shopOwnerUuid;
    }
    
    public PlayerShopGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, UUID shopOwnerUuid, String selectedTab) {
        super(playerRef, lifetime, PlayerShopGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopOwnerUuid = shopOwnerUuid;
        this.selectedTab = selectedTab != null ? selectedTab : "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        // Usa arquivo UI específico
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Player_Gui.ui");
        
        // Obtém o nome do jogador dono da loja
        String ownerName = EconomyManager.getInstance().getPlayerName(shopOwnerUuid);
        if (ownerName == null || ownerName.isEmpty()) {
            ownerName = "Unknown";
        }
        
        // Cria o título traduzido com o nome do jogador
        java.util.Map<String, String> titlePlaceholders = new java.util.HashMap<>();
        titlePlaceholders.put("player", ownerName);
        String titleText = LanguageManager.getTranslation("gui_playershop_title", titlePlaceholders);
        
        // Define o título dinamicamente
        uiCommandBuilder.set("#Title #PlayerShopTitle.Text", titleText);
        
        // Adiciona botão de voltar para /shops
        String backButtonText = LanguageManager.getTranslation("gui_shop_button_back");
        if (backButtonText == null || backButtonText.isEmpty()) {
            backButtonText = "Voltar";
        }
        uiCommandBuilder.set("#BackButton.Text", backButtonText);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", 
            EventData.of("Action", "back"));
        
        // Se não houver tab selecionada, seleciona a primeira tab disponível
        if (this.selectedTab == null || this.selectedTab.isEmpty()) {
            java.util.List<String> tabs = PlayerShopManager.getInstance().getAllTabs(shopOwnerUuid);
            if (!tabs.isEmpty()) {
                this.selectedTab = tabs.get(0);
            }
        }
        
        // Constrói as tabs
        this.buildTabs(uiCommandBuilder, uiEventBuilder);
        
        // Constrói os itens filtrados por tab
        this.buildShopItems(ref, store, uiCommandBuilder, uiEventBuilder, this.selectedTab);
    }

    private void buildTabs(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        java.util.List<String> tabs = PlayerShopManager.getInstance().getAllTabs(shopOwnerUuid);
        
        // Limita a 7 tabs
        int maxTabs = Math.min(tabs.size(), 7);
        
        // Limpa o container de tabs para garantir que não há tabs antigas
        commandBuilder.clear("#ShopTabs");
        
        if (maxTabs == 0) {
            return; // Não há tabs para mostrar
        }
        
        // Adiciona cada tab dinamicamente diretamente no #ShopTabs
        int currentIndex = 0;
        for (int i = 0; i < maxTabs; i++) {
            String tabName = tabs.get(i);
            
            // Adiciona espaçamento antes da tab (exceto na primeira)
            if (i > 0) {
                commandBuilder.appendInline("#ShopTabs", "Label { Anchor: (Width: 3); }");
                currentIndex++;
            }
            
            // Adiciona o botão da tab usando o arquivo UI
            commandBuilder.append("#ShopTabs", "Pages/EconomySystem_Shop_TabButton.ui");
            
            // Define o caminho do botão (Button é o elemento raiz anexado)
            String tabButtonPath = "#ShopTabs[" + currentIndex + "]";
            
            // Define o texto da tab no Label dentro do Button
            commandBuilder.set(tabButtonPath + " #TabLabel.TextSpans", Message.raw(tabName).color(new Color(255, 255, 255)));
            
            // Adiciona evento de clique
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, tabButtonPath, 
                EventData.of("Action", "tab:" + tabName));
            
            currentIndex++;
        }
    }

    private void buildShopItems(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, String selectedTab) {
        // Busca os itens filtrados por tab
        List<PlayerShopItem> items;
        if (selectedTab == null || selectedTab.isEmpty()) {
            items = new java.util.ArrayList<>(); // Não mostra itens se não houver tab selecionada
        } else {
            items = PlayerShopManager.getInstance().getItemsByTab(shopOwnerUuid, selectedTab);
        }
        
        // Mostra todos os itens, mesmo com estoque 0 (mas desabilita compra se estoque = 0)
        
        commandBuilder.clear("#ShopContent");
        
        if (items.isEmpty()) {
            commandBuilder.append("#ShopContent", "Pages/EconomySystem_Shop_EmptyMessage.ui");
            commandBuilder.set("#ShopContent[0] #EmptyMessageLabel.TextSpans", 
                Message.raw(LanguageManager.getTranslation("chat_playershop_empty")).color(new Color(204, 204, 204)));
            return;
        }
        
        int rowIndex = 0;
        int cardsInCurrentRow = 0;
        
        for (PlayerShopItem item : items) {
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#ShopContent", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            
            commandBuilder.append("#ShopContent[" + rowIndex + "]", "Pages/EconomySystem_Shop_ItemCard.ui");
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", item.getItemId());
            
            // Obtém o Item usando o ItemManager próprio (sem depender de mods de terceiros)
            Message itemName;
            com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(item.getItemId());
            if (itemConfig != null) {
                itemName = Message.translation(itemConfig.getTranslationKey());
            } else {
                // Fallback: tenta usar o padrão de tradução do Hytale
                itemName = Message.translation("item." + item.getItemId());
            }
            
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", itemName);
            
            // Mostra preços e estoque: "B: 200 S: 100\nStock: 10"
            StringBuilder priceText = new StringBuilder();
            if (item.getPriceBuy() > 0) {
                priceText.append(LanguageManager.getTranslation("gui_shop_price_buy_label"))
                    .append(CurrencyFormatter.formatNumberOnlyShort(item.getPriceBuy()));
            }
            if (item.getPriceSell() > 0) {
                if (priceText.length() > 0) {
                    priceText.append(" ");
                }
                priceText.append(LanguageManager.getTranslation("gui_shop_price_sell_label"))
                    .append(CurrencyFormatter.formatNumberOnlyShort(item.getPriceSell()));
            }
            if (priceText.length() > 0) {
                priceText.append("\n");
            }
            priceText.append(LanguageManager.getTranslation("gui_playershop_stock_label"))
                .append(item.getStock());
            
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.TextSpans", Message.raw(priceText.toString()));
            
            // Tooltip
            MessageHelper.ML tooltip = MessageHelper.multiLine();
            tooltip.append(itemName.bold(true).color(new Color(85, 255, 85)))
                    .nl()
                    .separator();
            
            // Verifica se é a própria loja antes de mostrar o Unique ID
            UUID ownerUuid = item.getOwnerUuid();
            boolean isOwnShop = ownerUuid != null && ownerUuid.equals(playerRef.getUuid());
            
            // Só mostra o Unique ID se for o dono da loja
            if (isOwnShop) {
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_unique_id") + ": ").color(new Color(255, 170, 0)).bold(true))
                        .append(Message.raw(String.valueOf(item.getUniqueId())).color(new Color(255, 255, 255)))
                        .nl();
            }
            
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_playershop_tooltip_stock") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(String.valueOf(item.getStock())).color(new Color(255, 255, 255)))
                    .nl();
            
            if (item.getPriceBuy() > 0) {
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_buy") + ": ").color(new Color(255, 170, 0)).bold(true))
                        .append(Message.raw(CurrencyFormatter.format(item.getPriceBuy())).color(new Color(255, 85, 85)).bold(true))
                        .nl();
            }
            
            if (item.getPriceSell() > 0) {
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_sell") + ": ").color(new Color(255, 170, 0)).bold(true))
                        .append(Message.raw(CurrencyFormatter.format(item.getPriceSell())).color(new Color(85, 255, 85)).bold(true))
                        .nl();
            }
            
            // Mostra durabilidade no formato "Dur/DurMax" com valores inteiros
            if (item.getDurability() > 0.0 || item.getMaxDurability() > 0.0) {
                int durabilityInt = (int) Math.round(item.getDurability());
                int maxDurabilityInt = (int) Math.round(item.getMaxDurability() > 0.0 ? item.getMaxDurability() : 
                    com.economy.util.ItemManager.getMaxDurability(item.getItemId()));
                if (maxDurabilityInt > 0) {
                    tooltip.append(Message.raw("Durability: ").color(new Color(255, 170, 0)).bold(true))
                            .append(Message.raw(durabilityInt + "/" + maxDurabilityInt).color(new Color(255, 255, 255)))
                            .nl();
                }
            }
            
            tooltip.separator();
            
            // Mostra instruções de compra/venda no tooltip
            boolean invertButtons = Main.CONFIG != null && Main.CONFIG.get() != null && Main.CONFIG.get().isInvertBuyButtonAction();
            
            if (item.getPriceBuy() > 0 && !isOwnShop) {
                if (item.getStock() > 0) {
                    String buyText = invertButtons 
                        ? LanguageManager.getTranslation("gui_shop_tooltip_right_click_buy")
                        : LanguageManager.getTranslation("gui_shop_tooltip_left_click_buy");
                    tooltip.append(Message.raw(buyText).color(new Color(85, 255, 85)));
                } else {
                    tooltip.append(Message.raw(LanguageManager.getTranslation("chat_playershop_insufficient_stock")).color(Color.RED));
                }
            }
            
            if (item.getPriceSell() > 0 && !isOwnShop) {
                if (item.getPriceBuy() > 0) {
                    tooltip.nl();
                }
                String sellText = invertButtons 
                    ? LanguageManager.getTranslation("gui_shop_tooltip_left_click_sell")
                    : LanguageManager.getTranslation("gui_shop_tooltip_right_click_sell");
                tooltip.append(Message.raw(sellText).color(new Color(255, 255, 85)));
            }
            
            // Se for a própria loja, mostra instrução para remover
            if (isOwnShop) {
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_playershop_tooltip_click_remove")).color(new Color(255, 85, 85)));
            }
            
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            
            // Eventos de clique
            String itemPath = "#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "]";
            String uniqueIdStr = String.valueOf(item.getUniqueId());
            
            // Verifica se não é a própria loja antes de adicionar os eventos
            if (!isOwnShop) {
                if (invertButtons) {
                    // Invertido: Activating = vender, RightClicking = comprar
                    if (item.getPriceSell() > 0) {
                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, itemPath, 
                            EventData.of("Action", "sell:" + uniqueIdStr));
                    }
                    if (item.getPriceBuy() > 0 && item.getStock() > 0) {
                        try {
                            CustomUIEventBindingType rightClickType = CustomUIEventBindingType.valueOf("RightClicking");
                            eventBuilder.addEventBinding(rightClickType, itemPath, 
                                EventData.of("Action", "buy:" + uniqueIdStr));
                        } catch (Exception e) {
                            try {
                                CustomUIEventBindingType secondaryType = CustomUIEventBindingType.valueOf("SecondaryActivating");
                                eventBuilder.addEventBinding(secondaryType, itemPath, 
                                    EventData.of("Action", "buy:" + uniqueIdStr));
                            } catch (Exception e2) {
                                // Ignora
                            }
                        }
                    }
                } else {
                    // Padrão: Activating = comprar, RightClicking = vender
                    if (item.getPriceBuy() > 0 && item.getStock() > 0) {
                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, itemPath, 
                            EventData.of("Action", "buy:" + uniqueIdStr));
                    }
                    if (item.getPriceSell() > 0) {
                        try {
                            CustomUIEventBindingType rightClickType = CustomUIEventBindingType.valueOf("RightClicking");
                            eventBuilder.addEventBinding(rightClickType, itemPath, 
                                EventData.of("Action", "sell:" + uniqueIdStr));
                        } catch (Exception e) {
                            try {
                                CustomUIEventBindingType secondaryType = CustomUIEventBindingType.valueOf("SecondaryActivating");
                                eventBuilder.addEventBinding(secondaryType, itemPath, 
                                    EventData.of("Action", "sell:" + uniqueIdStr));
                            } catch (Exception e2) {
                                // Ignora
                            }
                        }
                    }
                }
            } else {
                // Se é a própria loja, adiciona evento para remover item
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, itemPath, 
                    EventData.of("Action", "remove:" + uniqueIdStr));
            }
            
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 7) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PlayerShopGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            
            // Se for ação de voltar, abre a lista de lojas (verifica antes do split)
            if ("back".equals(data.action)) {
                player.getPageManager().openCustomPage(ref, store, 
                    new ShopsGui(this.playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
            
            String[] parts = data.action.split(":");
            if (parts.length < 2) {
                return;
            }
            
            String actionType = parts[0];
            
            // Se for uma mudança de tab, atualiza e reconstrói
            if ("tab".equals(actionType)) {
                String tabName = parts[1];
                // Atualiza a tab selecionada e reconstrói a GUI
                this.selectedTab = tabName;
                // Reconstrói a GUI com a nova tab selecionada
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(this.playerRef, CustomPageLifetime.CanDismiss, shopOwnerUuid, tabName));
                }
                return;
            }
            
            int uniqueId;
            try {
                uniqueId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return;
            }
            
            PlayerShopItem shopItem = PlayerShopManager.getInstance().getItem(uniqueId);
            if (shopItem == null) {
                player.sendMessage(com.economy.commands.CommandMessages.PLAYER_NOT_FOUND());
                return;
            }
            
            UUID playerUuid = playerRef.getUuid();
            UUID ownerUuid = shopItem.getOwnerUuid();
            boolean isOwnShop = ownerUuid != null && playerUuid.equals(ownerUuid);
            
            // Se for ação de remover e for a própria loja, abre GUI de confirmação para remover
            if ("remove".equals(actionType) && isOwnShop) {
                player.getPageManager().openCustomPage(ref, store, 
                    new PlayerShopRemoveConfirmationGui(playerRef, CustomPageLifetime.CanDismiss, shopItem));
                return;
            }
            
            // Verifica se o jogador está tentando comprar/vender de si mesmo ANTES de abrir qualquer GUI
            if (isOwnShop) {
                player.sendMessage(LanguageManager.getMessage("chat_playershop_cannot_buy_from_self", Color.RED));
                return; // Retorna imediatamente, sem abrir GUI
            }
            
            if (!"buy".equals(actionType) && !"sell".equals(actionType)) {
                return;
            }
            
            // Abre GUI de confirmação (similar ao ShopConfirmationGui mas para PlayerShopItem)
            // A quantidade inicial é sempre 1 (o preço é por unidade)
            // Passa a tab selecionada para preservar ao reabrir
            int quantity = 1;
            player.getPageManager().openCustomPage(ref, store, 
                new PlayerShopConfirmationGui(playerRef, CustomPageLifetime.CanDismiss, shopItem, actionType, quantity, this.selectedTab));
        }
    }

    public static class PlayerShopGuiData {
        private String action;
        
        public static final BuilderCodec<PlayerShopGuiData> CODEC = BuilderCodec.<PlayerShopGuiData>builder(PlayerShopGuiData.class, PlayerShopGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .build();
    }
}

