package com.economy.commands.subcommand.myshop;

import com.economy.Main;
import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.InventoryHelper;
import com.economy.util.LanguageManager;
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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MyShopRemoveCommand extends AbstractAsyncCommand {

    private RequiredArg<Double> uniqueIdArg;

    public MyShopRemoveCommand() {
        super("remove", com.economy.util.LanguageManager.getTranslation("desc_myshop_remove"));
        this.uniqueIdArg = this.withRequiredArg("uniqueId", "ID único do item", ArgTypes.DOUBLE);
        
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

        Double uniqueIdValue = commandContext.get(uniqueIdArg);
        if (uniqueIdValue == null) {
            player.sendMessage(LanguageManager.getMessage("chat_myshop_usage_remove", Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        final int finalUniqueId = uniqueIdValue.intValue();

        return CompletableFuture.runAsync(() -> {
            world.execute(() -> {
                try {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) {
                        return;
                    }

                    UUID playerUuid = playerRef.getUuid();
                    PlayerShopManager shopManager = PlayerShopManager.getInstance();
                    
                    PlayerShopItem item = shopManager.getItem(finalUniqueId);
                    if (item == null) {
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED));
                        return;
                    }

                    // IMPORTANTE: Verifica se o item pertence ao jogador antes de remover
                    if (item.getOwnerUuid() == null || !playerUuid.equals(item.getOwnerUuid())) {
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_not_owner", Color.RED));
                        return;
                    }

                    // CRÍTICO: Verifica o estoque atual ANTES de remover
                    // Se o estoque estiver zerado, não devolve nada para evitar duplicação
                    int currentStock = item.getStock();
                    String itemId = item.getItemId();
                    boolean itemAdded = false;
                    
                    // IMPORTANTE: Só devolve itens se o estoque for maior que 0
                    // Usa getStock() (estoque atual) e NÃO getQuantity() (quantidade original)
                    if (currentStock > 0) {
                        // Devolve APENAS o estoque atual, não a quantidade original
                        itemAdded = InventoryHelper.addItem(player, itemId, currentStock, item.getDurability());
                        
                        // Se não conseguiu adicionar ao inventário, cancela a remoção para evitar perda de itens
                        if (!itemAdded) {
                            player.sendMessage(LanguageManager.getMessage("chat_myshop_error_inventory_full", Color.YELLOW));
                            player.sendMessage(LanguageManager.getMessage("chat_myshop_error_remove_item", Color.RED));
                            return;
                        }
                    }
                    // Se o estoque for 0, não devolve nada, apenas remove o registro da loja

                    // Remove o item da loja APENAS se a devolução foi bem-sucedida (ou se estoque era 0)
                    if (shopManager.removeItem(finalUniqueId)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("uniqueid", String.valueOf(finalUniqueId));
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_item_removed", Color.GREEN, placeholders));
                    } else {
                        // Se a remoção falhou, tenta devolver os itens se foram adicionados (rollback)
                        if (itemAdded && currentStock > 0) {
                            // Tenta remover os itens que foram adicionados (rollback)
                            InventoryHelper.removeItem(player, itemId, currentStock);
                        }
                        player.sendMessage(LanguageManager.getMessage("chat_shop_item_not_found", Color.RED));
                    }
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage() != null ? e.getMessage() : "");
                    player.sendMessage(LanguageManager.getMessage("chat_error_item_remove", Color.RED, errorPlaceholders));
                    e.printStackTrace();
                }
            });
        }, world);
    }
}

