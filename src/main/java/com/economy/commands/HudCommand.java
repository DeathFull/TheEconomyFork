package com.economy.commands;

import com.economy.Main;
import com.economy.hud.HudPreferenceManager;
import com.economy.systems.EconomyHudSystem;
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

public class HudCommand extends AbstractAsyncCommand {

    public HudCommand() {
        super("hud", com.economy.util.LanguageManager.getTranslation("desc_hud"));
        this.addAliases("economyhud", "ehud");
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_HUD);
        
        // Adiciona subcomandos
        this.addSubCommand(new HudOnCommand());
        this.addSubCommand(new HudOffCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_HUD)) {
            sender.sendMessage(CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        // Verifica se a HUD está habilitada no servidor
        if (Main.CONFIG == null || Main.CONFIG.get() == null || !Main.CONFIG.get().isEnableHud()) {
            player.sendMessage(LanguageManager.getMessage("chat_hud_server_disabled", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        // Se nenhum subcomando foi especificado, mostra a mensagem de uso
        player.sendMessage(LanguageManager.getMessage("chat_hud_usage", Color.YELLOW));
        return CompletableFuture.completedFuture(null);
    }
    
    private static class HudOnCommand extends AbstractAsyncCommand {
        public HudOnCommand() {
            super("on", com.economy.util.LanguageManager.getTranslation("desc_hud_on"));
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            return processHudAction(commandContext, true);
        }
    }
    
    private static class HudOffCommand extends AbstractAsyncCommand {
        public HudOffCommand() {
            super("off", com.economy.util.LanguageManager.getTranslation("desc_hud_off"));
        }
        
        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            return processHudAction(commandContext, false);
        }
    }
    
    private static CompletableFuture<Void> processHudAction(CommandContext commandContext, boolean enable) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_HUD)) {
            sender.sendMessage(CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        // Verifica se a HUD está habilitada no servidor
        if (Main.CONFIG == null || Main.CONFIG.get() == null || !Main.CONFIG.get().isEnableHud()) {
            player.sendMessage(LanguageManager.getMessage("chat_hud_server_disabled", Color.RED));
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
            try {
                HudPreferenceManager preferenceManager = HudPreferenceManager.getInstance();
                
                // Obtém o PlayerRef e processa dentro do world.execute() para garantir thread correta
                world.execute(() -> {
                    try {
                        PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                        if (playerRef == null) {
                            player.sendMessage(LanguageManager.getMessage("chat_hud_error", Color.RED));
                            return;
                        }
                        
                        UUID playerUuid = playerRef.getUuid();
                        
                        if (enable) {
                            preferenceManager.setHudEnabled(playerUuid, true);
                            player.sendMessage(LanguageManager.getMessage("chat_hud_enabled", Color.GREEN));
                            
                            // Atualiza a HUD imediatamente
                            EconomyHudSystem.createHudForPlayer(player, playerRef);
                        } else {
                            preferenceManager.setHudEnabled(playerUuid, false);
                            player.sendMessage(LanguageManager.getMessage("chat_hud_disabled", Color.GREEN));
                            
                            // Remove a HUD imediatamente (passa o player para acessar o HudManager)
                            EconomyHudSystem.removeHudForPlayer(player, playerRef);
                        }
                    } catch (Exception e) {
                        player.sendMessage(LanguageManager.getMessage("chat_hud_error", Color.RED));
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                player.sendMessage(LanguageManager.getMessage("chat_hud_error", Color.RED));
                e.printStackTrace();
            }
        });
    }
}

