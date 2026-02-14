package com.economy.commands.subcommand.myshop;

import com.economy.Main;
import com.economy.playershop.PlayerShopManager;
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

public class MyShopRenameCommand extends AbstractAsyncCommand {

    private RequiredArg<String> shopNameArg;

    public MyShopRenameCommand() {
        super("rename", com.economy.util.LanguageManager.getTranslation("desc_myshop_rename"));
        
        // Define a permissão do comando via reflexão para aparecer no LuckPerms
        com.economy.util.CommandPermissionHelper.setCommandPermission(this, com.economy.commands.CommandMessages.PERMISSION_PLAYER_MYSHOP);
        
        // Argumento: nome da loja (aceita texto com espaços)
        this.shopNameArg = this.withRequiredArg("nome", "Nome da loja", ArgTypes.STRING);
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
        
        // Tenta pegar o resto do comando após "rename" usando reflection
        String nameValue = null;
        try {
            // Tenta vários métodos possíveis para pegar o input completo
            java.lang.reflect.Method[] methods = commandContext.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.equals("input") || methodName.equals("getinput") || methodName.equals("rawinput") || 
                     methodName.equals("getrawinput") || methodName.equals("remaininginput")) && 
                    method.getParameterCount() == 0) {
                    try {
                        Object result = method.invoke(commandContext);
                        if (result instanceof String) {
                            String input = (String) result;
                            if (input != null && !input.isEmpty()) {
                                // Remove "myshop rename ", "mshop rename " ou "minhaloja rename " do início
                                String[] prefixes = {"myshop rename ", "mshop rename ", "minhaloja rename "};
                                for (String prefix : prefixes) {
                                    if (input.toLowerCase().startsWith(prefix.toLowerCase())) {
                                        nameValue = input.substring(prefix.length());
                                        break;
                                    }
                                }
                                if (nameValue == null) {
                                    // Se não encontrou prefixo, tenta pegar tudo após "rename "
                                    int renameIndex = input.toLowerCase().indexOf("rename ");
                                    if (renameIndex >= 0) {
                                        nameValue = input.substring(renameIndex + "rename ".length());
                                    }
                                }
                            }
                            break;
                        }
                    } catch (Exception e) {
                        // Continua tentando outros métodos
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erros de reflection
        }
        
        // Se não conseguiu pegar pelo input completo, tenta pegar o argumento STRING
        if (nameValue == null || nameValue.trim().isEmpty()) {
            nameValue = commandContext.get(this.shopNameArg);
        }
        
        if (nameValue == null || nameValue.trim().isEmpty()) {
            player.sendMessage(LanguageManager.getMessage("chat_myshop_usage_rename", Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        // Processa o nome: remove espaços extras e aspas se existirem
        String trimmedName = nameValue.trim();
        
        // Remove aspas do início e fim se existirem (pode ter uma ou duas aspas)
        while (trimmedName.startsWith("\"") && trimmedName.endsWith("\"") && trimmedName.length() > 1) {
            trimmedName = trimmedName.substring(1, trimmedName.length() - 1).trim();
        }
        
        // Se não tiver aspas, usa o nome como está (já está trimmed)
        // Limita o tamanho
        if (trimmedName.length() > 50) {
            trimmedName = trimmedName.substring(0, 50);
        }
        
        final String finalName = trimmedName;
        
        return CompletableFuture.runAsync(() -> {
            world.execute(() -> {
                try {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) {
                        return;
                    }
                    
                    UUID playerUuid = playerRef.getUuid();
                    boolean success = PlayerShopManager.getInstance().renameShop(playerUuid, finalName);
                    
                    if (success) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("name", finalName);
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_renamed", Color.GREEN, placeholders));
                    } else {
                        player.sendMessage(LanguageManager.getMessage("chat_myshop_rename_error", Color.RED));
                    }
                } catch (Exception e) {
                    player.sendMessage(LanguageManager.getMessage("chat_myshop_rename_error", Color.RED));
                    e.printStackTrace();
                }
            });
        });
    }
}

