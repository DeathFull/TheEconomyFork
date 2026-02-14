package com.economy.commands.subcommand.admin;

import com.economy.npc.ShopNpcManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class ShopRemoveAllNpcCommand extends AbstractAsyncCommand {

    private final ShopNpcManager npcManager;

    public ShopRemoveAllNpcCommand(ShopNpcManager npcManager) {
        super("removeall", com.economy.util.LanguageManager.getTranslation("desc_shop_npc_removeall"));
        this.setPermissionGroup(GameMode.Creative);
        this.npcManager = npcManager;
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_REMOVE);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_REMOVE)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }

        npcManager.removeAllNpcs();
        sender.sendMessage(LanguageManager.getMessage("chat_npc_all_removed", Color.GREEN));
        return CompletableFuture.completedFuture(null);
    }
}



