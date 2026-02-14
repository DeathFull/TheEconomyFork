package com.economy.commands.subcommand.admin;

import com.economy.economy.EconomyManager;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class CashGiveCommand extends AbstractAsyncCommand {

    private RequiredArg<String> targetName;
    private RequiredArg<Integer> amount;

    public CashGiveCommand() {
        super("give", com.economy.util.LanguageManager.getTranslation("desc_cash_give"));
        this.setPermissionGroup(GameMode.Creative);
        this.targetName = this.withRequiredArg("nick", "Nome do jogador", ArgTypes.STRING);
        this.amount = this.withRequiredArg("valor", "Valor a adicionar", ArgTypes.INTEGER);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_CASH_GIVE);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_ADMIN_CASH_GIVE)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        String targetNameValue = commandContext.get(this.targetName);
        Integer amountValue = commandContext.get(this.amount);

        if (targetNameValue == null || targetNameValue.isEmpty()) {
            sender.sendMessage(com.economy.commands.CommandMessages.USAGE_CASH_GIVE());
            return CompletableFuture.completedFuture(null);
        }

        if (amountValue == null || amountValue <= 0) {
            sender.sendMessage(com.economy.commands.CommandMessages.INVALID_AMOUNT());
            return CompletableFuture.completedFuture(null);
        }
        
        // Se for Player, executa no World do player; se for Console, executa diretamente
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    executeGiveCommand(sender, targetNameValue, amountValue);
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            // Console - executa diretamente sem World
            return CompletableFuture.runAsync(() -> {
                executeGiveCommand(sender, targetNameValue, amountValue);
            });
        }
    }
    
    private void executeGiveCommand(CommandSender sender, String targetNameValue, Integer amountValue) {
        UUID targetUuid = EconomyManager.getInstance().getPlayerUuidByName(targetNameValue);
        if (targetUuid == null) {
            sender.sendMessage(com.economy.commands.CommandMessages.PLAYER_NOT_FOUND());
            return;
        }

        EconomyManager.getInstance().addCash(targetUuid, amountValue);
        String playerName = EconomyManager.getInstance().getPlayerName(targetUuid);
        int newCash = EconomyManager.getInstance().getCash(targetUuid);
        sender.sendMessage(com.economy.commands.CommandMessages.CASH_ADDED());
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("amount", com.economy.util.CurrencyFormatter.formatCash(amountValue));
        placeholders.put("player", playerName);
        placeholders.put("cash", com.economy.util.CurrencyFormatter.formatCash(newCash));
        sender.sendMessage(com.economy.util.LanguageManager.getMessage("chat_cash_added_to", java.awt.Color.GREEN, placeholders));
        
        // Notifica o jogador alvo se estiver online usando PlayerRef (como no chat-plus)
        com.hypixel.hytale.server.core.universe.PlayerRef targetPlayerRef = getOnlinePlayerRefByUuid(targetUuid);
        if (targetPlayerRef != null) {
            java.util.Map<String, String> receivedPlaceholders = new java.util.HashMap<>();
            receivedPlaceholders.put("amount", com.economy.util.CurrencyFormatter.formatCash(amountValue));
            com.hypixel.hytale.server.core.Message message = com.economy.util.LanguageManager.getMessage("chat_cash_received", java.awt.Color.GREEN, receivedPlaceholders);
            targetPlayerRef.sendMessage(message);
        }
    }
    
    /**
     * Obtém um PlayerRef online pelo UUID usando o mesmo método do chat-plus.
     * 
     * @param uuid O UUID do jogador
     * @return O PlayerRef se estiver online, null caso contrário
     */
    private com.hypixel.hytale.server.core.universe.PlayerRef getOnlinePlayerRefByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        try {
            // Usa o mesmo método do chat-plus: Universe.get() -> getWorlds() -> getPlayers() -> getPlayerRef()
            Class<?> universeClass = Class.forName("com.hypixel.hytale.server.core.universe.Universe");
            java.lang.reflect.Method getMethod = universeClass.getMethod("get");
            Object universe = getMethod.invoke(null);
            
            if (universe != null) {
                java.lang.reflect.Method getWorldsMethod = universe.getClass().getMethod("getWorlds");
                Object worlds = getWorldsMethod.invoke(universe);
                
                if (worlds != null) {
                    java.util.Collection<?> worldsCollection = null;
                    if (worlds instanceof java.util.Collection) {
                        worldsCollection = (java.util.Collection<?>) worlds;
                    } else if (worlds instanceof java.util.Map) {
                        worldsCollection = new java.util.ArrayList<>(((java.util.Map<?, ?>) worlds).values());
                    }
                    
                    if (worldsCollection != null) {
                        for (Object worldObj : worldsCollection) {
                            if (worldObj != null) {
                                try {
                                    java.lang.reflect.Method getPlayersMethod = worldObj.getClass().getMethod("getPlayers");
                                    Object playersResult = getPlayersMethod.invoke(worldObj);
                                    
                                    if (playersResult instanceof java.util.Collection) {
                                        java.util.Collection<Player> worldPlayers = (java.util.Collection<Player>) playersResult;
                                        for (Player player : worldPlayers) {
                                            if (player != null) {
                                                // Obtém o PlayerRef do Player
                                                com.hypixel.hytale.server.core.universe.PlayerRef playerRef = getPlayerRefFromPlayer(player);
                                                if (playerRef != null && uuid.equals(playerRef.getUuid())) {
                                                    return playerRef;
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Continua procurando em outros mundos
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erros - se não conseguir obter, retorna null
        }
        
        return null;
    }
    
    /**
     * Obtém PlayerRef a partir de um Player (mesmo método do chat-plus).
     */
    private com.hypixel.hytale.server.core.universe.PlayerRef getPlayerRefFromPlayer(Player player) {
        try {
            java.lang.reflect.Method getPlayerRefMethod = player.getClass().getMethod("getPlayerRef");
            Object playerRef = getPlayerRefMethod.invoke(player);
            if (playerRef instanceof com.hypixel.hytale.server.core.universe.PlayerRef) {
                return (com.hypixel.hytale.server.core.universe.PlayerRef) playerRef;
            }
        } catch (Exception e) {
            // Ignora erros
        }
        return null;
    }
}

