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

public class MyShopManagerAddTabGui extends InteractiveCustomUIPage<MyShopManagerAddTabGui.AddTabGuiData> {

    private final PlayerRef playerRef;
    private String currentTabName = "";

    public MyShopManagerAddTabGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, AddTabGuiData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Manager_AddTab.ui");
        
        String titleText = LanguageManager.getTranslation("gui_shop_manager_add_tab_title");
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        uiCommandBuilder.set("#TabNameLabel.Text", LanguageManager.getTranslation("gui_shop_manager_tab_name"));
        
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
                               @Nonnull AddTabGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        if (data.tabNameField != null && !data.tabNameField.isEmpty()) {
            this.currentTabName = data.tabNameField;
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
            String tabName = this.currentTabName;
            
            if (tabName == null || tabName.trim().isEmpty()) {
                player.getPageManager().openCustomPage(ref, store, 
                    new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss));
                return;
            }
            
            final String finalTabName = tabName.trim();
            final UUID finalPlayerUuid = this.playerRef.getUuid();
            
            world.execute(() -> {
                try {
                    if (PlayerShopManager.getInstance().hasTab(finalPlayerUuid, finalTabName)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", finalTabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_already_exists", Color.RED, placeholders));
                        return;
                    }
                    
                    java.util.List<String> tabs = PlayerShopManager.getInstance().getAllTabs(finalPlayerUuid);
                    if (tabs.size() >= 7) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_limit_reached", Color.RED));
                        return;
                    }
                    
                    PlayerShopManager.getInstance().createTab(finalPlayerUuid, finalTabName);
                    
                    // Verifica se a tab foi criada com sucesso
                    if (PlayerShopManager.getInstance().hasTab(finalPlayerUuid, finalTabName)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", finalTabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_created", Color.GREEN, placeholders));
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, finalTabName));
                    } else {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("error", "");
                        player.sendMessage(LanguageManager.getMessage("chat_error_tab_create", Color.RED, errorPlaceholders));
                    }
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_tab_create", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
    }

    public static class AddTabGuiData {
        private String action;
        private String tabName;
        private String tabNameField;
        
        public static final BuilderCodec<AddTabGuiData> CODEC = BuilderCodec.<AddTabGuiData>builder(AddTabGuiData.class, AddTabGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("TabName", Codec.STRING),
                        (data, value, extraInfo) -> data.tabName = value,
                        (data, extraInfo) -> data.tabName != null ? data.tabName : "").add()
                .append(new KeyedCodec<>("@TabNameField", Codec.STRING),
                        (data, value, extraInfo) -> data.tabNameField = value,
                        (data, extraInfo) -> data.tabNameField != null ? data.tabNameField : "").add()
                .build();
    }
}

