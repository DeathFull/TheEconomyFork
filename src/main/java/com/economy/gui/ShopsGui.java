package com.economy.gui;

import com.economy.economy.EconomyManager;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.UUID;

public class ShopsGui extends InteractiveCustomUIPage<ShopsGui.ShopsGuiData> {

    private final PlayerRef playerRef;

    public ShopsGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, ShopsGuiData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        // Usa arquivo UI específico
        uiCommandBuilder.append("Pages/EconomySystem_Shop_PlayerList_Gui.ui");
        
        // Define o título traduzido dinamicamente
        String titleText = LanguageManager.getTranslation("gui_shops_title_player");
        uiCommandBuilder.set("#Title #ShopsTitle.Text", titleText);
        
        buildShopsList(ref, store, uiCommandBuilder, uiEventBuilder);
    }

    private void buildShopsList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        List<UUID> openShopOwners = PlayerShopManager.getInstance().getOpenShopOwners();
        
        commandBuilder.clear("#ShopContent");
        
        if (openShopOwners.isEmpty()) {
            commandBuilder.append("#ShopContent", "Pages/EconomySystem_Shop_EmptyMessage.ui");
            commandBuilder.set("#ShopContent[0] #EmptyMessageLabel.TextSpans", 
                Message.raw(com.economy.util.LanguageManager.getTranslation("gui_shops_empty")).color(new Color(204, 204, 204)));
            return;
        }
        
        int rowIndex = 0;
        int cardsInCurrentRow = 0;
        
        for (UUID ownerUuid : openShopOwners) {
            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#ShopContent", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            
            commandBuilder.append("#ShopContent[" + rowIndex + "]", "Pages/EconomySystem_Shop_ItemCard.ui");
            
            String ownerName = EconomyManager.getInstance().getPlayerName(ownerUuid);
            if (ownerName == null || ownerName.isEmpty()) {
                ownerName = "Unknown";
            }
            
            // Usa o ícone personalizado da loja, ou um ícone genérico se não tiver
            String shopIcon = PlayerShopManager.getInstance().getShopIcon(ownerUuid);
            if (shopIcon == null || shopIcon.isEmpty()) {
                shopIcon = "Block_Chest"; // Ícone padrão
            }
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", shopIcon);
            
            // Verifica se a loja tem nome personalizado
            String shopNameText;
            String customName = PlayerShopManager.getInstance().getShopCustomName(ownerUuid);
            if (customName != null && !customName.isEmpty()) {
                // Usa o nome personalizado
                shopNameText = customName;
            } else {
                // Usa o nome padrão "Loja de {player}"
                java.util.Map<String, String> shopNamePlaceholders = new java.util.HashMap<>();
                shopNamePlaceholders.put("player", ownerName);
                shopNameText = com.economy.util.LanguageManager.getTranslation("gui_shops_player_shop", shopNamePlaceholders);
            }
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", 
                Message.raw(shopNameText).color(new Color(255, 255, 255)));
            
            // Conta quantos itens a loja tem (incluindo itens com estoque 0)
            List<com.economy.playershop.PlayerShopItem> items = PlayerShopManager.getInstance().getItemsByOwner(ownerUuid);
            int itemCount = items != null ? items.size() : 0;
            
            String itemsCountText = itemCount + " " + com.economy.util.LanguageManager.getTranslation("gui_shops_items_count");
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemPrice.TextSpans", 
                Message.raw(itemsCountText).color(new Color(200, 200, 200)));
            
            // Tooltip simples
            com.economy.util.MessageHelper.ML tooltip = com.economy.util.MessageHelper.multiLine();
            tooltip.append(Message.raw(shopNameText).bold(true).color(new Color(85, 255, 85)))
                    .nl()
                    .separator()
                    .append(Message.raw(com.economy.util.LanguageManager.getTranslation("gui_shops_tooltip_items") + ": ").color(new Color(255, 170, 0)).bold(true))
                    .append(Message.raw(String.valueOf(itemCount)).color(new Color(255, 255, 255)))
                    .nl()
                    .separator()
                    .append(Message.raw(com.economy.util.LanguageManager.getTranslation("gui_shops_tooltip_click")).color(new Color(85, 255, 85)));
            
            commandBuilder.set("#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            
            // Evento de clique para abrir a loja
            String itemPath = "#ShopContent[" + rowIndex + "][" + cardsInCurrentRow + "]";
            String ownerUuidStr = ownerUuid.toString();
            
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, itemPath, 
                EventData.of("Action", "open:" + ownerUuidStr));
            
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 7) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ShopsGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            
            String[] parts = data.action.split(":");
            if (parts.length < 2) {
                return;
            }
            
            String actionType = parts[0];
            if (!"open".equals(actionType)) {
                return;
            }
            
            UUID ownerUuid;
            try {
                ownerUuid = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException e) {
                return;
            }
            
            // Abre a loja do jogador
            player.getPageManager().openCustomPage(ref, store, 
                new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
        }
    }

    public static class ShopsGuiData {
        private String action;
        
        public static final BuilderCodec<ShopsGuiData> CODEC = BuilderCodec.<ShopsGuiData>builder(ShopsGuiData.class, ShopsGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .build();
    }
}

