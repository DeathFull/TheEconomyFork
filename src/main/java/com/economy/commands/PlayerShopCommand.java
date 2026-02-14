package com.economy.commands;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.gui.PlayerShopGui;
import com.economy.playershop.PlayerShopManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class PlayerShopCommand extends AbstractAsyncCommand {

    private RequiredArg<String> playerNameArg;

    public PlayerShopCommand() {
        super("playershop", com.economy.util.LanguageManager.getTranslation("desc_playershop"));
        this.addAliases("pshop", "lojajogador");
        this.playerNameArg = this.withRequiredArg("nick", "Nome do jogador", ArgTypes.STRING);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_PLAYERSHOP);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_PLAYERSHOP)) {
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

        String playerName = playerNameArg.get(commandContext);
        Ref<EntityStore> ref = player.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            return CompletableFuture.runAsync(() -> {
                UUID targetUuid = EconomyManager.getInstance().getPlayerUuidByName(playerName);
                if (targetUuid == null) {
                    player.sendMessage(com.economy.commands.CommandMessages.PLAYER_NOT_FOUND());
                    return;
                }

                PlayerShopManager shopManager = PlayerShopManager.getInstance();
                if (!shopManager.isShopOpen(targetUuid)) {
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_playershop_not_open", Color.YELLOW));
                    return;
                }

                // Abre a GUI da loja do jogador
                world.execute(() -> {
                    PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) return;
                    player.getPageManager().openCustomPage(ref, store, 
                        new PlayerShopGui(playerRef, CustomPageLifetime.CanDismiss, targetUuid));
                });
            }, world);
        } else {
            commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
            return CompletableFuture.completedFuture(null);
        }
    }
}

