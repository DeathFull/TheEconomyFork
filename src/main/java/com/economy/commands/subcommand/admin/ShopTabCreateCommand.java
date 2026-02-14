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

public class ShopTabCreateCommand extends AbstractAsyncCommand {

    private RequiredArg<String> tabName;

    public ShopTabCreateCommand() {
        super("create", com.economy.util.LanguageManager.getTranslation("desc_shop_tab_create"));
        this.setPermissionGroup(GameMode.Creative);
        this.tabName = this.withRequiredArg("name", "Nome da tab", ArgTypes.STRING);
        
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
        
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) return;

                    String tabNameValue = commandContext.get(this.tabName);

                    if (tabNameValue == null || tabNameValue.isEmpty()) {
                        player.sendMessage(com.economy.commands.CommandMessages.INVALID_ARGUMENTS());
                        return;
                    }

                    // Verifica se a tab já existe
                    if (ShopManager.getInstance().hasTab(tabNameValue)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("tab", tabNameValue);
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_already_exists", Color.RED, placeholders));
                        return;
                    }

                    // Verifica se já atingiu o limite de 7 tabs
                    if (ShopManager.getInstance().getAllTabs().size() >= 7) {
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_limit_reached", Color.RED));
                        return;
                    }

                    try {
                        ShopManager.getInstance().createTab(tabNameValue);
                    } catch (IllegalStateException e) {
                        player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_limit_reached", Color.RED));
                        return;
                    }
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("tab", tabNameValue);
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_created", Color.GREEN, placeholders));
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

