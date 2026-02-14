package com.economy.gui;

import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
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

public class PlayerShopRemoveConfirmationGui extends InteractiveCustomUIPage<PlayerShopRemoveConfirmationGui.ConfirmationGuiData> {

    private final PlayerRef playerRef;
    private final PlayerShopItem shopItem;

    public PlayerShopRemoveConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                          PlayerShopItem shopItem) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopItem = shopItem;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Remove_Confirmation.ui");
        
        // Obtém o nome do item
        String itemNameText = getItemNameText(shopItem.getItemId());
        
        // Cria a mensagem de confirmação
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", itemNameText);
        placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
        String confirmationText = LanguageManager.getTranslation("gui_myshop_confirm_remove", placeholders);
        
        uiCommandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
        
        // Define os textos dos botões usando tradução
        String confirmText = LanguageManager.getTranslation("gui_shop_button_confirm");
        String cancelText = LanguageManager.getTranslation("gui_shop_button_cancel");
        uiCommandBuilder.set("#ConfirmButton.Text", confirmText);
        uiCommandBuilder.set("#CancelButton.Text", cancelText);
        
        // Configura os botões
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton", 
            EventData.of("Action", "confirm"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", 
            EventData.of("Action", "cancel"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull ConfirmationGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.action == null) {
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
        
        UUID playerUuid = playerRef.getUuid();
        
        if ("cancel".equals(data.action)) {
            // Fecha a GUI e volta para a loja
            player.getPageManager().openCustomPage(ref, store, 
                new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, shopItem.getOwnerUuid()));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Remove o item da loja
            world.execute(() -> {
                try {
                    PlayerShopManager shopManager = PlayerShopManager.getInstance();
                    
                    // Verifica se o item ainda existe e pertence ao jogador
                    PlayerShopItem item = shopManager.getItem(shopItem.getUniqueId());
                    if (item == null) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED));
                        return;
                    }
                    
                    if (item.getOwnerUuid() == null || !playerUuid.equals(item.getOwnerUuid())) {
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_not_owner", Color.RED));
                        return;
                    }
                    
                    // CRÍTICO: Verifica o estoque atual ANTES de remover
                    // Se o estoque estiver zerado, não devolve nada para evitar duplicação
                    int currentStock = item.getStock();
                    String itemId = item.getItemId();
                    double durability = item.getDurability();
                    boolean itemAdded = false;
                    
                    // IMPORTANTE: Só devolve itens se o estoque for maior que 0
                    // Usa getStock() (estoque atual) e NÃO getQuantity() (quantidade original)
                    if (currentStock > 0) {
                        // Devolve APENAS o estoque atual, não a quantidade original
                        // Usa a durabilidade máxima para restaurar o item corretamente
                        double maxDurability = item.getMaxDurability();
                        int added = InventoryHelper.addItemAndGetQuantityWithMaxDurability(player, itemId, currentStock, durability, maxDurability);
                        itemAdded = added > 0;
                        
                        // Se não conseguiu adicionar ao inventário, cancela a remoção para evitar perda de itens
                        if (!itemAdded) {
                            player.sendMessage(LanguageManager.getMessage("chat_myshop_error_inventory_full", Color.YELLOW));
                            player.sendMessage(LanguageManager.getMessage("chat_myshop_error_remove_item", Color.RED));
                            return;
                        }
                    }
                    // Se o estoque for 0, não devolve nada, apenas remove o registro da loja
                    
                    // Remove o item da loja APENAS se a devolução foi bem-sucedida (ou se estoque era 0)
                    if (shopManager.removeItem(shopItem.getUniqueId())) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_item_removed", Color.GREEN, placeholders));
                        
                        // Fecha a GUI e volta para a loja
                        player.getPageManager().openCustomPage(ref, store, 
                            new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, shopItem.getOwnerUuid()));
                    } else {
                        // Se a remoção falhou, tenta devolver os itens se foram adicionados
                        if (itemAdded && currentStock > 0) {
                            // Tenta remover os itens que foram adicionados (rollback)
                            InventoryHelper.removeItem(player, itemId, currentStock);
                        }
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED));
                    }
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_remove", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
    }
    
    private String getItemNameText(String itemId) {
        // Obtém o Item usando o ItemManager próprio (sem depender de mods de terceiros)
        Message itemName;
        String itemNameText = itemId;

        com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = ItemManager.getItem(itemId);
        if (itemConfig != null) {
            itemName = Message.translation(itemConfig.getTranslationKey());
            itemNameText = extractTextFromMessage(itemName);
        } else {
            // Fallback: tenta usar o padrão de tradução do Hytale
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
            // Tenta getText()
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
                // Continua tentando
            }
            
            // Tenta getString()
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
                // Continua tentando
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

    public static class ConfirmationGuiData {
        private String action;
        
        public static final BuilderCodec<ConfirmationGuiData> CODEC = BuilderCodec.<ConfirmationGuiData>builder(ConfirmationGuiData.class, ConfirmationGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .build();
    }
}

