package com.economy.commands;

import com.economy.Main;
import com.economy.commands.subcommand.myshop.MyShopAddCommand;
import com.economy.commands.subcommand.myshop.MyShopCloseCommand;
import com.economy.commands.subcommand.myshop.MyShopOpenCommand;
import com.economy.commands.subcommand.myshop.MyShopRemoveCommand;
import com.economy.commands.subcommand.myshop.MyShopRenameCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class MyShopCommand extends AbstractAsyncCommand {

    public MyShopCommand() {
        super("myshop", com.economy.util.LanguageManager.getTranslation("desc_myshop"));
        this.addAliases("mshop", "minhaloja");
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_MYSHOP);
        
        // Adiciona subcomandos
        this.addSubCommand(new MyShopOpenCommand());
        this.addSubCommand(new MyShopCloseCommand());
        this.addSubCommand(new MyShopAddCommand());
        this.addSubCommand(new MyShopRemoveCommand());
        this.addSubCommand(new MyShopRenameCommand());
        this.addSubCommand(new com.economy.commands.subcommand.myshop.MyShopTabCommand());
        this.addSubCommand(new com.economy.commands.subcommand.myshop.MyShopManagerCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        // O sistema de subcomandos do Hytale deve rotear automaticamente para os subcomandos
        // Se chegou aqui, significa que nenhum subcomando foi especificado
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_MYSHOP)) {
            sender.sendMessage(CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        if (!Main.CONFIG.get().isEnablePlayerShop()) {
            player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_playershop_disabled", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        // Se nenhum subcomando foi especificado, mostra a mensagem de uso
        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_myshop_usage", Color.YELLOW));
        return CompletableFuture.completedFuture(null);
    }
}
