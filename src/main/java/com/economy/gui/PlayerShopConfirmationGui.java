package com.economy.gui;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
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

public class PlayerShopConfirmationGui extends InteractiveCustomUIPage<PlayerShopConfirmationGui.ConfirmationGuiData> {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
    private final PlayerRef playerRef;
    private final PlayerShopItem shopItem;
    private final String actionType;
    private int quantity;
    private String selectedTab = "";

    public PlayerShopConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                  PlayerShopItem shopItem, String actionType, int quantity) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopItem = shopItem;
        this.actionType = actionType != null ? actionType : "buy";
        this.quantity = quantity;
    }
    
    public PlayerShopConfirmationGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, 
                                  PlayerShopItem shopItem, String actionType, int quantity, String selectedTab) {
        super(playerRef, lifetime, ConfirmationGuiData.CODEC);
        this.playerRef = playerRef;
        this.shopItem = shopItem;
        this.actionType = actionType != null ? actionType : "buy";
        this.quantity = quantity;
        this.selectedTab = selectedTab != null ? selectedTab : "";
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, 
                     @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/EconomySystem_Shop_Confirmation.ui");
        
        String itemNameText = getItemNameText(shopItem.getItemId());
        
        // Calcula o preço total baseado na ação
        double totalPrice;
        String confirmationText;
        
        if ("sell".equals(actionType)) {
            // Vender: usa preço de venda
            double pricePerUnit = shopItem.getPriceSell();
            totalPrice = pricePerUnit * quantity;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", itemNameText);
            placeholders.put("quantity", String.valueOf(quantity));
            placeholders.put("price", CurrencyFormatter.format(totalPrice));
            confirmationText = LanguageManager.getTranslation("gui_shop_confirm_sell", placeholders);
        } else {
            // Comprar: usa preço de compra
            double pricePerUnit = shopItem.getPriceBuy();
            totalPrice = pricePerUnit * quantity;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", itemNameText);
            placeholders.put("quantity", String.valueOf(quantity));
            placeholders.put("price", CurrencyFormatter.format(totalPrice));
            confirmationText = LanguageManager.getTranslation("gui_playershop_confirm_buy", placeholders);
        }
        
        uiCommandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
        
        uiCommandBuilder.set("#QuantityField.Value", String.valueOf(quantity));
        
        String quantityLabelText = LanguageManager.getTranslation("gui_shop_quantity_label");
        quantityLabelText = quantityLabelText.replace("{quantity}", "").replace("{qu...}", "").trim();
        if (!quantityLabelText.endsWith(":")) {
            quantityLabelText += ":";
        }
        uiCommandBuilder.set("#QuantityLabel.Text", quantityLabelText);
        
        String confirmText = LanguageManager.getTranslation("gui_shop_button_confirm");
        String cancelText = LanguageManager.getTranslation("gui_shop_button_cancel");
        uiCommandBuilder.set("#ConfirmButton.Text", confirmText);
        uiCommandBuilder.set("#CancelButton.Text", cancelText);
        
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton", 
            EventData.of("Action", "confirm"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", 
            EventData.of("Action", "cancel"));
        
        // Captura mudanças no campo de quantidade usando o padrão do ShopConfirmationGui
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#QuantityField", 
            EventData.of("@QuantityField", "#QuantityField.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ConfirmationGuiData data) {
        super.handleDataEvent(ref, store, data);
        
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Atualiza a quantidade se o campo foi alterado (sem recriar a GUI)
        // Igual ao ShopConfirmationGui - só atualiza se não for confirm/cancel
        if (data.quantityField != null && !data.quantityField.isEmpty() && 
            !"confirm".equals(data.action) && !"cancel".equals(data.action)) {
            try {
                int newQuantity = Integer.parseInt(data.quantityField.trim());
                if (newQuantity > 0 && newQuantity != this.quantity) {
                    // Limita a quantidade baseado na ação
                    if ("buy".equals(actionType)) {
                        newQuantity = Math.min(newQuantity, shopItem.getStock());
                    }
                    // Para vender, não limita aqui (será verificado no handleSell)
                    
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
            // Fecha a GUI de confirmação e volta para a loja
            UUID ownerUuid = shopItem.getOwnerUuid();
            if (ownerUuid != null) {
                player.getPageManager().openCustomPage(ref, store, 
                    new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid, this.selectedTab));
            }
            return;
        }

        if ("confirm".equals(data.action)) {
            // Lê o valor atual do campo de quantidade antes de processar
            if (data.quantityField != null && !data.quantityField.isEmpty()) {
                try {
                    int newQuantity = Integer.parseInt(data.quantityField.trim());
                    if (newQuantity > 0) {
                        this.quantity = newQuantity;
                    }
                } catch (NumberFormatException e) {
                    // Se o valor for inválido, usa a quantidade padrão
                }
            }
            
            // Valida a quantidade final
            if (this.quantity <= 0) {
                player.sendMessage(Message.raw(LanguageManager.getTranslation("chat_invalid_amount")).color(Color.RED));
                return;
            }
            
            // Limita a quantidade baseado na ação
            UUID ownerUuid = shopItem.getOwnerUuid();
            
            if ("buy".equals(actionType)) {
                // Limita a quantidade ao estoque disponível para compra
                this.quantity = Math.min(this.quantity, shopItem.getStock());
                if (this.quantity <= 0) {
                    player.sendMessage(LanguageManager.getMessage("chat_playershop_insufficient_stock", Color.RED));
                    // Fecha a confirmação e volta para a loja
                    if (ownerUuid != null) {
                        player.getPageManager().openCustomPage(ref, store, 
                            new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
                    }
                    return;
                }
            }
            // Para vender, não limita aqui (será verificado no handleSell)

            World world = player.getWorld();
            UUID playerUuid = playerRef.getUuid();
            
            if ("buy".equals(actionType)) {
                handleBuy(player, playerUuid, world, ref, store, ownerUuid);
            } else if ("sell".equals(actionType)) {
                handleSell(player, playerUuid, world, ref, store, ownerUuid);
            }
        }
    }

    private void updateConfirmationMessage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            String itemNameText = getItemNameText(shopItem.getItemId());
            
            double totalPrice;
            String confirmationText;
            
            if ("sell".equals(actionType)) {
                // Vender: usa preço de venda
                double pricePerUnit = shopItem.getPriceSell();
                totalPrice = pricePerUnit * quantity;
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("item", itemNameText);
                placeholders.put("quantity", String.valueOf(quantity));
                placeholders.put("price", CurrencyFormatter.format(totalPrice));
                confirmationText = LanguageManager.getTranslation("gui_shop_confirm_sell", placeholders);
            } else {
                // Comprar: usa preço de compra
                double pricePerUnit = shopItem.getPriceBuy();
                totalPrice = pricePerUnit * quantity;
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("item", itemNameText);
                placeholders.put("quantity", String.valueOf(quantity));
                placeholders.put("price", CurrencyFormatter.format(totalPrice));
                confirmationText = LanguageManager.getTranslation("gui_playershop_confirm_buy", placeholders);
            }
            
            // Atualiza apenas a mensagem usando sendUpdate (igual ao ShopConfirmationGui)
            UICommandBuilder commandBuilder = new UICommandBuilder();
            commandBuilder.set("#ConfirmationMessage.TextSpans", Message.raw(confirmationText).color(Color.WHITE));
            // Não atualiza o campo de quantidade aqui para evitar loop infinito
            this.sendUpdate(commandBuilder, null, false);
        } catch (Exception e) {
            // Ignora erros para evitar travamentos
            logger.at(Level.FINE).log("Error updating confirmation message: " + e.getMessage());
        }
    }
    
    /**
     * Obtém um PlayerRef online pelo UUID usando o mesmo método do chat-plus.
     * 
     * @param uuid O UUID do jogador
     * @return O PlayerRef se estiver online, null caso contrário
     */
    private com.hypixel.hytale.server.core.universe.PlayerRef getOnlinePlayerRefByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        try {
            // Usa o mesmo método do chat-plus: Universe.get() -> getWorlds() -> getPlayers() -> getPlayerRef()
            Class<?> universeClass = Class.forName("com.hypixel.hytale.server.core.universe.Universe");
            java.lang.reflect.Method getMethod = universeClass.getMethod("get");
            Object universe = getMethod.invoke(null);
            
            if (universe != null) {
                java.lang.reflect.Method getWorldsMethod = universe.getClass().getMethod("getWorlds");
                Object worlds = getWorldsMethod.invoke(universe);
                
                if (worlds != null) {
                    java.util.Collection<?> worldsCollection = null;
                    if (worlds instanceof java.util.Collection) {
                        worldsCollection = (java.util.Collection<?>) worlds;
                    } else if (worlds instanceof java.util.Map) {
                        worldsCollection = new java.util.ArrayList<>(((java.util.Map<?, ?>) worlds).values());
                    }
                    
                    if (worldsCollection != null) {
                        for (Object worldObj : worldsCollection) {
                            if (worldObj != null) {
                                try {
                                    java.lang.reflect.Method getPlayersMethod = worldObj.getClass().getMethod("getPlayers");
                                    Object playersResult = getPlayersMethod.invoke(worldObj);
                                    
                                    if (playersResult instanceof java.util.Collection) {
                                        java.util.Collection<Player> worldPlayers = (java.util.Collection<Player>) playersResult;
                                        for (Player player : worldPlayers) {
                                            if (player != null) {
                                                // Obtém o PlayerRef do Player
                                                com.hypixel.hytale.server.core.universe.PlayerRef playerRef = getPlayerRefFromPlayer(player);
                                                if (playerRef != null && uuid.equals(playerRef.getUuid())) {
                                                    return playerRef;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Continua procurando em outros mundos
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erros - se não conseguir obter, retorna null
            logger.at(Level.FINE).log("Error getting online PlayerRef by UUID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtém PlayerRef a partir de um Player (mesmo método do chat-plus).
     */
    private com.hypixel.hytale.server.core.universe.PlayerRef getPlayerRefFromPlayer(Player player) {
        try {
            java.lang.reflect.Method getPlayerRefMethod = player.getClass().getMethod("getPlayerRef");
            Object playerRef = getPlayerRefMethod.invoke(player);
            if (playerRef instanceof com.hypixel.hytale.server.core.universe.PlayerRef) {
                return (com.hypixel.hytale.server.core.universe.PlayerRef) playerRef;
            }
        } catch (Exception e) {
            // Ignora erros
        }
        return null;
    }

    private String getItemNameText(String itemId) {
        // Obtém o Item usando o ItemManager próprio (sem depender de mods de terceiros)
        Message itemName;
        String itemNameText = itemId;

        com.hypixel.hytale.server.core.asset.type.item.config.Item itemConfig = com.economy.util.ItemManager.getItem(itemId);
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

    private String formatItemId(String itemId) {
        String[] parts = itemId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                formatted.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return formatted.toString().trim();
    }

    private void handleBuy(@Nonnull Player player, UUID playerUuid, World world, 
                         @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, UUID ownerUuid) {
        if (shopItem.getPriceBuy() <= 0) {
            return;
        }

        // Verifica se o jogador está tentando comprar de si mesmo
        if (ownerUuid != null && playerUuid.equals(ownerUuid)) {
            player.sendMessage(LanguageManager.getMessage("chat_playershop_cannot_buy_from_self", Color.RED));
            // Fecha a confirmação e volta para a loja
            player.getPageManager().openCustomPage(ref, store, 
                new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
            return;
        }

        // Verifica se ainda há estoque disponível
        PlayerShopItem currentItem = PlayerShopManager.getInstance().getItem(shopItem.getUniqueId());
        if (currentItem == null || currentItem.getStock() < quantity) {
            player.sendMessage(LanguageManager.getMessage("chat_playershop_insufficient_stock", Color.RED));
            // Fecha a confirmação e volta para a loja
            if (ownerUuid != null) {
                player.getPageManager().openCustomPage(ref, store, 
                    new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
            }
            return;
        }

        // O preço é por unidade, então multiplica pela quantidade
        double pricePerUnit = shopItem.getPriceBuy();
        double totalPrice = pricePerUnit * quantity;

        // CRÍTICO: Verifica se o jogador tem dinheiro suficiente ANTES de processar
        if (!EconomyManager.getInstance().hasBalance(playerUuid, totalPrice)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", CurrencyFormatter.format(totalPrice));
            player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_balance", Color.RED, placeholders));
            // Fecha a confirmação e volta para a loja
            if (ownerUuid != null) {
                player.getPageManager().openCustomPage(ref, store, 
                    new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
            }
            return;
        }
        
        world.execute(() -> {
            // Verifica novamente o estoque dentro do execute (pode ter mudado)
            PlayerShopItem itemCheck = PlayerShopManager.getInstance().getItem(shopItem.getUniqueId());
            if (itemCheck == null || itemCheck.getStock() < quantity) {
                player.sendMessage(LanguageManager.getMessage("chat_playershop_insufficient_stock", Color.RED));
                if (ownerUuid != null) {
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
                }
                return;
            }
            
            // Adiciona item ao inventário do comprador com a durabilidade e durabilidade máxima original do item
            // Usa os valores do item atualizado (itemCheck) para garantir que está usando os valores mais recentes
            double itemDurability = itemCheck.getDurability();
            double itemMaxDurability = itemCheck.getMaxDurability();
            int actualQuantityAdded = InventoryHelper.addItemAndGetQuantityWithMaxDurability(
                player, itemCheck.getItemId(), quantity, itemDurability, itemMaxDurability);
            
            if (actualQuantityAdded > 0) {
                // Calcula o preço baseado na quantidade REALMENTE adicionada
                double actualPrice = pricePerUnit * actualQuantityAdded;
                
                // Remove dinheiro do comprador (paga apenas pelo que foi adicionado)
                EconomyManager.getInstance().subtractBalance(playerUuid, actualPrice);
                
                // Calcula a taxa e o valor que o dono recebe (após dedução da taxa)
                double playerTax = Main.CONFIG.get().getPlayerTax();
                double taxAmount = actualPrice * (playerTax / 100.0);
                double ownerReceives = actualPrice - taxAmount;
                
                // Adiciona dinheiro ao dono da loja (recebe o preço menos a taxa)
                if (ownerUuid != null) {
                    EconomyManager.getInstance().addBalance(ownerUuid, ownerReceives);
                }
                
                // Reduz o estoque apenas pela quantidade realmente adicionada
                PlayerShopManager.getInstance().decreaseStock(shopItem.getUniqueId(), actualQuantityAdded);
                
                Map<String, String> placeholders = new HashMap<>();
                String itemNameText = getItemNameText(shopItem.getItemId());
                placeholders.put("item", itemNameText);
                placeholders.put("quantity", String.valueOf(actualQuantityAdded));
                placeholders.put("price", CurrencyFormatter.format(actualPrice));
                
                // Se nem todos os itens foram adicionados, avisa o jogador
                if (actualQuantityAdded < quantity) {
                    Map<String, String> warningPlaceholders = new HashMap<>();
                    warningPlaceholders.put("added", String.valueOf(actualQuantityAdded));
                    warningPlaceholders.put("requested", String.valueOf(quantity));
                    warningPlaceholders.put("item", itemNameText);
                    warningPlaceholders.put("price", CurrencyFormatter.format(actualPrice));
                    player.sendMessage(LanguageManager.getMessage("chat_playershop_item_bought_partial", Color.YELLOW, warningPlaceholders));
                } else {
                    player.sendMessage(LanguageManager.getMessage("chat_playershop_item_bought", Color.GREEN, placeholders));
                }
                
                // Envia mensagem para o dono da loja se estiver online (usando PlayerRef como no chat-plus)
                if (ownerUuid != null) {
                    com.hypixel.hytale.server.core.universe.PlayerRef ownerPlayerRef = getOnlinePlayerRefByUuid(ownerUuid);
                    if (ownerPlayerRef != null) {
                        Map<String, String> ownerPlaceholders = new HashMap<>();
                        ownerPlaceholders.put("quantity", String.valueOf(actualQuantityAdded));
                        ownerPlaceholders.put("item", itemNameText);
                        ownerPlaceholders.put("player", player.getDisplayName());
                        ownerPlaceholders.put("money", CurrencyFormatter.format(ownerReceives));
                        com.hypixel.hytale.server.core.Message ownerMessage = LanguageManager.getMessage("chat_playershop_owner_sold", Color.GREEN, ownerPlaceholders);
                        ownerPlayerRef.sendMessage(ownerMessage);
                    }
                }
                
                // Fecha a GUI de confirmação e volta para a loja com estoque atualizado
                if (ownerUuid != null) {
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
                }
            } else {
                // Se falhar ao adicionar o item, não debita o dinheiro (não foi debitado ainda)
                Map<String, String> errorPlaceholders = new HashMap<>();
                errorPlaceholders.put("error", "");
                player.sendMessage(LanguageManager.getMessage("chat_error_inventory_add", Color.RED, errorPlaceholders));
            }
        });
    }

    private void handleSell(@Nonnull Player player, UUID playerUuid, World world, 
                           @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, UUID ownerUuid) {
        if (shopItem.getPriceSell() <= 0) {
            return;
        }
        
        // O preço de venda é por unidade
        double pricePerUnit = shopItem.getPriceSell();
        double totalPrice = pricePerUnit * quantity;
        
        world.execute(() -> {
            // Verifica se o jogador tem itens suficientes no inventário
            int itemCount = InventoryHelper.getItemCount(player, shopItem.getItemId());
            
            if (itemCount < quantity) {
                Map<String, String> placeholders = new HashMap<>();
                String itemNameText = getItemNameText(shopItem.getItemId());
                placeholders.put("item", itemNameText);
                placeholders.put("quantity", String.valueOf(quantity));
                player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_items", Color.RED, placeholders));
                // Fecha a confirmação e volta para a loja
                if (ownerUuid != null) {
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
                }
                return;
            }
            
            // CRÍTICO: Verifica se o dono tem saldo suficiente ANTES de processar a transação
            if (ownerUuid != null) {
                if (!EconomyManager.getInstance().hasBalance(ownerUuid, totalPrice)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("amount", CurrencyFormatter.format(totalPrice));
                    player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_balance", Color.RED, placeholders));
                    // Fecha a confirmação e volta para a loja
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
                    return;
                }
            }
            
            // Remove o item do inventário do jogador
            if (!InventoryHelper.removeItem(player, shopItem.getItemId(), quantity)) {
                Map<String, String> errorPlaceholders = new HashMap<>();
                errorPlaceholders.put("error", "");
                player.sendMessage(LanguageManager.getMessage("chat_error_inventory_remove", Color.RED, errorPlaceholders));
                return;
            }
            
            // CRÍTICO: Verifica novamente o saldo do dono antes de adicionar ao estoque
            // (pode ter mudado entre a primeira verificação e agora)
            if (ownerUuid != null) {
                if (!EconomyManager.getInstance().hasBalance(ownerUuid, totalPrice)) {
                    // Se o dono não tem mais dinheiro, devolve o item ao jogador
                    InventoryHelper.addItem(player, shopItem.getItemId(), quantity);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("amount", CurrencyFormatter.format(totalPrice));
                    player.sendMessage(LanguageManager.getMessage("chat_shop_insufficient_balance", Color.RED, placeholders));
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
                    return;
                }
            }
            
            // Adiciona o item ao estoque da loja (ou aumenta se já existir)
            boolean itemAddedToShop = false;
            if (ownerUuid != null) {
                PlayerShopManager shopManager = PlayerShopManager.getInstance();
                
                // Obtém a durabilidade do item (se houver) - por enquanto usa 0.0
                double durability = shopItem.getDurability();
                
                // Obtém a tab do item
                String itemTab = shopItem.getTab() != null ? shopItem.getTab() : "";
                
                // Adiciona ou atualiza o item na loja (aumenta o estoque)
                try {
                    shopManager.addOrUpdateItem(
                        shopItem.getItemId(), 
                        shopItem.getQuantity(), 
                        shopItem.getPriceBuy(), 
                        shopItem.getPriceSell(), 
                        ownerUuid, 
                        durability, 
                        quantity,  // quantidade a adicionar ao estoque
                        itemTab    // IMPORTANTE: passar a tab para encontrar o item correto
                    );
                    itemAddedToShop = true;
                } catch (Exception e) {
                    // Se falhar ao adicionar ao estoque, devolve o item ao jogador
                    logger.at(Level.WARNING).log("Failed to add item to shop stock: " + e.getMessage());
                    InventoryHelper.addItem(player, shopItem.getItemId(), quantity);
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_add", Color.RED));
                    return;
                }
            }
            
            // Calcula a taxa e o valor que o vendedor recebe (após dedução da taxa)
            double playerTax = Main.CONFIG.get().getPlayerTax();
            double taxAmount = totalPrice * (playerTax / 100.0);
            double sellerReceives = totalPrice - taxAmount;
            
            // Só processa o pagamento se o item foi adicionado ao estoque com sucesso
            if (itemAddedToShop || ownerUuid == null) {
                // Paga o preço de venda ao jogador que está vendendo (menos a taxa)
                EconomyManager.getInstance().addBalance(playerUuid, sellerReceives);
                
                // Debita o preço de venda completo do dono da loja
                if (ownerUuid != null) {
                    // Debita o dinheiro do dono (já verificamos que tem saldo suficiente acima)
                    EconomyManager.getInstance().subtractBalance(ownerUuid, totalPrice);
                }
            } else {
                // Se não conseguiu adicionar ao estoque, devolve o item
                InventoryHelper.addItem(player, shopItem.getItemId(), quantity);
                player.sendMessage(LanguageManager.getMessage("chat_error_item_add", Color.RED));
                return;
            }
            
            Map<String, String> placeholders = new HashMap<>();
            String itemNameText = getItemNameText(shopItem.getItemId());
            placeholders.put("item", itemNameText);
            placeholders.put("quantity", String.valueOf(quantity));
            placeholders.put("price", CurrencyFormatter.format(sellerReceives));
            player.sendMessage(LanguageManager.getMessage("chat_shop_item_sold", Color.GREEN, placeholders));
            
            // Envia mensagem para o dono da loja se estiver online (usando PlayerRef como no chat-plus)
            if (ownerUuid != null) {
                com.hypixel.hytale.server.core.universe.PlayerRef ownerPlayerRef = getOnlinePlayerRefByUuid(ownerUuid);
                if (ownerPlayerRef != null) {
                    Map<String, String> ownerPlaceholders = new HashMap<>();
                    ownerPlaceholders.put("quantity", String.valueOf(quantity));
                    ownerPlaceholders.put("item", itemNameText);
                    ownerPlaceholders.put("player", player.getDisplayName());
                    ownerPlaceholders.put("money", CurrencyFormatter.format(totalPrice));
                    com.hypixel.hytale.server.core.Message ownerMessage = LanguageManager.getMessage("chat_playershop_owner_bought", Color.GREEN, ownerPlaceholders);
                    ownerPlayerRef.sendMessage(ownerMessage);
                }
            }
            
            // Fecha a GUI de confirmação e volta para a loja com estoque atualizado
            if (ownerUuid != null) {
                player.getPageManager().openCustomPage(ref, store, 
                    new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, ownerUuid));
            }
        });
    }

    private String extractTextFromMessage(Message message) {
        if (message == null) {
            return null;
        }
        
        try {
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
            
            String toString = message.toString();
            if (toString != null && !toString.isEmpty() && 
                !toString.startsWith("item.") && 
                !toString.contains("com.hypixel") &&
                !toString.startsWith("Message@")) {
                return toString;
            }
            
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error extracting text from Message: " + e.getMessage());
        }
        
        return null;
    }

    public static class ConfirmationGuiData {
        private String action;
        private String quantityField;

        public static final BuilderCodec<ConfirmationGuiData> CODEC = BuilderCodec.<ConfirmationGuiData>builder(ConfirmationGuiData.class, ConfirmationGuiData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, extraInfo) -> data.action = value,
                        (data, extraInfo) -> data.action).add()
                .append(new KeyedCodec<>("@QuantityField", Codec.STRING),
                        (data, value, extraInfo) -> data.quantityField = value,
                        (data, extraInfo) -> data.quantityField).add()
                .build();
    }
}

