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

public class ShopRemoveConfirmationGui extends InteractiveCustomUIPage<ShopRemoveConfirmationGui.ConfirmationGuiData> {

    private final PlayerRef playerRef;
    private final ShopItem shopItem;
    private final String selectedTab;

    public ShopRemoveConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                    ShopItem shopItem, String selectedTab) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopItem = shopItem;
        this.selectedTab = selectedTab != null ? selectedTab : "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Remove_Confirmation.ui");
        
        // Obtém o nome do item
        String itemNameText = getItemNameText(shopItem.getItemId());
        
        // Cria a mensagem de confirmação usando a mesma tradução do myshop
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
        
        if ("cancel".equals(data.action)) {
            // Fecha a GUI e volta para a loja
            player.getPageManager().openCustomPage(ref, store, 
                new ShopGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Remove o item da loja
            world.execute(() -> {
                try {
                    // Verifica permissão novamente
                    if (!com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_REMOVE)) {
                        player.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
                        return;
                    }
                    
                    // Verifica se o item ainda existe
                    ShopItem item = ShopManager.getInstance().getItem(shopItem.getUniqueId());
                    if (item == null) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED));
                        return;
                    }
                    
                    // Remove o item da loja
                    if (ShopManager.getInstance().removeItem(shopItem.getUniqueId())) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_removed", Color.GREEN, placeholders));
                        
                        // Fecha a GUI e volta para a loja
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab));
                    } else {
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

