package com.economy.commands.subcommand;

import com.economy.economy.EconomyManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class MoneyPayCommand extends AbstractAsyncCommand {

    private RequiredArg<String> targetName;
    private RequiredArg<Double> amount;

    public MoneyPayCommand() {
        super("pay", com.economy.util.LanguageManager.getTranslation("desc_money_pay"));
        this.targetName = this.withRequiredArg("nick", "Nome do jogador", ArgTypes.STRING);
        this.amount = this.withRequiredArg("valor", "Valor a transferir", ArgTypes.DOUBLE);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MONEY_PAY);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MONEY_PAY)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
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

                    String targetNameValue = commandContext.get(this.targetName);
                    Double amountValue = commandContext.get(this.amount);

                    if (targetNameValue == null || targetNameValue.isEmpty()) {
                        player.sendMessage(com.economy.commands.CommandMessages.USAGE_PAY());
                        return;
                    }

                    if (amountValue == null || amountValue <= 0) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_AMOUNT());
                        return;
                    }

                    UUID targetUuid = EconomyManager.getInstance().getPlayerUuidByName(targetNameValue);
                    if (targetUuid == null) {
                        player.sendMessage(com.economy.commands.CommandMessages.PLAYER_NOT_FOUND());
                        return;
                    }

                    if (targetUuid.equals(playerRef.getUuid())) {
                        player.sendMessage(com.economy.commands.CommandMessages.CANNOT_PAY_YOURSELF());
                        return;
                    }

                    if (!EconomyManager.getInstance().hasBalance(playerRef.getUuid(), amountValue)) {
                        player.sendMessage(com.economy.commands.CommandMessages.INSUFFICIENT_BALANCE());
                        return;
                    }

                    EconomyManager.getInstance().subtractBalance(playerRef.getUuid(), amountValue);
                    EconomyManager.getInstance().addBalance(targetUuid, amountValue);

                    player.sendMessage(com.economy.commands.CommandMessages.PAYMENT_SENT());
                    
                    // Notifica o jogador que recebeu o dinheiro (usando PlayerRef como no chat-plus)
                    com.hypixel.hytale.server.core.universe.PlayerRef targetPlayerRef = getOnlinePlayerRefByUuid(targetUuid);
                    if (targetPlayerRef != null) {
                        java.util.Map<String, String> receivedPlaceholders = new java.util.HashMap<>();
                        receivedPlaceholders.put("amount", com.economy.util.CurrencyFormatter.format(amountValue));
                        receivedPlaceholders.put("player", player.getDisplayName());
                        com.hypixel.hytale.server.core.Message message = com.economy.util.LanguageManager.getMessage("chat_money_received_from", java.awt.Color.GREEN, receivedPlaceholders);
                        targetPlayerRef.sendMessage(message);
                    }
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
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

