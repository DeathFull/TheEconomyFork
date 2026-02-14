package com.economy.util;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.lang.reflect.Method;

/**
 * Helper class para verificação de permissões compatível com LuckPerms e outros plugins de permissão
 */
public class PermissionHelper {

    /**
     * Verifica se o sender tem a permissão especificada.
     * Tenta múltiplas formas de verificação para garantir compatibilidade com LuckPerms.
     * 
     * @param sender O CommandSender a verificar
     * @param permission A permissão a verificar
     * @return true se o sender tem a permissão, false caso contrário
     */
    public static boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null || permission == null || permission.isEmpty()) {
            return false;
        }

        // Método 1: Verificação direta usando hasPermission
        try {
            if (sender.hasPermission(permission)) {
                return true;
            }
        } catch (Exception e) {
            // Continua para outros métodos
        }

        // Método 2: Verificação via reflexão (compatibilidade com LuckPerms)
        try {
            Method hasPermissionMethod = sender.getClass().getMethod("hasPermission", String.class);
            Boolean result = (Boolean) hasPermissionMethod.invoke(sender, permission);
            if (result != null && result) {
                return true;
            }
        } catch (Exception e) {
            // Continua para outros métodos
        }

        // Método 3: Verifica permissões com wildcards
        // Ex: se verificar "theeconomy.player.money", também verifica "theeconomy.player.*" e "theeconomy.*"
        String[] parts = permission.split("\\.");
        for (int i = parts.length - 1; i >= 0; i--) {
            StringBuilder wildcardPerm = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) wildcardPerm.append(".");
                wildcardPerm.append(parts[j]);
            }
            if (i > 0) {
                wildcardPerm.append(".*");
            } else {
                wildcardPerm.append("*");
            }
            
            String wildcard = wildcardPerm.toString();
            
            try {
                if (sender.hasPermission(wildcard)) {
                    return true;
                }
            } catch (Exception e) {
                // Continua
            }
            
            try {
                Method hasPermissionMethod = sender.getClass().getMethod("hasPermission", String.class);
                Boolean result = (Boolean) hasPermissionMethod.invoke(sender, wildcard);
                if (result != null && result) {
                    return true;
                }
            } catch (Exception e) {
                // Continua
            }
        }

        // Método 4: Verifica permissão universal "*" (apenas para console/op)
        try {
            if (sender.hasPermission("*")) {
                return true;
            }
        } catch (Exception e) {
            // Ignora
        }

        // Método 5: Se for Player, tenta verificar via PlayerRef (método alternativo)
        if (sender instanceof Player player) {
            try {
                // Tenta obter PlayerRef e verificar permissão através dele
                var ref = player.getReference();
                if (ref != null && ref.isValid()) {
                    var store = ref.getStore();
                    // Alguns sistemas de permissão podem estar integrados no PlayerRef
                    // Tenta verificar novamente com o player
                    if (player.hasPermission(permission)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Ignora
            }
        }

        return false;
    }
}

