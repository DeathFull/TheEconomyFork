package com.economy.commands.subcommand.admin;

import com.economy.npc.ShopNpcData;
import com.economy.npc.ShopNpcManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.GameMode;
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

public class ShopMoveNpcCommand extends AbstractAsyncCommand {

    private final ShopNpcManager npcManager;
    private RequiredArg<String> shopIdArg;

    public ShopMoveNpcCommand(ShopNpcManager npcManager) {
        super("movehere", com.economy.util.LanguageManager.getTranslation("desc_shop_npc_movehere"));
        this.setPermissionGroup(GameMode.Creative);
        this.npcManager = npcManager;
        
        // Permite argumentos extras para aceitar o Shop ID
        this.setAllowsExtraArguments(true);
        
        // Aceita Shop ID obrigatório
        this.shopIdArg = this.withRequiredArg("shopId", "Shop ID do NPC", ArgTypes.STRING);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_ADD)) {
            sender.sendMessage(com.economy.commands.CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }

        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Tenta obter o argumento do contexto
        String shopIdStr = null;
        try {
            shopIdStr = commandContext.get(shopIdArg);
        } catch (Exception e) {
            // Se falhar, tenta obter dos argumentos extras
        }
        
        // Se não conseguiu obter do RequiredArg, tenta processar manualmente
        if (shopIdStr == null || shopIdStr.isEmpty()) {
            // Tenta obter da string de entrada
            String input = commandContext.getInputString();
            if (input != null && !input.isEmpty()) {
                // Remove o comando base e o subcomando "movehere"
                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 4) {
                    // parts[0] = comando base, parts[1] = "npc", parts[2] = "movehere", parts[3] = shopId
                    shopIdStr = parts[3];
                }
            }
        }
        
        if (shopIdStr == null || shopIdStr.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage("chat_npc_movehere_usage", Color.RED));
            sender.sendMessage(LanguageManager.getMessage("chat_npc_remove_usage_list", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        final String finalShopIdStr = shopIdStr;

        return CompletableFuture.runAsync(() -> {
            try {
                // Tenta interpretar como Shop ID (número)
                int shopId = Integer.parseInt(finalShopIdStr);
                ShopNpcData npc = npcManager.getNpcByShopId(shopId);
                if (npc == null) {
                    world.execute(() -> {
                        sender.sendMessage(LanguageManager.getMessage("chat_npc_not_found", Color.RED));
                    });
                    return;
                }

                // Obtém a posição e rotação do jogador (mesma lógica do ShopNpcAddGui)
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    world.execute(() -> {
                        sender.sendMessage(LanguageManager.getMessage("chat_npc_error_get_position", Color.RED));
                    });
                    return;
                }

                // Obtém o Transform do PlayerRef (mesma lógica do ShopNpcAddGui)
                Transform playerTransform = playerRef.getTransform();
                if (playerTransform == null) {
                    world.execute(() -> {
                        sender.sendMessage(LanguageManager.getMessage("chat_npc_error_get_position", Color.RED));
                    });
                    return;
                }

                // Tenta obter a rotação atual do jogador do TransformComponent (mesma lógica do ShopNpcAddGui)
                try {
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent playerTransformComp = 
                        store.getComponent(ref, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                    if (playerTransformComp != null) {
                        var playerRot = playerTransformComp.getRotation();
                        if (playerRot != null) {
                            playerTransform = new Transform(
                                playerTransform.getPosition(),
                                playerRot
                            );
                        }
                    }
                } catch (Exception e) {
                    // Usa a rotação do PlayerRef se falhar
                }

                // Obtém o UUID do mundo do PlayerRef (mesma lógica do ShopNpcAddGui)
                UUID worldUuidObj = playerRef.getWorldUuid();
                if (worldUuidObj == null) {
                    world.execute(() -> {
                        sender.sendMessage(LanguageManager.getMessage("chat_npc_error_get_world_uuid", Color.RED));
                    });
                    return;
                }
                String worldUuid = worldUuidObj.toString();

                // Move o NPC
                boolean moved = npcManager.moveNpc(npc.npcId, playerTransform, worldUuid);
                if (moved) {
                    world.execute(() -> {
                        sender.sendMessage(LanguageManager.getMessage("chat_npc_moved", Color.GREEN));
                    });
                } else {
                    world.execute(() -> {
                        sender.sendMessage(LanguageManager.getMessage("chat_npc_not_found", Color.RED));
                    });
                }
            } catch (NumberFormatException e) {
                world.execute(() -> {
                    sender.sendMessage(LanguageManager.getMessage("chat_npc_invalid_shop_id", Color.RED));
                });
            } catch (Exception e) {
                world.execute(() -> {
                    sender.sendMessage(LanguageManager.getMessage("chat_npc_invalid_shop_id", Color.RED));
                });
            }
        }, world);
    }
}

