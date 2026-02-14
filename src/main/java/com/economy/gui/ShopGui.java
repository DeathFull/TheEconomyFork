package com.economy.gui;

import com.economy.Main;
import com.economy.shop.ShopItem;
import com.economy.shop.ShopManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopGui extends InteractiveCustomUIPage<ShopGui.ShopGuiData> {

    private final PlayerRef playerRef;
    private String selectedTab = "";
    private int shopId; // ID da loja (0 para /shop, 1+ para NPCs)

    public ShopGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        this(playerRef, lifetime, "", 0);
    }

    public ShopGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab) {
        this(playerRef, lifetime, selectedTab, 0);
    }
    
    public ShopGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, int shopId) {
        this(playerRef, lifetime, "", shopId);
    }
    
    public ShopGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab, int shopId) {
        super(playerRef, lifetime, ShopGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        this.shopId = shopId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull com.hypixel.hytale.server.core.ui.builder.UICommandBuilder uiCommandBuilder, @Nonnull com.hypixel.hytale.server.core.ui.builder.UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        // Usa arquivo UI específico
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Admin_Gui.ui");
        
        // Define o título traduzido dinamicamente
        String titleText = com.economy.util.LanguageManager.getTranslation("gui_shop_title");
        uiCommandBuilder.set("#Title #AdminShopTitle.Text", titleText);
        
        // Se não houver tab selecionada, seleciona a primeira tab disponível
        if (this.selectedTab == null || this.selectedTab.isEmpty()) {
            java.util.List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
            if (!tabs.isEmpty()) {
                this.selectedTab = tabs.get(0);
            }
        }
        
        // Constrói as tabs
        this.buildTabs(uiCommandBuilder, uiEventBuilder);
        
        // Se o jogador tiver permissão e for uma loja de NPC (shopId > 0), mostra botão de editar
        Player player = store.getComponent(ref, Player.getComponentType());
        boolean canEdit = player != null && com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_MANAGER);
        if (canEdit && this.shopId > 0) {
            // Configura o botão de editar (mesmo lugar do Add Item no ShopManagerGui)
            String editButtonText = com.economy.util.LanguageManager.getTranslation("gui_shop_manager_title");
            uiCommandBuilder.set("#Content #ManagerButtons #ManageShopButton #ManageShopLabel.TextSpans", 
                Message.raw(editButtonText).color(new Color(255, 255, 255)));
            uiCommandBuilder.set("#Content #ManagerButtons #ManageShopButton.Visible", true);
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                "#Content #ManagerButtons #ManageShopButton", 
                EventData.of("Action", "edit_shop"));
        } else {
            // Oculta o botão se não tiver permissão ou não for loja de NPC
            uiCommandBuilder.set("#Content #ManagerButtons #ManageShopButton.Visible", false);
        }
        
        // Sempre reconstrói a lista de itens toda vez que a GUI é aberta
        // Isso garante que itens adicionados via /shop add apareçam imediatamente
        this.buildShopItems(ref, store, uiCommandBuilder, uiEventBuilder, this.selectedTab);
    }

    private void buildTabs(@Nonnull com.hypixel.hytale.server.core.ui.builder.UICommandBuilder commandBuilder, @Nonnull com.hypixel.hytale.server.core.ui.builder.UIEventBuilder eventBuilder) {
        java.util.List<String> tabs = ShopManager.getInstance().getAllTabs(this.shopId);
        
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
            
            // Define o texto da tab no Label dentro do Group
            commandBuilder.set(tabButtonPath + " #TabLabel.TextSpans", Message.raw(tabName).color(new Color(255, 255, 255)));
            
            // TODO: Adicionar cores de fundo para tabs selecionadas/não selecionadas
            // Por enquanto, usa o estilo padrão do botão
            
            // Adiciona evento de clique
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, tabButtonPath, 
                EventData.of("Action", "tab:" + tabName));
            
            currentIndex++;
        }
    }

    private void buildShopItems(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull com.hypixel.hytale.server.core.ui.builder.UICommandBuilder commandBuilder, @Nonnull com.hypixel.hytale.server.core.ui.builder.UIEventBuilder eventBuilder, String selectedTab) {
        // Busca os itens mais recentes do ShopManager filtrados por tab
        // Sempre filtra por tab - se não houver tab selecionada, mostra lista vazia
        List<ShopItem> items;
        if (selectedTab == null || selectedTab.isEmpty()) {
            items = new java.util.ArrayList<>(); // Não mostra itens se não houver tab selecionada
        } else {
            items = ShopManager.getInstance().getItemsByTab(selectedTab, this.shopId);
        }
        
        // Verifica se o jogador tem permissão para adicionar itens (para mostrar Unique ID)
        Player player = store.getComponent(ref, Player.getComponentType());
        boolean canAdd = player != null && com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD);
        
        // Limpa o container para garantir que não há itens antigos
        commandBuilder.clear("#ShopContent");
        
        if (items.isEmpty()) {
            // Se não houver itens, mostra uma mensagem usando um arquivo UI
            commandBuilder.append("#ShopContent", "Pages/EconomySystem_Shop_EmptyMessage.ui");
            // Quando anexa um arquivo UI, o seletor precisa incluir o índice do elemento anexado
            commandBuilder.set("#ShopContent[0] #EmptyMessageLabel.TextSpans", Message.raw(LanguageManager.getTranslation("gui_shop_empty")).color(new Color(204, 204, 204)));
            return;
        }
        
        int rowIndex = 0;
        int cardsInCurrentRow = 0;
        
        for (ShopItem item : items) {
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#ShopContent", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            
            // Adiciona o card do item
            commandBuilder.append("#ShopContent[" + rowIndex + "]", "Pages/EconomySystem_Shop_ItemCard.ui");
            
            // Define o ItemId do ícone
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", item.getItemId());
            
            // Obtém o nome do item - se for comando console, usa displayName
            Message itemName;
            if (item.isConsoleCommand() && item.getDisplayName() != null && !item.getDisplayName().isEmpty()) {
                // Para comandos console, usa o displayName
                itemName = Message.raw(item.getDisplayName());
            } else {
                // Para itens normais, usa o nome traduzido do item
                com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(item.getItemId());
                if (itemConfig != null) {
                    itemName = Message.translation(itemConfig.getTranslationKey());
                } else {
                    // Fallback: tenta usar o padrão de tradução do Hytale
                    itemName = Message.translation("item." + item.getItemId());
                }
            }
            
            // Define o nome do item traduzido
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", itemName);
            
            // Define os preços embaixo do nome no formato "B: 200 / S: 100" (Buy / Sell)
            // Se for Cash, mostra "10 Cash" em vez de "B: 10"
            // Só mostra preços que são maiores que 0
            // Usa formatação resumida para valores grandes (1k, 1kk, 1kkk)
            StringBuilder priceTextBuilder = new StringBuilder();
            if (item.getPriceBuy() > 0) {
                if (item.isUseCash()) {
                    // Para Cash, mostra "10 Cash" em vez de "B: 10"
                    priceTextBuilder.append(CurrencyFormatter.formatNumberOnlyShort(item.getPriceBuy()))
                        .append(" ")
                        .append(LanguageManager.getTranslation("gui_shop_manager_payment_cash"));
                } else {
                    priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_buy_label"))
                        .append(CurrencyFormatter.formatNumberOnlyShort(item.getPriceBuy()));
                }
            }
            if (item.getPriceSell() > 0) {
                if (priceTextBuilder.length() > 0) {
                    priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_separator"));
                }
                priceTextBuilder.append(LanguageManager.getTranslation("gui_shop_price_sell_label"))
                    .append(CurrencyFormatter.formatNumberOnlyShort(item.getPriceSell()));
            }
            
            // Só define o texto se houver pelo menos um preço > 0
            if (priceTextBuilder.length() > 0) {
                commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.TextSpans", Message.raw(priceTextBuilder.toString()));
            } else {
                // Se ambos forem 0, oculta o label de preço
                commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.Visible", false);
            }
            
            // Cria o tooltip com estilo melhorado e traduzível
            MessageHelper.ML tooltip = MessageHelper.multiLine();
            
            // Título do item em destaque (verde claro, negrito)
            tooltip.append(itemName.bold(true).color(new Color(85, 255, 85)))
                    .nl()
                    .separator();
            
            // Informações do item com tradução
            // Só mostra o Unique ID se tiver permissão de adicionar itens
            if (canAdd) {
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_unique_id") + ": ").color(new Color(255, 170, 0)).bold(true))
                        .append(Message.raw(String.valueOf(item.getUniqueId())).color(new Color(255, 255, 255)))
                        .nl();
            }
            tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_quantity") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(String.valueOf(item.getQuantity())).color(new Color(255, 255, 255)))
                    .nl();
            
            // Se for comando console, mostra apenas que é um comando console (não mostra o comando)
            if (item.isConsoleCommand()) {
                tooltip.separator();
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_console_command")).color(new Color(255, 170, 0)).bold(true))
                        .nl();
            }
            
            // Preços com estilo melhorado e tradução
            if (item.getPriceBuy() > 0 || item.getPriceSell() > 0) {
                tooltip.separator();
                
                if (item.getPriceBuy() > 0) {
                    // Tooltip mostra valor completo (não resumido)
                    String buyLabel = item.isUseCash() 
                        ? LanguageManager.getTranslation("gui_shop_manager_payment_cash") 
                        : LanguageManager.getTranslation("gui_shop_tooltip_buy");
                    tooltip.append(Message.raw(buyLabel + ": ").color(new Color(255, 170, 0)).bold(true))
                            .append(Message.raw(CurrencyFormatter.format(item.getPriceBuy())).color(new Color(255, 85, 85)).bold(true))
                            .nl();
                }
                
                if (item.getPriceSell() > 0) {
                    // Tooltip mostra valor completo (não resumido)
                    tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_sell") + ": ").color(new Color(255, 170, 0)).bold(true))
                            .append(Message.raw(CurrencyFormatter.format(item.getPriceSell())).color(new Color(85, 255, 85)).bold(true))
                            .nl();
                }
            }
            
            // Instruções de botão com tradução
            boolean invertButtons = Main.CONFIG != null && Main.CONFIG.get() != null && Main.CONFIG.get().isInvertBuyButtonAction();
            
            if (item.getPriceBuy() > 0 || item.getPriceSell() > 0) {
                tooltip.separator();
                
                if (item.isConsoleCommand()) {
                    // Para comandos console, só mostra compra (não tem venda)
                    if (item.getPriceBuy() > 0) {
                        String buyText = invertButtons 
                            ? LanguageManager.getTranslation("gui_shop_tooltip_right_click_buy")
                            : LanguageManager.getTranslation("gui_shop_tooltip_left_click_buy");
                        tooltip.append(Message.raw(buyText).color(new Color(85, 255, 85)));
                    }
                } else {
                    if (item.getPriceBuy() > 0 && item.getPriceSell() > 0) {
                        // Ambos disponíveis
                        String buyText = invertButtons 
                            ? LanguageManager.getTranslation("gui_shop_tooltip_right_click_buy")
                            : LanguageManager.getTranslation("gui_shop_tooltip_left_click_buy");
                        String sellText = invertButtons 
                            ? LanguageManager.getTranslation("gui_shop_tooltip_left_click_sell")
                            : LanguageManager.getTranslation("gui_shop_tooltip_right_click_sell");
                        tooltip.append(Message.raw(buyText).color(new Color(85, 255, 85)))
                                .nl()
                                .append(Message.raw(sellText).color(new Color(255, 255, 85)));
                    } else if (item.getPriceBuy() > 0) {
                        // Apenas compra disponível
                        String buyText = invertButtons 
                            ? LanguageManager.getTranslation("gui_shop_tooltip_right_click_buy")
                            : LanguageManager.getTranslation("gui_shop_tooltip_left_click_buy");
                        tooltip.append(Message.raw(buyText).color(new Color(85, 255, 85)));
                    } else if (item.getPriceSell() > 0) {
                        // Apenas venda disponível
                        String sellText = invertButtons 
                            ? LanguageManager.getTranslation("gui_shop_tooltip_left_click_sell")
                            : LanguageManager.getTranslation("gui_shop_tooltip_right_click_sell");
                        tooltip.append(Message.raw(sellText).color(new Color(255, 255, 85)));
                    }
                }
            } else {
                tooltip.separator();
                tooltip.append(Message.raw(LanguageManager.getTranslation("gui_shop_tooltip_not_available")).color(new Color(128, 128, 128)));
            }
            
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            
            // Adiciona eventos de clique
            String itemPath = "#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "]";
            String uniqueIdStr = String.valueOf(item.getUniqueId());
            
            // Adiciona eventos de clique apenas se os preços correspondentes forem > 0
            // Se ambos os preços forem 0, não adiciona nenhum evento (item não clicável)
            if (item.getPriceBuy() <= 0 && item.getPriceSell() <= 0) {
                // Não adiciona eventos de clique - o item não será clicável
            } else {
                // Determina qual evento usar para comprar e vender baseado na configuração
                if (invertButtons) {
                    // Invertido: Activating = vender, RightClicking = comprar
                    if (item.getPriceSell() > 0) {
                        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, itemPath, 
                            EventData.of("Action", "sell:" + uniqueIdStr));
                    }
                    if (item.getPriceBuy() > 0) {
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
                    if (item.getPriceBuy() > 0) {
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
            }
            
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 7) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ShopGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            
            // Parse do action: formato "action:value" ou "tab:tabName" ou "edit_shop"
            String[] parts = data.action.split(":");
            String actionType = parts.length > 0 ? parts[0] : "";
            
            // Se for editar loja (apenas para lojas de NPC com permissão)
            if ("edit_shop".equals(actionType) && this.shopId > 0) {
                // Abre o ShopManagerGui para editar esta loja específica
                player.getPageManager().openCustomPage(ref, store, 
                    new com.economy.gui.ShopManagerGui(this.playerRef, CustomPageLifetime.CanDismiss, this.selectedTab, this.shopId));
                return;
            }
            
            if (parts.length < 2) {
                return;
            }
            
            // Se for uma mudança de tab, atualiza e reconstrói
            if ("tab".equals(actionType)) {
                String tabName = parts[1];
                // Atualiza a tab selecionada e reconstrói a GUI
                this.selectedTab = tabName;
                // Reconstrói a GUI com a nova tab selecionada
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store, 
                        new ShopGui(this.playerRef, CustomPageLifetime.CanDismiss, tabName, this.shopId));
                }
                return;
            }
            
            // Ações de compra/venda
            int uniqueId;
            try {
                uniqueId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return;
            }
            
            ShopItem shopItem = ShopManager.getInstance().getItem(uniqueId, this.shopId);
            if (shopItem == null) {
                player.sendMessage(com.economy.commands.CommandMessages.PLAYER_NOT_FOUND());
                return;
            }

            int quantity = shopItem.getQuantity(); // Usa a quantidade padrão do item
            
            // Abre a GUI de confirmação por cima da loja, passando a tab selecionada e shopId
            player.getPageManager().openCustomPage(ref, store, 
                new ShopConfirmationGui(this.playerRef, CustomPageLifetime.CanDismiss, shopItem, actionType, quantity, this.selectedTab, this.shopId));
        }
    }

    public static class ShopGuiData {
        private String action;
        private String selectedTab;
        
        public static final BuilderCodec<ShopGuiData> CODEC = BuilderCodec.<ShopGuiData>builder(ShopGuiData.class, ShopGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("SelectedTab", Codec.STRING),
                        (data, value, extraInfo) -> data.selectedTab = value,
                        (data, extraInfo) -> data.selectedTab != null ? data.selectedTab : "").add()
                .build();
    }
}

