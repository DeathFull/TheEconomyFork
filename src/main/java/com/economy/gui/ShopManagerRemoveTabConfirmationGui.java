package com.economy.gui;

import com.economy.shop.ShopManager;
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

public class ShopManagerRemoveTabConfirmationGui extends InteractiveCustomUIPage<ShopManagerRemoveTabConfirmationGui.ConfirmationGuiData> {

    private final PlayerRef playerRef;
    private final String tabName;
    private final String previousSelectedTab;
    private final int shopId; // ID da loja (0 para /shop, 1+ para NPCs)

    public ShopManagerRemoveTabConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                              String tabName, String previousSelectedTab) {
        this(playerRef, lifetime, tabName, previousSelectedTab, 0);
    }
    
    public ShopManagerRemoveTabConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                              String tabName, String previousSelectedTab, int shopId) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.tabName = tabName != null ? tabName : "";
        this.previousSelectedTab = previousSelectedTab != null ? previousSelectedTab : "";
        this.shopId = shopId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Remove_Confirmation.ui");
        
        // Cria a mensagem de confirmação
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tab", tabName);
        String confirmationText = LanguageManager.getTranslation("gui_shop_manager_confirm_remove_tab", placeholders);
        
        uiCommandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
        
        // Define os textos dos botões
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
            // Volta para a GUI de gerenciamento
            String tabToShow = previousSelectedTab.equals(tabName) ? "" : previousSelectedTab;
            player.getPageManager().openCustomPage(ref, store, 
                new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, tabToShow, this.shopId));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Remove a tab
            world.execute(() -> {
                try {
                    // Verifica permissão
                    if (!com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD)) {
                        player.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
                        return;
                    }
                    
                    // Verifica se a tab existe
                    if (!ShopManager.getInstance().hasTab(tabName, this.shopId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, placeholders));
                        return;
                    }
                    
                    // Remove a tab
                    if (ShopManager.getInstance().removeTab(tabName, this.shopId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_removed", Color.GREEN, placeholders));
                        
                        // Volta para a GUI de gerenciamento (se a tab removida era a selecionada, volta para a primeira disponível)
                        String tabToShow = previousSelectedTab.equals(tabName) ? "" : previousSelectedTab;
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, tabToShow, this.shopId));
                    } else {
                        player.sendMessage(LanguageManager.getMessage("chat_error_tab_remove", Color.RED));
                    }
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

