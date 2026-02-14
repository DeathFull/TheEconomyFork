package com.economy.gui;

import com.economy.playershop.PlayerShopItem;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyShopManagerRemoveTabConfirmationGui extends InteractiveCustomUIPage<MyShopManagerRemoveTabConfirmationGui.ConfirmationGuiData> {

    private final PlayerRef playerRef;
    private final String tabName;
    private final String previousSelectedTab;
    private final UUID playerUuid;

    public MyShopManagerRemoveTabConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                              String tabName, String previousSelectedTab, UUID playerUuid) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.tabName = tabName != null ? tabName : "";
        this.previousSelectedTab = previousSelectedTab != null ? previousSelectedTab : "";
        this.playerUuid = playerUuid;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Remove_Confirmation.ui");
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tab", tabName);
        String confirmationText = LanguageManager.getTranslation("gui_shop_manager_confirm_remove_tab", placeholders);
        
        uiCommandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
        
        String confirmText = LanguageManager.getTranslation("gui_shop_button_confirm");
        String cancelText = LanguageManager.getTranslation("gui_shop_button_cancel");
        uiCommandBuilder.set("#ConfirmButton.Text", confirmText);
        uiCommandBuilder.set("#CancelButton.Text", cancelText);
        
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
            player.getPageManager().openCustomPage(ref, store, 
                new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, previousSelectedTab));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            world.execute(() -> {
                try {
                    // Verifica se a tab tem itens antes de remover
                    List<PlayerShopItem> tabItems = PlayerShopManager.getInstance().getItemsByTab(playerUuid, tabName);
                    if (tabItems != null && !tabItems.isEmpty()) {
                        // Tab tem itens - não permite remover
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabName);
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_tab_has_items", Color.RED, placeholders));
                        player.getPageManager().openCustomPage(ref, store, 
                            new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, previousSelectedTab));
                        return;
                    }
                    
                    // Tab está vazia - pode remover
                    boolean success = PlayerShopManager.getInstance().removeTab(playerUuid, tabName);
                    
                    if (success) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_removed", Color.GREEN, placeholders));
                    } else {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("error", "");
                        player.sendMessage(LanguageManager.getMessage("chat_error_tab_remove", Color.RED, errorPlaceholders));
                    }
                    
                    player.getPageManager().openCustomPage(ref, store, 
                        new MyShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, previousSelectedTab));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_tab_remove", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
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

