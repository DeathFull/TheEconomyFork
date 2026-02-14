package com.economy.commands;

import com.economy.util.ItemManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemInfoCommand extends AbstractAsyncCommand {

    public ItemInfoCommand() {
        super("iteminfo", com.economy.util.LanguageManager.getTranslation("desc_iteminfo"));
        this.addAliases("ii", "item");
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_ITEMINFO);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_ITEMINFO)) {
            sender.sendMessage(CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // Obtém o item na mão do jogador
                var inventory = player.getInventory();
                if (inventory == null) {
                    player.sendMessage(LanguageManager.getMessage("chat_iteminfo_error_inventory", Color.RED));
                    return;
                }

                // Tenta obter o item ativo (na mão)
                var activeItem = inventory.getActiveHotbarItem();
                if (activeItem == null || activeItem.isEmpty()) {
                    player.sendMessage(LanguageManager.getMessage("chat_iteminfo_no_item", Color.YELLOW));
                    return;
                }

                // Obtém o ID do item
                String itemId = activeItem.getItemId();
                if (itemId == null || itemId.isEmpty()) {
                    player.sendMessage(LanguageManager.getMessage("chat_iteminfo_invalid_id", Color.RED));
                    return;
                }

                // Obtém o nome do item traduzido (usando o ItemManager)
                String itemName = ItemManager.getItemName(itemId);

                // Envia a mensagem no chat usando tradução
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("itemid", itemId);
                placeholders.put("itemname", itemName);
                player.sendMessage(LanguageManager.getMessage("chat_iteminfo_info", Color.WHITE, placeholders));
            } catch (Exception e) {
                Map<String, String> errorPlaceholders = new HashMap<>();
                errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                player.sendMessage(LanguageManager.getMessage("chat_error_item_info", Color.RED, errorPlaceholders));
                e.printStackTrace();
            }
        });
    }

}

