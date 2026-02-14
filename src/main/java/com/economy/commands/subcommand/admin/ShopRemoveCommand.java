package com.economy.commands.subcommand.admin;

import com.economy.shop.ShopManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class ShopRemoveCommand extends AbstractAsyncCommand {

    private RequiredArg<Double> uniqueId;

    public ShopRemoveCommand() {
        super("remove", com.economy.util.LanguageManager.getTranslation("desc_shop_remove"));
        this.setPermissionGroup(GameMode.Creative);
        this.uniqueId = this.withRequiredArg("id", "ID único do item", ArgTypes.DOUBLE);
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_REMOVE);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, com.economy.commands.CommandMessages.PERMISSION_ADMIN_SHOP_REMOVE)) {
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

                    Double uniqueIdValue = commandContext.get(this.uniqueId);

                    if (uniqueIdValue == null) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_ARGUMENTS());
                        return;
                    }
                    
                    int uniqueIdInt = uniqueIdValue.intValue();

                    if (!ShopManager.getInstance().hasItem(uniqueIdInt)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("uniqueid", String.valueOf(uniqueIdInt));
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_item_not_found", Color.RED, placeholders));
                        return;
                    }

                    boolean removed = ShopManager.getInstance().removeItem(uniqueIdInt);
                    
                    if (removed) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("uniqueid", String.valueOf(uniqueIdInt));
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_item_removed", Color.GREEN, placeholders));
                    } else {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("uniqueid", String.valueOf(uniqueIdInt));
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_item_not_found", Color.RED, placeholders));
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
}

