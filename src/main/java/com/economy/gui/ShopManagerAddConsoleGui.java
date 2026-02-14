package com.economy.gui;

import com.economy.shop.ShopItem;
import com.economy.shop.ShopManager;
import com.economy.util.CurrencyFormatter;
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

public class ShopManagerAddConsoleGui extends InteractiveCustomUIPage<ShopManagerAddConsoleGui.AddConsoleGuiData> {

    private final PlayerRef playerRef;
    private final String selectedTab;
    private final int shopId; // ID da loja (0 para /shop, 1+ para NPCs)
    private String currentDisplayName = "";
    private String currentConsoleCommand = "";
    private String currentPriceBuy = "0";
    private String currentItemId = ""; // ItemId do ícone (da hotbar)
    private boolean useCash = false; // Se true, usa Cash em vez de Dinheiro

    public ShopManagerAddConsoleGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String selectedTab, int shopId) {
        super(playerRef, lifetime, AddConsoleGuiData.CODEC);
        this.playerRef = playerRef;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        this.shopId = shopId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Manager_AddConsole.ui");
        
        Player player = store.getComponent(ref, Player.getComponentType());
        String defaultItemId = "";
        
        // Tenta pegar o item da mão para preencher como padrão
        if (player != null) {
            var inventory = player.getInventory();
            if (inventory != null) {
                var activeItem = inventory.getActiveHotbarItem();
                if (activeItem != null && !activeItem.isEmpty()) {
                    defaultItemId = activeItem.getItemId();
                }
            }
        }
        
        // Define o título
        String titleText = LanguageManager.getTranslation("gui_shop_manager_add_console_title");
        uiCommandBuilder.set("#TitleLabel.TextSpans", Message.raw(titleText).color(Color.WHITE));
        
        // Define os labels
        uiCommandBuilder.set("#DisplayNameLabel.Text", LanguageManager.getTranslation("gui_shop_manager_console_display_name"));
        uiCommandBuilder.set("#ConsoleCommandLabel.Text", LanguageManager.getTranslation("gui_shop_manager_console_command"));
        uiCommandBuilder.set("#PriceBuyLabel.Text", LanguageManager.getTranslation("gui_shop_manager_price_buy"));
        uiCommandBuilder.set("#PaymentTypeLabel.Text", LanguageManager.getTranslation("gui_shop_manager_payment_type"));
        uiCommandBuilder.set("#HotbarItemsLabel.Text", LanguageManager.getTranslation("gui_shop_manager_console_icon"));
        
        // Configura os botões de seleção (simulando radio buttons)
        String moneyText = LanguageManager.getTranslation("gui_shop_manager_payment_money");
        String cashText = LanguageManager.getTranslation("gui_shop_manager_payment_cash");
        
        // Adiciona indicador visual de seleção no texto usando [X] para o selecionado
        if (!this.useCash) {
            moneyText = "[X] " + moneyText;
            cashText = "[ ] " + cashText;
        } else {
            moneyText = "[ ] " + moneyText;
            cashText = "[X] " + cashText;
        }
        
        uiCommandBuilder.set("#MoneyRadioButton.Text", moneyText);
        uiCommandBuilder.set("#CashRadioButton.Text", cashText);
        
        // Define valores padrão
        if (this.currentItemId == null || this.currentItemId.isEmpty()) {
            this.currentItemId = defaultItemId != null ? defaultItemId : "";
        }
        if (this.currentPriceBuy == null || this.currentPriceBuy.isEmpty()) {
            this.currentPriceBuy = "0";
        }
        
        uiCommandBuilder.set("#DisplayNameField.Value", this.currentDisplayName);
        uiCommandBuilder.set("#ConsoleCommandField.Value", this.currentConsoleCommand);
        uiCommandBuilder.set("#PriceBuyField.Value", this.currentPriceBuy);
        
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
        
        // Captura mudanças nos radio buttons
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MoneyRadioButton", 
            EventData.of("Action", "select_money"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CashRadioButton", 
            EventData.of("Action", "select_cash"));
        
        // Captura mudanças nos campos de texto (sem recriar a GUI)
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DisplayNameField", 
            EventData.of("@DisplayNameField", "#DisplayNameField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ConsoleCommandField", 
            EventData.of("@ConsoleCommandField", "#ConsoleCommandField.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PriceBuyField", 
            EventData.of("@PriceBuyField", "#PriceBuyField.Value"), false);
        
        // Carrega os itens da hotbar
        buildHotbarItems(ref, store, uiCommandBuilder, uiEventBuilder);
    }
    
    private void buildHotbarItems(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                                  @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        var inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        
        // Ocultar todos os botões inicialmente
        for (int i = 0; i < 9; i++) {
            commandBuilder.set("#HotbarButton" + i + ".Visible", false);
            commandBuilder.set("#HotbarButton" + i + " #ItemIcon.Visible", false);
        }
        
        // Obtém a hotbar usando a API correta
        try {
            com.hypixel.hytale.server.core.inventory.container.ItemContainer hotbar = inventory.getHotbar();
            if (hotbar == null) {
                return;
            }
            
            // Itera sobre todos os slots da hotbar (0-8)
            for (int slot = 0; slot < 9; slot++) {
                try {
                    // Obtém o item do slot usando getItemStack(short) - API correta
                    com.hypixel.hytale.server.core.inventory.ItemStack hotbarItem = hotbar.getItemStack((short) slot);
                    
                    // Verifica se há um item válido no slot
                    if (hotbarItem != null && !hotbarItem.isEmpty()) {
                        String itemId = hotbarItem.getItemId();
                        
                        // Valida o itemId (deve existir e não ser "Empty")
                        if (itemId != null && !itemId.isEmpty() && !itemId.equals("Empty")) {
                            // Configura o botão para este item
                            String buttonId = "HotbarButton" + slot;
                            
                            // Define o ItemId do ícone
                            commandBuilder.set("#" + buttonId + " #ItemIcon.ItemId", itemId);
                            commandBuilder.set("#" + buttonId + " #ItemIcon.Visible", true);
                            commandBuilder.set("#" + buttonId + ".Visible", true);
                            
                            // Adiciona evento de clique para atualizar o campo ItemId
                            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, 
                                "#" + buttonId, 
                                EventData.of("Action", "selectHotbar:" + slot + ":" + itemId));
                        }
                    }
                } catch (Exception e) {
                    // Ignora slots vazios ou erros
                }
            }
        } catch (Exception e) {
            // Ignora erros ao acessar hotbar
        }
    }
    
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull AddConsoleGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        // Atualiza os valores das variáveis de instância quando os campos mudam
        if (data.displayNameField != null) {
            this.currentDisplayName = data.displayNameField;
        }
        if (data.consoleCommandField != null) {
            this.currentConsoleCommand = data.consoleCommandField;
        }
        if (data.priceBuyField != null) {
            this.currentPriceBuy = data.priceBuyField;
        }
        
        // Processa seleção de tipo de pagamento
        if (data.action != null && "select_money".equals(data.action)) {
            this.useCash = false;
            // Recria a GUI para atualizar os radio buttons
            World world = store.getExternalData().getWorld();
            if (world != null) {
                world.execute(() -> {
                    try {
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            ShopManagerAddConsoleGui newGui = new ShopManagerAddConsoleGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId);
                            newGui.currentItemId = this.currentItemId;
                            newGui.currentDisplayName = this.currentDisplayName;
                            newGui.currentConsoleCommand = this.currentConsoleCommand;
                            newGui.currentPriceBuy = this.currentPriceBuy;
                            newGui.useCash = false;
                            player.getPageManager().openCustomPage(ref, store, newGui);
                        }
                    } catch (Exception e) {
                        // Ignora erros
                    }
                });
            }
            return;
        }
        
        if (data.action != null && "select_cash".equals(data.action)) {
            this.useCash = true;
            // Recria a GUI para atualizar os radio buttons
            World world = store.getExternalData().getWorld();
            if (world != null) {
                world.execute(() -> {
                    try {
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            ShopManagerAddConsoleGui newGui = new ShopManagerAddConsoleGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId);
                            newGui.currentItemId = this.currentItemId;
                            newGui.currentDisplayName = this.currentDisplayName;
                            newGui.currentConsoleCommand = this.currentConsoleCommand;
                            newGui.currentPriceBuy = this.currentPriceBuy;
                            newGui.useCash = true;
                            player.getPageManager().openCustomPage(ref, store, newGui);
                        }
                    } catch (Exception e) {
                        // Ignora erros
                    }
                });
            }
            return;
        }
        
        // Processa seleção de item da hotbar
        if (data.action != null && data.action.startsWith("selectHotbar:")) {
            String[] parts = data.action.split(":");
            if (parts.length >= 3) {
                try {
                    String itemId = parts[2];
                    this.currentItemId = itemId;
                    
                    // Atualiza os campos na GUI recriando a página
                    World world = store.getExternalData().getWorld();
                    if (world != null) {
                        world.execute(() -> {
                            try {
                                Player player = store.getComponent(ref, Player.getComponentType());
                                if (player != null) {
                                    // Recria a página com os valores atualizados
                                    ShopManagerAddConsoleGui newGui = new ShopManagerAddConsoleGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId);
                                    newGui.currentItemId = itemId;
                                    newGui.currentDisplayName = this.currentDisplayName;
                                    newGui.currentConsoleCommand = this.currentConsoleCommand;
                                    newGui.currentPriceBuy = this.currentPriceBuy;
                                    newGui.useCash = this.useCash;
                                    player.getPageManager().openCustomPage(ref, store, newGui);
                                }
                            } catch (Exception e) {
                                // Ignora erros
                            }
                        });
                    }
                } catch (Exception e) {
                    // Ignora erros
                }
            }
            return;
        }
        
        // Ignora eventos de mudança de valor dos campos de texto (apenas atualiza os valores)
        if (data.action == null || (!"confirm".equals(data.action) && !"cancel".equals(data.action))) {
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
                new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Processa a adição do comando console
            world.execute(() -> {
                try {
                    // Verifica permissão
                    if (!com.economy.util.PermissionHelper.hasPermission(player, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD)) {
                        player.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
                        return;
                    }
                    
                    // Valida a tab
                    if (selectedTab == null || selectedTab.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, new HashMap<>()));
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    if (!ShopManager.getInstance().hasTab(selectedTab, this.shopId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", selectedTab);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, placeholders));
                        player.getPageManager().openCustomPage(ref, store, 
                            new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                        return;
                    }
                    
                    // Valida os campos
                    String displayName = this.currentDisplayName != null ? this.currentDisplayName.trim() : "";
                    String consoleCommand = this.currentConsoleCommand != null ? this.currentConsoleCommand.trim() : "";
                    String itemId = this.currentItemId != null ? this.currentItemId.trim() : "";
                    
                    if (displayName.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_manager_console_display_name_empty", Color.RED));
                        return;
                    }
                    
                    if (consoleCommand.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_manager_console_command_empty", Color.RED));
                        return;
                    }
                    
                    if (itemId.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_manager_console_icon_empty", Color.RED));
                        return;
                    }
                    
                    // Verifica se o item existe (para o ícone)
                    if (!ItemManager.hasItem(itemId)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemId);
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        return;
                    }
                    
                    // Obtém o preço
                    double priceBuy = 0.0;
                    String priceBuyStr = this.currentPriceBuy != null ? this.currentPriceBuy : "0";
                    if (!priceBuyStr.isEmpty()) {
                        try {
                            priceBuy = Double.parseDouble(priceBuyStr);
                            if (priceBuy < 0) {
                                priceBuy = 0.0;
                            }
                        } catch (NumberFormatException e) {
                            priceBuy = 0.0;
                        }
                    }
                    
                    // Adiciona o item à loja primeiro (para obter o UniqueId)
                    ShopItem shopItem = ShopManager.getInstance().addItem(itemId, 1, 0.0, priceBuy, selectedTab, this.shopId);
                    
                    // Configura como comando console
                    shopItem.setConsoleCommand(true);
                    shopItem.setConsoleCommand(consoleCommand);
                    shopItem.setDisplayName(displayName);
                    shopItem.setUseCash(this.useCash);
                    
                    // Atualiza o item no banco de dados com os campos de console
                    ShopManager.getInstance().updateItem(shopItem, this.shopId);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("name", displayName);
                    placeholders.put("command", consoleCommand);
                    placeholders.put("pricebuy", CurrencyFormatter.format(priceBuy));
                    player.sendMessage(LanguageManager.getMessage("chat_shop_console_added", Color.GREEN, placeholders));
                    
                    // Volta para a GUI de gerenciamento
                    player.getPageManager().openCustomPage(ref, store, 
                        new ShopManagerGui(playerRef, CustomPageLifetime.CanDismiss, selectedTab, this.shopId));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_console_add", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }
    }

    public static class AddConsoleGuiData {
        private String action;
        private String displayName;
        private String consoleCommand;
        private String priceBuy;
        private String displayNameField;
        private String consoleCommandField;
        private String priceBuyField;
        
        public static final BuilderCodec<AddConsoleGuiData> CODEC = BuilderCodec.<AddConsoleGuiData>builder(AddConsoleGuiData.class, AddConsoleGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                        (data, value, extraInfo) -> data.displayName = value,
                        (data, extraInfo) -> data.displayName != null ? data.displayName : "").add()
                .append(new KeyedCodec<>("ConsoleCommand", Codec.STRING),
                        (data, value, extraInfo) -> data.consoleCommand = value,
                        (data, extraInfo) -> data.consoleCommand != null ? data.consoleCommand : "").add()
                .append(new KeyedCodec<>("PriceBuy", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuy = value,
                        (data, extraInfo) -> data.priceBuy != null ? data.priceBuy : "").add()
                .append(new KeyedCodec<>("@DisplayNameField", Codec.STRING),
                        (data, value, extraInfo) -> data.displayNameField = value,
                        (data, extraInfo) -> data.displayNameField != null ? data.displayNameField : "").add()
                .append(new KeyedCodec<>("@ConsoleCommandField", Codec.STRING),
                        (data, value, extraInfo) -> data.consoleCommandField = value,
                        (data, extraInfo) -> data.consoleCommandField != null ? data.consoleCommandField : "").add()
                .append(new KeyedCodec<>("@PriceBuyField", Codec.STRING),
                        (data, value, extraInfo) -> data.priceBuyField = value,
                        (data, extraInfo) -> data.priceBuyField != null ? data.priceBuyField : "").add()
                .build();
    }
}

