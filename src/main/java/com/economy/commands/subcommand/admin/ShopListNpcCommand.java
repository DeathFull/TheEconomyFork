package com.economy.commands.subcommand.admin;

import com.economy.npc.ShopNpcData;
import com.economy.npc.ShopNpcManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopListNpcCommand extends AbstractAsyncCommand {

    private final ShopNpcManager npcManager;

    public ShopListNpcCommand(ShopNpcManager npcManager) {
        super("list", com.economy.util.LanguageManager.getTranslation("desc_shop_npc_list"));
        this.setPermissionGroup(GameMode.Creative);
        this.npcManager = npcManager;
        
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

        List<ShopNpcData> npcs = npcManager.getAllNpcs();
        if (npcs.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage("chat_npc_list_empty", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("count", String.valueOf(npcs.size()));
        sender.sendMessage(LanguageManager.getMessage("chat_npc_list_header", headerPlaceholders));

        for (int i = 0; i < npcs.size(); i++) {
            ShopNpcData npc = npcs.get(i);
            Map<String, String> itemPlaceholders = new HashMap<>();
            itemPlaceholders.put("index", String.valueOf(i + 1));
            itemPlaceholders.put("shopId", String.valueOf(npc.shopId));
            itemPlaceholders.put("npcId", npc.npcId.toString().substring(0, 8));
            itemPlaceholders.put("worldId", npc.worldUuid.substring(0, 8));
            itemPlaceholders.put("x", String.format("%.1f", npc.x));
            itemPlaceholders.put("y", String.format("%.1f", npc.y));
            itemPlaceholders.put("z", String.format("%.1f", npc.z));
            sender.sendMessage(LanguageManager.getMessage("chat_npc_list_item", itemPlaceholders));
        }

        return CompletableFuture.completedFuture(null);
    }
}



