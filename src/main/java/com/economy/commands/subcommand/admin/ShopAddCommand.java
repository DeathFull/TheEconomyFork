package com.economy.commands.subcommand.admin;

import com.economy.shop.ShopItem;
import com.economy.shop.ShopManager;
import com.economy.util.CurrencyFormatter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
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
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class ShopAddCommand extends AbstractAsyncCommand {

    private RequiredArg<String> tab;
    private RequiredArg<String> itemId;
    private RequiredArg<Double> quantity;
    private RequiredArg<Double> priceSell;
    private RequiredArg<Double> priceBuy;

    public ShopAddCommand() {
        super("add", com.economy.util.LanguageManager.getTranslation("desc_shop_add"));
        this.setPermissionGroup(GameMode.Creative);
        this.tab = this.withRequiredArg("tab", "Nome da tab", ArgTypes.STRING);
        this.itemId = this.withRequiredArg("itemid", "ID do item", ArgTypes.STRING);
        this.quantity = this.withRequiredArg("quantity", "Quantidade", ArgTypes.DOUBLE);
        this.priceSell = this.withRequiredArg("pricesell", "Preço de venda", ArgTypes.DOUBLE);
        this.priceBuy = this.withRequiredArg("pricebuy", "Preço de compra", ArgTypes.DOUBLE);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) return;

                    String tabValue = commandContext.get(this.tab);
                    String itemIdValue = commandContext.get(this.itemId);
                    Double quantityValue = commandContext.get(this.quantity);
                    Double priceSellValue = commandContext.get(this.priceSell);
                    Double priceBuyValue = commandContext.get(this.priceBuy);

                    if (tabValue == null || tabValue.isEmpty()) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_ARGUMENTS());
                        return;
                    }

                    // Verifica se a tab existe
                    if (!ShopManager.getInstance().hasTab(tabValue)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("tab", tabValue);
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_not_found", Color.RED, errorPlaceholders));
                        return;
                    }

                    if (itemIdValue == null || itemIdValue.isEmpty()) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_ARGUMENTS());
                        return;
                    }

                    // Se o itemId for "hand" ou "Hand", pega o item da mão do jogador
                    if ("hand".equalsIgnoreCase(itemIdValue)) {
                        var inventory = player.getInventory();
                        if (inventory == null) {
                            player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_iteminfo_error_inventory", Color.RED));
                            return;
                        }

                        var activeItem = inventory.getActiveHotbarItem();
                        if (activeItem == null || activeItem.isEmpty()) {
                            player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_iteminfo_no_item", Color.YELLOW));
                            return;
                        }

                        itemIdValue = activeItem.getItemId();
                        if (itemIdValue == null || itemIdValue.isEmpty()) {
                            player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_item_not_found", Color.RED, new HashMap<>()));
                            return;
                        }
                    }

                    // Verifica se o item existe
                    if (!com.economy.util.ItemManager.hasItem(itemIdValue)) {
                        Map<String, String> errorPlaceholders = new HashMap<>();
                        errorPlaceholders.put("itemid", itemIdValue);
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_item_not_found", Color.RED, errorPlaceholders));
                        return;
                    }

                    if (quantityValue == null || quantityValue <= 0) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_AMOUNT());
                        return;
                    }
                    
                    int quantityInt = quantityValue.intValue();

                    if (priceSellValue == null || priceSellValue < 0) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_AMOUNT());
                        return;
                    }

                    if (priceBuyValue == null || priceBuyValue < 0) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_AMOUNT());
                        return;
                    }

                    ShopItem item = ShopManager.getInstance().addItem(itemIdValue, quantityInt, priceSellValue, priceBuyValue, tabValue);
                    
                    // Obtém o nome do item traduzido
                    String itemName = com.economy.util.ItemManager.getItemName(itemIdValue);
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("uniqueid", String.valueOf(item.getUniqueId()));
                    placeholders.put("itemid", itemName);
                    placeholders.put("quantity", String.valueOf(quantityInt));
                    placeholders.put("pricesell", CurrencyFormatter.formatNumberOnly(priceSellValue));
                    placeholders.put("pricebuy", CurrencyFormatter.formatNumberOnly(priceBuyValue));
                    
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_item_added", Color.GREEN, placeholders));
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}

