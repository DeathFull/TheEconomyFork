package com.economy.commands.subcommand.myshop;

import com.economy.Main;
import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.InventoryHelper;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MyShopAddCommand extends AbstractAsyncCommand {

    private RequiredArg<String> tabArg;
    private RequiredArg<Double> priceBuyArg;
    private RequiredArg<Double> priceSellArg;

    public MyShopAddCommand() {
        super("add", com.economy.util.LanguageManager.getTranslation("desc_myshop_add"));
        // O comando é: /myshop add <tab> <preço_compra> <preço_venda>
        // tab = nome da tab (obrigatório, vem primeiro)
        // priceBuy = preço de compra (o que o comprador paga)
        // priceSell = preço de venda (o que o jogador recebe quando alguém compra)
        this.tabArg = this.withRequiredArg("tab", "Nome da tab", ArgTypes.STRING);
        this.priceBuyArg = this.withRequiredArg("priceBuy", "Preço de compra", ArgTypes.DOUBLE);
        this.priceSellArg = this.withRequiredArg("priceSell", "Preço de venda", ArgTypes.DOUBLE);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MYSHOP);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MYSHOP)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        if (!Main.CONFIG.get().isEnablePlayerShop()) {
            player.sendMessage(LanguageManager.getMessage("chat_playershop_disabled", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        String tabName = commandContext.get(tabArg);
        Double priceBuy = commandContext.get(priceBuyArg);
        Double priceSell = commandContext.get(priceSellArg);
        
        if (tabName == null || tabName.isEmpty()) {
            player.sendMessage(LanguageManager.getMessage("chat_myshop_usage_add", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        if (priceBuy == null || priceBuy < 0) {
            player.sendMessage(LanguageManager.getMessage("chat_myshop_usage_add", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        if (priceSell == null || priceSell < 0) {
            player.sendMessage(LanguageManager.getMessage("chat_myshop_usage_add", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        final double finalPriceSell = priceSell;
        final double finalPriceBuy = priceBuy;
        final String finalTabName = tabName;

        return CompletableFuture.runAsync(() -> {
            world.execute(() -> {
                try {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) {
                        return;
                    }

                    UUID playerUuid = playerRef.getUuid();
                    
                    // Valida a tab (obrigatória)
                    if (!PlayerShopManager.getInstance().hasTab(playerUuid, finalTabName)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", finalTabName);
                        player.sendMessage(LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, placeholders));
                        return;
                    }
                    PlayerShopManager shopManager = PlayerShopManager.getInstance();

                    // Obtém o item na mão
                    var inventory = player.getInventory();
                    if (inventory == null) {
                        player.sendMessage(LanguageManager.getMessage("chat_iteminfo_error_inventory", Color.RED));
                        return;
                    }

                    var activeItem = inventory.getActiveHotbarItem();
                    if (activeItem == null || activeItem.isEmpty()) {
                        player.sendMessage(LanguageManager.getMessage("chat_iteminfo_no_item", Color.YELLOW));
                        return;
                    }

                    String itemId = activeItem.getItemId();
                    int quantity = activeItem.getQuantity();
                    
                    // Verifica se o item existe
                    if (itemId == null || itemId.isEmpty() || !com.economy.util.ItemManager.hasItem(itemId)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemId != null ? itemId : "null");
                        player.sendMessage(LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        return;
                    }
                    
                    // Obtém a durabilidade do item (se houver)
                    double durability = 0.0;
                    try {
                        java.lang.reflect.Method getDurabilityMethod = activeItem.getClass().getMethod("getDurability");
                        Object durabilityObj = getDurabilityMethod.invoke(activeItem);
                        if (durabilityObj != null) {
                            if (durabilityObj instanceof Number) {
                                durability = ((Number) durabilityObj).doubleValue();
                            } else {
                                durability = Double.parseDouble(durabilityObj.toString());
                            }
                        }
                    } catch (Exception e) {
                        // Durabilidade não disponível ou método não existe, usa 0.0
                    }

                    // Remove o item usando o ItemStack original (importante para itens com durabilidade alterada)
                    // Isso garante que todas as propriedades do item (incluindo durabilidade) sejam consideradas
                    try {
                        var combined = inventory.getCombinedHotbarFirst();
                        var transaction = combined.removeItemStack(activeItem);
                        if (!transaction.succeeded()) {
                            player.sendMessage(LanguageManager.getMessage("chat_myshop_error_remove_item", Color.RED));
                            return;
                        }
                    } catch (Exception e) {
                        // Se der erro, tenta o método antigo como fallback
                        if (!InventoryHelper.removeItem(player, itemId, quantity)) {
                            player.sendMessage(LanguageManager.getMessage("chat_myshop_error_remove_item", Color.RED));
                            return;
                        }
                    }

                    // Atualiza informações do jogador (UUID e nick)
                    shopManager.updatePlayerInfo(playerUuid, player.getDisplayName());
                    
                    // Adiciona o item à loja do jogador (ou atualiza se já existir)
                    // priceSell = preço de venda (o que o jogador recebe quando alguém compra)
                    // priceBuy = preço de compra (o que o comprador paga)
                    PlayerShopItem shopItem = shopManager.addOrUpdateItem(itemId, quantity, finalPriceBuy, finalPriceSell, playerUuid, durability, quantity, finalTabName);
                    
                    // Obtém o nome do item traduzido
                    String itemName = com.economy.util.ItemManager.getItemName(itemId);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("item", itemName);
                    placeholders.put("quantity", String.valueOf(quantity));
                    placeholders.put("price", CurrencyFormatter.format(finalPriceBuy));
                    placeholders.put("uniqueid", String.valueOf(shopItem.getUniqueId()));
                    player.sendMessage(LanguageManager.getMessage("chat_myshop_item_added", Color.GREEN, placeholders));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_add", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }, world);
    }
}

