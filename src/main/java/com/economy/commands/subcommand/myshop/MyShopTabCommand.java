package com.economy.commands.subcommand.myshop;

import com.economy.Main;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class MyShopTabCommand extends AbstractAsyncCommand {

    public MyShopTabCommand() {
        super("tab", com.economy.util.LanguageManager.getTranslation("desc_myshop_tab"));
        this.addSubCommand(new MyShopTabCreateCommand());
        this.addSubCommand(new MyShopTabRemoveCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }
        
        if (!Main.CONFIG.get().isEnablePlayerShop()) {
            player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_playershop_disabled", Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        // Se nenhum subcomando foi especificado, mostra a mensagem de uso
        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_myshop_usage_tab", Color.YELLOW));
        return CompletableFuture.completedFuture(null);
    }
}

