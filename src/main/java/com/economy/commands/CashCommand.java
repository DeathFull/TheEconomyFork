package com.economy.commands;

import com.economy.commands.subcommand.admin.CashGiveCommand;
import com.economy.economy.EconomyManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class CashCommand extends AbstractAsyncCommand {

    private OptionalArg<String> playerName;

    public CashCommand() {
        super("cash", com.economy.util.LanguageManager.getTranslation("desc_cash"));
        this.addAliases("cashbalance", "cb");
        this.setPermissionGroup(GameMode.Adventure);
        this.playerName = this.withOptionalArg("nick", "Nome do jogador", ArgTypes.STRING);

        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_CASH);

        this.addSubCommand(new CashGiveCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_CASH)) {
            sender.sendMessage(CommandMessages.NO_PERMISSION());
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

                    String targetName = commandContext.get(this.playerName);
                    UUID targetUuid;

                    if (targetName == null || targetName.isEmpty()) {
                        // Mostra o próprio saldo
                        targetUuid = playerRef.getUuid();
                    } else {
                        // Busca o jogador pelo nome
                        targetUuid = EconomyManager.getInstance().getPlayerUuidByName(targetName);
                        if (targetUuid == null) {
                            player.sendMessage(CommandMessages.PLAYER_NOT_FOUND());
                            return;
                        }
                    }

                    int cash = EconomyManager.getInstance().getCash(targetUuid);
                    String playerNameDisplay = EconomyManager.getInstance().getPlayerName(targetUuid);
                    if (playerNameDisplay.equals("Desconhecido")) {
                        playerNameDisplay = targetName != null ? targetName : player.getDisplayName();
                    }

                    // Formata o cash sem símbolo de moeda
                    final String cashText = com.economy.util.CurrencyFormatter.formatCash(cash);

                    // Variável final para usar no lambda
                    final String finalPlayerName = playerNameDisplay;

                    // Usa o sistema de título da documentação do Hytale
                    world.execute(() -> {
                        // Título principal com o cash formatado em dourado
                        Message titleMessage = Message.raw(cashText).color(new Color(255, 215, 0));
                        // Subtítulo com o nome do jogador em branco
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("player", finalPlayerName);
                        Message subtitleMessage = com.economy.util.LanguageManager.getMessage("chat_cash_of", Color.WHITE, placeholders);
                        
                        // Usa a versão com 4 parâmetros conforme a documentação
                        EventTitleUtil.showEventTitleToPlayer(
                            playerRef,
                            titleMessage,
                            subtitleMessage,
                            true // Show animation
                        );
                    });
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

