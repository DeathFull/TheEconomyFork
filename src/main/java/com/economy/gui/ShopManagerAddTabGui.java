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

public class ShopManagerAddTabGui extends InteractiveCustomUIPage<ShopManagerAddTabGui.AddTabGuiData> {

    private final PlayerRef playerRef;
    private final int shopId; // ID da loja (0 para /shop, 1+ para NPCs)
    private String currentTabName = ""; // Armazena o valor atual do campo

    public ShopManagerAddTabGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        this(playerRef, lifetime, 0);
    }
    
    public ShopManagerAddTabGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, int shopId) {
        super(playerRef, lifetime, AddTabGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopId = shopId;
        com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem")
            .at(java.util.logging.Level.INFO).log("ShopManagerAddTabGui created with shopId: %d", this.shopId);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Manager_AddTab.ui");
        
        // Define o título
        String titleText = LanguageManager.getTranslation("gui_shop_manager_add_tab_title");
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define o label
        uiCommandBuilder.set("#TabNameLabel.Text", LanguageManager.getTranslation("gui_shop_manager_tab_name"));
        
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
        
        // Captura mudanças no campo de texto (sem recriar a GUI)
        // O valor é capturado automaticamente quando o campo muda
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TabNameField", 
            EventData.of("@TabNameField", "#TabNameField.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull AddTabGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        // Atualiza o valor armazenado quando o campo muda
        if (data.tabNameField != null && !data.tabNameField.isEmpty()) {
            this.currentTabName = data.tabNameField;
        }
        
        // Se for apenas uma mudança de valor do campo (sem ação), apenas atualiza e retorna
        if (data.action == null) {
            return;
        }
        
        // Processa apenas ações de confirm ou cancel
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
            // Volta para a GUI de gerenciamento
            player.getPageManager().openCustomPage(ref, store, 
                new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, "", this.shopId));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Processa a criação da tab
            world.execute(() -> {
                com.hypixel.hytale.logger.HytaleLogger logger = com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem");
                try {
                    // Verifica permissão
                    if (!com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD)) {
                        player.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
                        return;
                    }
                    
                    // Obtém o nome da tab (prioriza o valor armazenado, depois tenta do data)
                    String tabName = null;
                    if (!this.currentTabName.isEmpty()) {
                        tabName = this.currentTabName;
                    } else if (data.tabNameField != null && !data.tabNameField.isEmpty()) {
                        tabName = data.tabNameField;
                    } else if (data.tabName != null && !data.tabName.isEmpty()) {
                        tabName = data.tabName;
                    }
                    
                    // Se estiver vazio, apenas fecha a janela
                    if (tabName == null || tabName.trim().isEmpty()) {
                        // Fecha a GUI e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss));
                        return;
                    }
                    
                    tabName = tabName.trim();
                    
                    // Verifica se a tab já existe
                    if (ShopManager.getInstance().hasTab(tabName, this.shopId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_already_exists", Color.RED, placeholders));
                        // Fecha a GUI e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, "", this.shopId));
                        return;
                    }
                    
                    // Verifica o limite de tabs
                    if (ShopManager.getInstance().getAllTabs(this.shopId).size() >= 7) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_limit_reached", Color.RED));
                        // Fecha a GUI e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, "", this.shopId));
                        return;
                    }
                    
                    // Cria a tab
                    try {
                        ShopManager.getInstance().createTab(tabName, this.shopId);
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_created", Color.GREEN, placeholders));
                        
                        // Atualiza a GUI de gerenciamento com a nova tab selecionada
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, tabName, this.shopId));
                    } catch (IllegalStateException e) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_limit_reached", Color.RED));
                        // Fecha a GUI e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, "", this.shopId));
                    } catch (Exception e) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                        player.sendMessage(LanguageManager.getMessage("chat_error_tab_create", Color.RED, errorPlaceholders));
                        // Fecha a GUI e volta para a GUI de gerenciamento
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, "", this.shopId));
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

