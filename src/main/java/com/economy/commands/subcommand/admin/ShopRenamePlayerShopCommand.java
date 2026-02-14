package com.economy.commands.subcommand.admin;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class ShopRenamePlayerShopCommand extends AbstractAsyncCommand {

    private RequiredArg<String> targetName;
    private RequiredArg<String> newShopName;

    public ShopRenamePlayerShopCommand() {
        super("renameplayershop", com.economy.util.LanguageManager.getTranslation("desc_shop_renameplayershop"));
        this.addAliases("renamepshop", "rpshop");
        this.setPermissionGroup(GameMode.Creative);
        
        this.targetName = this.withRequiredArg("nick", "Nome do jogador", ArgTypes.STRING);
        this.newShopName = this.withRequiredArg("nome", "Novo nome da loja", ArgTypes.STRING);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_RENAME_PLAYER);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_RENAME_PLAYER)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (!Main.CONFIG.get().isEnablePlayerShop()) {
            sender.sendMessage(LanguageManager.getMessage("chat_playershop_disabled", Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        String targetNameValue = commandContext.get(this.targetName);
        String newShopNameValue = commandContext.get(this.newShopName);

        if (targetNameValue == null || targetNameValue.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage("chat_shop_rename_player_usage", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        if (newShopNameValue == null || newShopNameValue.trim().isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage("chat_shop_rename_player_usage", Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        // Processa o nome: remove espaços extras e aspas se existirem
        String trimmedName = newShopNameValue.trim();
        
        // Remove aspas do início e fim se existirem
        while (trimmedName.startsWith("\"") && trimmedName.endsWith("\"") && trimmedName.length() > 1) {
            trimmedName = trimmedName.substring(1, trimmedName.length() - 1).trim();
        }
        
        // Limita o tamanho
        if (trimmedName.length() > 50) {
            trimmedName = trimmedName.substring(0, 50);
        }
        
        final String finalName = trimmedName;
        
        // Se for Player, executa no World do player; se for Console, executa diretamente
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    executeRenameCommand(sender, targetNameValue, finalName);
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            // Console - executa diretamente sem World
            return CompletableFuture.runAsync(() -> {
                executeRenameCommand(sender, targetNameValue, finalName);
            });
        }
    }
    
    private void executeRenameCommand(CommandSender sender, String targetNameValue, String finalName) {
        UUID targetUuid = EconomyManager.getInstance().getPlayerUuidByName(targetNameValue);
        if (targetUuid == null) {
            sender.sendMessage(com.economy.commands.CommandMessages.PLAYER_NOT_FOUND());
            return;
        }

        boolean success = PlayerShopManager.getInstance().renameShop(targetUuid, finalName);
        
        if (success) {
            String playerName = EconomyManager.getInstance().getPlayerName(targetUuid);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            placeholders.put("name", finalName);
            sender.sendMessage(LanguageManager.getMessage("chat_shop_rename_player_success", Color.GREEN, placeholders));
        } else {
            sender.sendMessage(LanguageManager.getMessage("chat_shop_rename_player_error", Color.RED));
        }
    }
}

