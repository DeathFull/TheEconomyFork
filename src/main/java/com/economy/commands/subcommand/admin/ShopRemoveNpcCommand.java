package com.economy.commands.subcommand.admin;

import com.economy.npc.ShopNpcData;
import com.economy.npc.ShopNpcManager;
import com.economy.util.LanguageManager;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class ShopRemoveNpcCommand extends AbstractAsyncCommand {

    private final ShopNpcManager npcManager;
    private RequiredArg<String> shopIdArg;

    public ShopRemoveNpcCommand(ShopNpcManager npcManager) {
        super("remove", com.economy.util.LanguageManager.getTranslation("desc_shop_npc_remove"));
        this.setPermissionGroup(GameMode.Creative);
        this.npcManager = npcManager;
        
        // Permite argumentos extras para aceitar o Shop ID
        this.setAllowsExtraArguments(true);
        
        // Aceita Shop ID obrigatório (pode ser número ou string)
        this.shopIdArg = this.withRequiredArg("shopId", "Shop ID do NPC", ArgTypes.STRING);
        
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
                // Remove o comando base e o subcomando "remove"
                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 3) {
                    // parts[0] = comando base, parts[1] = "npc", parts[2] = "remove", parts[3] = shopId
                    if (parts.length >= 4) {
                        shopIdStr = parts[3];
                    }
                }
            }
        }
        
        if (shopIdStr == null || shopIdStr.isEmpty()) {
            sender.sendMessage(LanguageManager.getMessage("chat_npc_remove_usage", Color.RED));
            sender.sendMessage(LanguageManager.getMessage("chat_npc_remove_usage_list", Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        try {
            // Tenta interpretar como Shop ID (número)
            int shopId = Integer.parseInt(shopIdStr);
            ShopNpcData npc = npcManager.getNpcByShopId(shopId);
            if (npc == null) {
                sender.sendMessage(LanguageManager.getMessage("chat_npc_not_found", Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            
            boolean removed = npcManager.removeNpc(npc.npcId);
            if (removed) {
                sender.sendMessage(LanguageManager.getMessage("chat_npc_removed", Color.GREEN));
            } else {
                sender.sendMessage(LanguageManager.getMessage("chat_npc_not_found", Color.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(LanguageManager.getMessage("chat_npc_invalid_shop_id", Color.RED));
        } catch (Exception e) {
            sender.sendMessage(LanguageManager.getMessage("chat_npc_invalid_shop_id", Color.RED));
        }
        
        return CompletableFuture.completedFuture(null);
    }
}

