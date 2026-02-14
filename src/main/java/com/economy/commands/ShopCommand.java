package com.economy.commands;

import com.economy.commands.subcommand.admin.ShopAddCommand;
import com.economy.commands.subcommand.admin.ShopRemoveCommand;
import com.economy.commands.subcommand.admin.ShopTabCommand;
import com.economy.gui.ShopGui;
import com.economy.npc.ShopNpcManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class ShopCommand extends AbstractAsyncCommand {

    private final ShopNpcManager npcManager;

    public ShopCommand(ShopNpcManager npcManager) {
        super("loja", com.economy.util.LanguageManager.getTranslation("desc_shop"));
        this.addAliases("shop");
        this.npcManager = npcManager;
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, CommandMessages.PERMISSION_PLAYER_SHOP);
        
        // Adiciona subcomandos admin
        this.addSubCommand(new ShopAddCommand());
        this.addSubCommand(new ShopRemoveCommand());
        this.addSubCommand(new ShopTabCommand());
        this.addSubCommand(new com.economy.commands.subcommand.admin.ShopManagerCommand());
        this.addSubCommand(new com.economy.commands.subcommand.admin.ShopNpcCommand(npcManager));
        this.addSubCommand(new com.economy.commands.subcommand.admin.ShopRenamePlayerShopCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (!com.economy.util.PermissionHelper.hasPermission(sender, CommandMessages.PERMISSION_PLAYER_SHOP)) {
            sender.sendMessage(CommandMessages.NO_PERMISSION());
            return CompletableFuture.completedFuture(null);
        }
        
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRefComponent != null) {
                        player.getPageManager().openCustomPage(ref, store, new ShopGui(playerRefComponent, CustomPageLifetime.CanDismiss));
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
