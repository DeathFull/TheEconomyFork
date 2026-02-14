package com.economy.commands;

import com.economy.commands.subcommand.MoneyPayCommand;
import com.economy.commands.subcommand.MoneyTopCommand;
import com.economy.commands.subcommand.admin.MoneySetCommand;
import com.economy.commands.subcommand.admin.MoneyGiveCommand;
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

public class MoneyCommand extends AbstractAsyncCommand {

    private OptionalArg<String> playerName;

    public MoneyCommand() {
        super("money", com.economy.util.LanguageManager.getTranslation("desc_money"));
        this.addAliases("eco", "balance", "bal");
        this.setPermissionGroup(GameMode.Adventure);
        this.playerName = this.withOptionalArg("nick", "Nome do jogador", ArgTypes.STRING);

        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_MONEY);

        this.addSubCommand(new MoneyPayCommand());
        this.addSubCommand(new MoneyTopCommand());
        this.addSubCommand(new MoneySetCommand());
        this.addSubCommand(new MoneyGiveCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_MONEY)) {
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

                    double balance = EconomyManager.getInstance().getBalance(targetUuid);
                    String playerNameDisplay = EconomyManager.getInstance().getPlayerName(targetUuid);
                    if (playerNameDisplay.equals("Desconhecido")) {
                        playerNameDisplay = targetName != null ? targetName : player.getDisplayName();
                    }

                    // Formata o dinheiro com a sigla da moeda configurada
                    final String balanceText = com.economy.util.CurrencyFormatter.format(balance);

                    // Variável final para usar no lambda
                    final String finalPlayerName = playerNameDisplay;

                    // Usa o sistema de título da documentação do Hytale
                    world.execute(() -> {
                        // Título principal com o saldo formatado em dourado
                        Message titleMessage = Message.raw(balanceText).color(new Color(255, 215, 0));
                        // Subtítulo com o nome do jogador em branco
                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("player", finalPlayerName);
                        Message subtitleMessage = com.economy.util.LanguageManager.getMessage("chat_balance_of", Color.WHITE, placeholders);
                        
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

