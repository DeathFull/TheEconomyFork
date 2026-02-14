package com.economy.commands.subcommand;

import com.economy.Main;
import com.economy.economy.EconomyManager;
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

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class MoneyTopCommand extends AbstractAsyncCommand {

    public MoneyTopCommand() {
        super("top", com.economy.util.LanguageManager.getTranslation("desc_money_top"));
        this.addAliases("ranking", "leaderboard");
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MONEY_TOP);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MONEY_TOP)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        // Verifica se o money top está habilitado na configuração
        if (Main.CONFIG != null && Main.CONFIG.get() != null && !Main.CONFIG.get().isEnableMoneyTop()) {
            sender.sendMessage(com.economy.util.LanguageManager.getMessage("chat_money_top_disabled", java.awt.Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) return;

                    List<Map.Entry<UUID, Double>> topBalances = EconomyManager.getInstance().getTopBalances(10);
                    
                    if (topBalances.isEmpty()) {
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_top_no_players", Color.YELLOW));
                        return;
                    }

                    // Envia o cabeçalho
                    Color goldColor = new Color(255, 215, 0);
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_top_separator", goldColor));
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_top_10_richest", goldColor).bold(true));
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_top_separator", goldColor));
                    
                    // Envia cada posição do ranking
                    int position = 1;
                    for (Map.Entry<UUID, Double> entry : topBalances) {
                        String playerName = EconomyManager.getInstance().getPlayerName(entry.getKey());
                        double balance = entry.getValue();
                        
                        Color positionColor;
                        if (position == 1) {
                            positionColor = new Color(255, 215, 0); // Dourado para 1º
                        } else if (position == 2) {
                            positionColor = new Color(192, 192, 192); // Prata para 2º
                        } else if (position == 3) {
                            positionColor = new Color(205, 127, 50); // Bronze para 3º
                        } else {
                            positionColor = Color.WHITE; // Branco para os demais
                        }
                        
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        String positionText = String.valueOf(position);
                        // Adiciona sufixo para inglês
                        String lang = com.economy.Main.CONFIG.get().getLanguage();
                        if ("EN".equals(lang)) {
                            if (position == 1) positionText = "1st";
                            else if (position == 2) positionText = "2nd";
                            else if (position == 3) positionText = "3rd";
                            else positionText = position + "th";
                        } else {
                            positionText = position + "º";
                        }
                        placeholders.put("position", positionText);
                        placeholders.put("player", playerName);
                        placeholders.put("balance", com.economy.util.CurrencyFormatter.formatNumberOnly(balance));
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_top_position", positionColor, placeholders));
                        position++;
                    }
                    
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_top_separator", goldColor));
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}

