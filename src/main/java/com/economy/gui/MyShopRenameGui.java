package com.economy.gui;

import com.economy.playershop.PlayerShopManager;
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

public class MyShopRenameGui extends InteractiveCustomUIPage<MyShopRenameGui.RenameGuiData> {

    private final PlayerRef playerRef;
    private String currentShopName = "";

    public MyShopRenameGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, RenameGuiData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Manager_AddTab.ui");
        
        // Usa o mesmo UI do AddTab, mas muda os textos
        String titleText = LanguageManager.getTranslation("gui_myshop_rename_shop_title");
        if (titleText == null || titleText.isEmpty() || titleText.equals("myshop_rename_shop_title")) {
            titleText = LanguageManager.getTranslation("gui_myshop_rename_shop");
        }
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        String labelText = LanguageManager.getTranslation("gui_myshop_rename_shop_label");
        if (labelText == null || labelText.isEmpty() || labelText.equals("myshop_rename_shop_label")) {
            labelText = LanguageManager.getTranslation("gui_shop_manager_tab_name");
        }
        uiCommandBuilder.set("#TabNameLabel.Text", labelText);
        
        // Obtém o nome atual da loja
        UUID playerUuid = this.playerRef.getUuid();
        String currentName = PlayerShopManager.getInstance().getShopCustomName(playerUuid);
        if (currentName == null || currentName.isEmpty()) {
            currentName = "";
        }
        this.currentShopName = currentName;
        
        uiCommandBuilder.set("#TabNameField.Value", currentName);
        
        String confirmText = LanguageManager.getTranslation("gui_shop_button_confirm");
        String cancelText = LanguageManager.getTranslation("gui_shop_button_cancel");
        uiCommandBuilder.set("#ConfirmButton.Text", confirmText);
        uiCommandBuilder.set("#CancelButton.Text", cancelText);
        
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton", 
            EventData.of("Action", "confirm"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", 
            EventData.of("Action", "cancel"));
        
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TabNameField", 
            EventData.of("@TabNameField", "#TabNameField.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull RenameGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.shopNameField != null && !data.shopNameField.isEmpty()) {
            this.currentShopName = data.shopNameField;
        }
        
        if (data.action == null) {
            return;
        }
        
        if (!"confirm".equals(data.action) && !"cancel".equals(data.action)) {
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
                new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            String shopName = this.currentShopName;
            
            if (shopName == null || shopName.trim().isEmpty()) {
                // Se vazio, remove o nome personalizado
                shopName = "";
            } else {
                shopName = shopName.trim();
                // Remove aspas se existirem
                while (shopName.startsWith("\"") && shopName.endsWith("\"") && shopName.length() > 1) {
                    shopName = shopName.substring(1, shopName.length() - 1).trim();
                }
                // Limita o tamanho
                if (shopName.length() > 50) {
                    shopName = shopName.substring(0, 50);
                }
            }
            
            final String finalShopName = shopName;
            
            world.execute(() -> {
                try {
                    UUID playerUuid = this.playerRef.getUuid();
                    boolean success = PlayerShopManager.getInstance().renameShop(playerUuid, finalShopName);
                    
                    if (success) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("name", finalShopName.isEmpty() ? "Loja padrão" : finalShopName);
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_renamed", Color.GREEN, placeholders));
                    } else {
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_rename_error", Color.RED));
                    }
                    
                    player.getPageManager().openCustomPage(ref, store, 
                        new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss));
                } catch (Exception e) {
                    player.sendMessage(LanguageManager.getMessage("chat_myshop_rename_error", Color.RED));
                    e.printStackTrace();
                }
            });
        }
    }

    public static class RenameGuiData {
        private String action;
        private String shopName;
        private String shopNameField;
        
        public static final BuilderCodec<RenameGuiData> CODEC = BuilderCodec.<RenameGuiData>builder(RenameGuiData.class, RenameGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("ShopName", Codec.STRING),
                        (data, value, extraInfo) -> data.shopName = value,
                        (data, extraInfo) -> data.shopName != null ? data.shopName : "").add()
                .append(new KeyedCodec<>("@TabNameField", Codec.STRING),
                        (data, value, extraInfo) -> data.shopNameField = value,
                        (data, extraInfo) -> data.shopNameField != null ? data.shopNameField : "").add()
                .build();
    }
}

