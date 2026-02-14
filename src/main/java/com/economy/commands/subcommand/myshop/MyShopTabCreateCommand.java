package com.economy.commands.subcommand.myshop;

import com.economy.Main;
import com.economy.playershop.PlayerShopManager;
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

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class MyShopTabCreateCommand extends AbstractAsyncCommand {

    private RequiredArg<String> tabName;

    public MyShopTabCreateCommand() {
        super("create", com.economy.util.LanguageManager.getTranslation("desc_myshop_tab_create"));
        this.tabName = this.withRequiredArg("name", "Nome da tab", ArgTypes.STRING);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }
        
        if (!Main.CONFIG.get().isEnablePlayerShop()) {
            player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_playershop_disabled", Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        Ref<EntityStore> ref = player.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                String tabNameValue = commandContext.get(this.tabName);
                UUID playerUuid = playerRef.getUuid();

                if (tabNameValue == null || tabNameValue.isEmpty()) {
                    player.sendMessage(com.economy.commands.CommandMessages.INVALID_ARGUMENTS());
                    return;
                }

                // Verifica se a tab já existe
                if (PlayerShopManager.getInstance().hasTab(playerUuid, tabNameValue)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("tab", tabNameValue);
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_already_exists", Color.RED, placeholders));
                    return;
                }

                // Verifica se já atingiu o limite de 7 tabs
                if (PlayerShopManager.getInstance().getAllTabs(playerUuid).size() >= 7) {
                    player.sendMessage(com.economy.util.LanguageManager.getMessage("chat_shop_tab_limit_reached", Color.RED));
                    return;
                }

                try {
                    PlayerShopManager.getInstance().createTab(playerUuid, tabNameValue);
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
    }
}

