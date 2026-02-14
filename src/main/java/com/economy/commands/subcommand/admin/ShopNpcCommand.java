package com.economy.commands.subcommand.admin;

import com.economy.npc.ShopNpcManager;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class ShopNpcCommand extends AbstractAsyncCommand {

    public ShopNpcCommand(ShopNpcManager npcManager) {
        super("npc", com.economy.util.LanguageManager.getTranslation("desc_shop_npc"));
        this.setPermissionGroup(GameMode.Creative);
        
        // Adiciona subcomandos
        this.addSubCommand(new ShopAddNpcCommand(npcManager));
        this.addSubCommand(new ShopRemoveNpcCommand(npcManager));
        this.addSubCommand(new ShopListNpcCommand(npcManager));
        this.addSubCommand(new ShopRemoveAllNpcCommand(npcManager));
        this.addSubCommand(new ShopMoveNpcCommand(npcManager));
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        // Este comando apenas agrupa subcomandos, não executa nada diretamente
        return CompletableFuture.completedFuture(null);
    }
}

