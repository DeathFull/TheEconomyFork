package com.economy.gui;

import com.economy.shop.ShopItem;
import com.economy.util.CurrencyFormatter;
import com.economy.util.InventoryHelper;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
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
import java.util.logging.Level;

public class ShopConfirmationGui extends InteractiveCustomUIPage<ShopConfirmationGui.ConfirmationGuiData> {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
    private final PlayerRef playerRef;
    private final ShopItem shopItem;
    private final String actionType; // "buy" ou "sell"
    private final String selectedTab; // Tab selecionada na loja
    private final int shopId; // ID da loja (0 para /shop, 1+ para NPCs)
    private int quantity; // Agora mutável para permitir edição

    public ShopConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                              ShopItem shopItem, String actionType, int quantity) {
        this(playerRef, lifetime, shopItem, actionType, quantity, "", 0);
    }

    public ShopConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                              ShopItem shopItem, String actionType, int quantity, String selectedTab) {
        this(playerRef, lifetime, shopItem, actionType, quantity, selectedTab, 0);
    }
    
    public ShopConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                              ShopItem shopItem, String actionType, int quantity, String selectedTab, int shopId) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopItem = shopItem;
        this.selectedTab = selectedTab != null ? selectedTab : "";
        this.actionType = actionType;
        this.quantity = quantity;
        this.shopId = shopId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Confirmation.ui");
        
        // Obtém o nome do item traduzido (mesma abordagem do ShopGui)
        String itemNameText;
        
        // Se for comando console, usa displayName
        if (shopItem.isConsoleCommand() && shopItem.getDisplayName() != null && !shopItem.getDisplayName().isEmpty()) {
            itemNameText = shopItem.getDisplayName();
        } else {
            // Para itens normais, obtém o nome do item
            // Obtém o Item usando o ItemManager próprio (sem depender de mods de terceiros)
            Message itemName;
            itemNameText = shopItem.getItemId(); // Fallback
            
            com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(shopItem.getItemId());
            if (itemConfig != null) {
                itemName = Message.translation(itemConfig.getTranslationKey());
                // Extrai o texto do Message usando o mesmo método que o Hytale usa
                itemNameText = extractTextFromMessage(itemName);
            } else {
                // Fallback: tenta usar o padrão de tradução do Hytale
                itemName = Message.translation("item." + shopItem.getItemId());
                itemNameText = extractTextFromMessage(itemName);
            }
            
            // Se ainda não conseguiu um nome válido, usa o itemId formatado
            if (itemNameText == null || itemNameText.isEmpty() || itemNameText.startsWith("item.") || itemNameText.contains("com.hypixel")) {
                itemNameText = formatItemId(shopItem.getItemId());
            }
        }
        
        double totalPrice;
        String messageKey;
        if ("buy".equals(actionType)) {
            totalPrice = shopItem.getPriceBuy() * quantity;
            messageKey = "gui_shop_confirm_buy";
        } else {
            totalPrice = shopItem.getPriceSell() * quantity;
            messageKey = "gui_shop_confirm_sell";
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", itemNameText);
        placeholders.put("quantity", String.valueOf(quantity));
        placeholders.put("price", CurrencyFormatter.format(totalPrice));
        
        String confirmationText = LanguageManager.getTranslation(messageKey, placeholders);
        uiCommandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
        
            // Define o valor inicial do campo de quantidade
            uiCommandBuilder.set("#QuantityField.Value", String.valueOf(quantity));
            
            // Define o texto do label de quantidade (apenas "Quantity:" sem número)
            // O campo de entrada já mostra a quantidade, então o label só precisa do texto
            String quantityLabelText = LanguageManager.getTranslation("gui_shop_quantity_label");
            // Remove qualquer placeholder ou número que possa estar no texto
            quantityLabelText = quantityLabelText.replace("{quantity}", "")
                                                 .replace("{qu...}", "")
                                                 .replaceAll("\\d+", "") // Remove números
                                                 .trim();
            // Garante que termina com ":" se não tiver
            if (!quantityLabelText.endsWith(":")) {
                quantityLabelText = quantityLabelText + ":";
            }
            uiCommandBuilder.set("#QuantityLabel.Text", quantityLabelText);
        
        // Não vamos atualizar em tempo real para evitar problemas
        // O valor será lido quando o usuário clicar em Confirmar
        
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
        
        // Captura mudanças no campo de quantidade usando o padrão do AdminUI
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#QuantityField", 
            EventData.of("@QuantityField", "#QuantityField.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, 
                               @Nonnull ConfirmationGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        
        // Atualiza a quantidade se o campo foi alterado (sem recriar a GUI)
        if (data.quantityField != null && !data.quantityField.isEmpty() && !"confirm".equals(data.action) && !"cancel".equals(data.action)) {
            try {
                int newQuantity = Integer.parseInt(data.quantityField);
                if (newQuantity > 0 && newQuantity != this.quantity) {
                    this.quantity = newQuantity;
                    // Atualiza apenas a mensagem de confirmação sem recriar toda a GUI
                    updateConfirmationMessage(ref, store);
                    return;
                }
            } catch (NumberFormatException e) {
                // Ignora valores inválidos
            }
        }
        
        if (data.action == null) return;
        
        if ("cancel".equals(data.action)) {
            // Fecha a GUI de confirmação e volta para a loja com a tab selecionada
            player.getPageManager().openCustomPage(ref, store, 
                new ShopGui(playerRef, CustomPageLifetime.CanDismiss, this.selectedTab, this.shopId));
            return;
        }
        
        if ("confirm".equals(data.action)) {
            // Lê o valor atual do campo de quantidade antes de processar
            if (data.quantityField != null && !data.quantityField.isEmpty()) {
                try {
                    int newQuantity = Integer.parseInt(data.quantityField);
                    if (newQuantity > 0) {
                        this.quantity = newQuantity;
                    }
                } catch (NumberFormatException e) {
                    // Se o valor for inválido, usa a quantidade padrão
                }
            }
            
            UUID playerUuid = this.playerRef.getUuid();
            World world = store.getExternalData().getWorld();
            
            if ("buy".equals(actionType)) {
                handleBuy(player, playerUuid, world);
            } else if ("sell".equals(actionType)) {
                handleSell(player, playerUuid, world);
            }
            
            // Fecha a GUI de confirmação e volta para a loja com a tab selecionada
            player.getPageManager().openCustomPage(ref, store, 
                new ShopGui(playerRef, CustomPageLifetime.CanDismiss, this.selectedTab, this.shopId));
        }
    }
    
    private void handleBuy(@Nonnull Player player, UUID playerUuid, World world) {
        if (shopItem.getPriceBuy() <= 0) {
            return;
        }
        
        double pricePerUnit = shopItem.getPriceBuy();
        double totalPrice = pricePerUnit * quantity;
        
        // Verifica se o jogador tem dinheiro/cash suficiente ANTES de processar
        boolean useCash = shopItem.isUseCash();
        if (useCash && "buy".equals(actionType)) {
            // Verifica Cash
            int cashPrice = (int)totalPrice;
            if (!com.economy.economy.EconomyManager.getInstance().hasCash(playerUuid, cashPrice)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", String.valueOf(cashPrice));
                player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_cash", Color.RED, placeholders));
                return;
            }
        } else if ("buy".equals(actionType)) {
            // Verifica Dinheiro
            if (!com.economy.economy.EconomyManager.getInstance().hasBalance(playerUuid, totalPrice)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", CurrencyFormatter.format(totalPrice));
                player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_balance", Color.RED, placeholders));
                return;
            }
        } else {
            // Venda sempre usa dinheiro
            if (!com.economy.economy.EconomyManager.getInstance().hasBalance(playerUuid, totalPrice)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", CurrencyFormatter.format(totalPrice));
                player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_balance", Color.RED, placeholders));
                return;
            }
        }
        
        world.execute(() -> {
            // Verifica se é um comando console
            if (shopItem.isConsoleCommand()) {
                // Executa o comando console
                String command = shopItem.getConsoleCommand();
                if (command != null && !command.isEmpty()) {
                    // Substitui placeholders
                    String playerName = "";
                    try {
                        // Usa o PlayerRef que já está disponível na classe (mesmo método usado no HUD)
                        if (this.playerRef != null) {
                            playerName = this.playerRef.getUsername();
                        } else {
                            // Fallback: tenta obter via EconomyManager
                            String nameFromManager = com.economy.economy.EconomyManager.getInstance().getPlayerName(playerUuid);
                            if (nameFromManager != null && !nameFromManager.isEmpty() && !"Desconhecido".equals(nameFromManager)) {
                                playerName = nameFromManager;
                            } else {
                                // Último fallback: tenta obter do player diretamente
                                try {
                                    com.hypixel.hytale.server.core.universe.PlayerRef playerRefFromPlayer = player.getPlayerRef();
                                    if (playerRefFromPlayer != null) {
                                        playerName = playerRefFromPlayer.getUsername();
                                    } else {
                                        playerName = playerUuid.toString();
                                    }
                                } catch (Exception e) {
                                    playerName = playerUuid.toString();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Se falhar, usa UUID como último recurso
                        playerName = playerUuid.toString();
                    }
                    // Suporta múltiplos comandos separados por vírgula
                    String[] commands = command.split(",");
                    String[] processedCommands = new String[commands.length];
                    
                    for (int i = 0; i < commands.length; i++) {
                        String cmd = commands[i].trim();
                        // Substitui placeholders em cada comando
                        cmd = cmd.replace("{playername}", playerName);
                        cmd = cmd.replace("{quanty}", String.valueOf(quantity));
                        processedCommands[i] = cmd;
                    }
                    
                    // Executa todos os comandos no console do servidor
                    if (processedCommands.length == 1) {
                        // Comando único (compatibilidade)
                        com.economy.util.HytaleConsoleCommands.runAsConsole(processedCommands[0]);
                    } else {
                        // Múltiplos comandos
                        com.economy.util.HytaleConsoleCommands.runMany(processedCommands);
                    }
                }
                
                // Remove dinheiro ou cash do comprador
                if (shopItem.isUseCash()) {
                    int cashPrice = (int)totalPrice;
                    com.economy.economy.EconomyManager.getInstance().subtractCash(playerUuid, cashPrice);
                } else {
                    com.economy.economy.EconomyManager.getInstance().subtractBalance(playerUuid, totalPrice);
                }
                
                    Map<String, String> placeholders = new HashMap<>();
                    String displayName = shopItem.getDisplayName();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = getItemNameText(shopItem.getItemId());
                    }
                    placeholders.put("item", displayName);
                    placeholders.put("quantity", String.valueOf(quantity));
                    if (shopItem.isUseCash()) {
                        placeholders.put("price", String.valueOf((int)totalPrice) + " " + LanguageManager.getTranslation("gui_shop_manager_payment_cash"));
                    } else {
                        placeholders.put("price", CurrencyFormatter.format(totalPrice));
                    }
                    player.sendMessage(LanguageManager.getMessage("chat_shop_console_bought", Color.GREEN, placeholders));
            } else {
                // Comportamento normal: adiciona item ao inventário
                int actualQuantityAdded = InventoryHelper.addItemAndGetQuantity(player, shopItem.getItemId(), quantity, -1.0);
                
                if (actualQuantityAdded > 0) {
                    // Calcula o preço baseado na quantidade REALMENTE adicionada
                    double actualPrice = pricePerUnit * actualQuantityAdded;
                    
                    // Remove dinheiro ou cash do comprador (paga apenas pelo que foi adicionado)
                    if (shopItem.isUseCash()) {
                        int cashPrice = (int)actualPrice;
                        com.economy.economy.EconomyManager.getInstance().subtractCash(playerUuid, cashPrice);
                    } else {
                        com.economy.economy.EconomyManager.getInstance().subtractBalance(playerUuid, actualPrice);
                    }
                    
                    Map<String, String> placeholders = new HashMap<>();
                    // Obtém o nome do item da mesma forma que na confirmação
                    String itemNameText = getItemNameText(shopItem.getItemId());
                    placeholders.put("item", itemNameText);
                    placeholders.put("quantity", String.valueOf(actualQuantityAdded));
                    if (shopItem.isUseCash()) {
                        placeholders.put("price", String.valueOf((int)actualPrice) + " " + LanguageManager.getTranslation("gui_shop_manager_payment_cash")); // Cash é inteiro
                    } else {
                        placeholders.put("price", CurrencyFormatter.format(actualPrice));
                    }
                    
                    // Se nem todos os itens foram adicionados, avisa o jogador
                    if (actualQuantityAdded < quantity) {
                        Map<String, String> warningPlaceholders = new HashMap<>();
                        warningPlaceholders.put("added", String.valueOf(actualQuantityAdded));
                        warningPlaceholders.put("requested", String.valueOf(quantity));
                        warningPlaceholders.put("item", itemNameText);
                        if (shopItem.isUseCash()) {
                            warningPlaceholders.put("price", String.valueOf((int)actualPrice) + " " + LanguageManager.getTranslation("gui_shop_manager_payment_cash")); // Cash é inteiro
                        } else {
                            warningPlaceholders.put("price", CurrencyFormatter.format(actualPrice));
                        }
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_bought_partial", Color.YELLOW, warningPlaceholders));
                    } else {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_bought", Color.GREEN, placeholders));
                    }
                } else {
                    // Se falhar ao adicionar o item, não debita o dinheiro (não foi debitado ainda)
                    player.sendMessage(LanguageManager.getMessage("chat_error_inventory_add", Color.RED));
                }
            }
        });
    }
    
    private void handleSell(@Nonnull Player player, UUID playerUuid, World world) {
        if (shopItem.getPriceSell() <= 0) {
            return;
        }
        
        double totalPrice = shopItem.getPriceSell() * quantity;
        
        world.execute(() -> {
            // Verifica se o jogador tem itens suficientes
            int itemCount = InventoryHelper.getItemCount(player, shopItem.getItemId());
            
            if (itemCount < quantity) {
                Map<String, String> placeholders = new HashMap<>();
                String itemNameText = getItemNameText(shopItem.getItemId());
                placeholders.put("item", itemNameText);
                placeholders.put("quantity", String.valueOf(quantity));
                player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_items", Color.RED, placeholders));
                return;
            }
            
            // Remove o item do inventário
            if (InventoryHelper.removeItem(player, shopItem.getItemId(), quantity)) {
                com.economy.economy.EconomyManager.getInstance().addBalance(playerUuid, totalPrice);
                
                Map<String, String> placeholders = new HashMap<>();
                String itemNameText = getItemNameText(shopItem.getItemId());
                placeholders.put("item", itemNameText);
                placeholders.put("quantity", String.valueOf(quantity));
                placeholders.put("price", CurrencyFormatter.format(totalPrice));
                player.sendMessage(LanguageManager.getMessage("chat_shop_item_sold", Color.GREEN, placeholders));
            } else {
                player.sendMessage(LanguageManager.getMessage("chat_error_inventory_remove", Color.RED));
            }
        });
    }
    
    private String extractTextFromMessage(Message message) {
        if (message == null) {
            return null;
        }
        
        try {
            // Tenta vários métodos para obter o texto do Message
            // Método 1: getText()
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
            
            // Método 2: getString()
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
            
            // Método 3: toString() mas verifica se não é apenas a classe
            String toString = message.toString();
            if (toString != null && !toString.isEmpty() && 
                !toString.startsWith("item.") && 
                !toString.contains("com.hypixel") &&
                !toString.startsWith("Message@")) {
                return toString;
            }
            
            // Método 4: Tenta acessar spans internos
            try {
                java.lang.reflect.Method getSpansMethod = message.getClass().getMethod("getSpans");
                Object spansObj = getSpansMethod.invoke(message);
                if (spansObj != null) {
                    if (spansObj instanceof java.util.List) {
                        java.util.List<?> spans = (java.util.List<?>) spansObj;
                        StringBuilder textBuilder = new StringBuilder();
                        for (Object span : spans) {
                            if (span != null) {
                                try {
                                    java.lang.reflect.Method getTextMethod = span.getClass().getMethod("getText");
                                    Object spanText = getTextMethod.invoke(span);
                                    if (spanText != null) {
                                        textBuilder.append(spanText.toString());
                                    }
                                } catch (Exception e) {
                                    // Ignora este span
                                }
                            }
                        }
                        String result = textBuilder.toString();
                        if (result != null && !result.isEmpty() && !result.startsWith("item.") && !result.contains("com.hypixel")) {
                            return result;
                        }
                    }
                }
            } catch (Exception e) {
                // Continua
            }
            
        } catch (Exception e) {
            // Falha ao extrair texto
        }
        
        return null;
    }
    
    private String getItemNameText(String itemId) {
        // Obtém o Item usando o ItemManager próprio (sem depender de mods de terceiros)
        Message itemName;
        String itemNameText = itemId; // Fallback
        
        com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(itemId);
        if (itemConfig != null) {
            itemName = Message.translation(itemConfig.getTranslationKey());
            itemNameText = extractTextFromMessage(itemName);
        } else {
            // Fallback: tenta usar o padrão de tradução do Hytale
            itemName = Message.translation("item." + itemId);
            itemNameText = extractTextFromMessage(itemName);
        }
        
        // Se ainda não conseguiu um nome válido, usa o itemId formatado
        if (itemNameText == null || itemNameText.isEmpty() || itemNameText.startsWith("item.") || itemNameText.contains("com.hypixel")) {
            itemNameText = formatItemId(itemId);
        }
        
        return itemNameText;
    }
    
    private String formatItemId(String itemId) {
        // Formata o itemId para um nome mais legível
        // Ex: "Tool_Hatchet_Adamantite" -> "Adamantite Hatchet"
        if (itemId == null || itemId.isEmpty()) {
            return itemId;
        }
        
        // Remove prefixos comuns
        String formatted = itemId;
        if (formatted.startsWith("Tool_")) {
            formatted = formatted.substring(5);
        }
        if (formatted.startsWith("Item_")) {
            formatted = formatted.substring(5);
        }
        if (formatted.startsWith("Block_")) {
            formatted = formatted.substring(6);
        }
        
        // Substitui underscores por espaços
        formatted = formatted.replace("_", " ");
        
        // Capitaliza cada palavra
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (words[i].length() > 0) {
                result.append(words[i].substring(0, 1).toUpperCase());
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    private void updateConfirmationMessage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String itemNameText;
        
        // Se for comando console, usa displayName
        if (shopItem.isConsoleCommand() && shopItem.getDisplayName() != null && !shopItem.getDisplayName().isEmpty()) {
            itemNameText = shopItem.getDisplayName();
        } else {
            // Para itens normais, obtém o nome do item
            itemNameText = shopItem.getItemId();
            
            // Obtém o Item usando o ItemManager próprio (sem depender de mods de terceiros)
            com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(shopItem.getItemId());
            if (itemConfig != null) {
                Message itemName = Message.translation(itemConfig.getTranslationKey());
                itemNameText = extractTextFromMessage(itemName);
            } else {
                // Fallback: tenta usar o padrão de tradução do Hytale
                Message itemName = Message.translation("item." + shopItem.getItemId());
                itemNameText = extractTextFromMessage(itemName);
            }
            
            // Se ainda não conseguiu um nome válido, usa o itemId formatado
            if (itemNameText == null || itemNameText.isEmpty() || itemNameText.startsWith("item.") || itemNameText.contains("com.hypixel")) {
                itemNameText = formatItemId(shopItem.getItemId());
            }
        }
        
        double totalPrice;
        String messageKey;
        if ("buy".equals(actionType)) {
            totalPrice = shopItem.getPriceBuy() * quantity;
            messageKey = "gui_shop_confirm_buy";
        } else {
            totalPrice = shopItem.getPriceSell() * quantity;
            messageKey = "gui_shop_confirm_sell";
        }
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", itemNameText);
        placeholders.put("quantity", String.valueOf(quantity));
        placeholders.put("price", CurrencyFormatter.format(totalPrice));
        
        String confirmationText = LanguageManager.getTranslation(messageKey, placeholders);
        
        // Atualiza apenas a mensagem usando sendUpdate
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
        this.sendUpdate(commandBuilder, null, false);
    }

    public static class ConfirmationGuiData {
        public static final BuilderCodec<ConfirmationGuiData> CODEC = BuilderCodec.<ConfirmationGuiData>builder(ConfirmationGuiData.class, ConfirmationGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), 
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("@QuantityField", Codec.STRING), 
                        (data, value, extraInfo) -> data.quantityField = value,
                        (data, extraInfo) -> data.quantityField).add()
                .build();

        private String action;
        private String quantityField;
    }
}
