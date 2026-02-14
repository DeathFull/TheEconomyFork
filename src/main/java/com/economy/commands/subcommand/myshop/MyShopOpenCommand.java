package com.economy.commands.subcommand.myshop;

import com.economy.Main;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MyShopOpenCommand extends AbstractAsyncCommand {

    public MyShopOpenCommand() {
        super("open", com.economy.util.LanguageManager.getTranslation("desc_myshop_open"));
        
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

        return CompletableFuture.runAsync(() -> {
            world.execute(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }

                UUID playerUuid = playerRef.getUuid();
                PlayerShopManager shopManager = PlayerShopManager.getInstance();
                
                // Atualiza informações do jogador (UUID e nick)
                shopManager.updatePlayerInfo(playerUuid, player.getDisplayName());
                shopManager.setShopOpen(playerUuid, true);
                player.sendMessage(LanguageManager.getMessage("chat_myshop_opened", Color.GREEN));
            });
        }, world);
    }
}

