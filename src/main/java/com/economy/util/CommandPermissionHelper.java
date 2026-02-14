package com.economy.util;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import java.lang.reflect.Method;

/**
 * Helper para definir permissões nos comandos do Hytale via reflexão
 * Isso garante que as permissões sejam registradas no sistema e apareçam no LuckPerms
 */
public class CommandPermissionHelper {

    /**
     * Tenta definir a permissão de um comando usando reflexão.
     * Tenta múltiplos métodos possíveis para garantir compatibilidade.
     * 
     * @param command O comando a ter a permissão definida
     * @param permission A permissão a definir
     */
    public static void setCommandPermission(AbstractAsyncCommand command, String permission) {
        if (command == null || permission == null || permission.isEmpty()) {
            return;
        }

        // Tenta múltiplos métodos possíveis
        String[] methodNames = {
            "setPermission",
            "requirePermission",
            "withPermission",
            "permission"
        };

        for (String methodName : methodNames) {
            try {
                Method method = findMethod(command.getClass(), methodName, String.class);
                if (method != null) {
                    method.invoke(command, permission);
                    return; // Sucesso, para de tentar
                }
            } catch (Exception e) {
                // Continua tentando outros métodos
            }
        }

        // Se nenhum método funcionou, tenta definir via campo diretamente
        try {
            java.lang.reflect.Field permissionField = findField(command.getClass(), "permission");
            if (permissionField != null) {
                permissionField.setAccessible(true);
                permissionField.set(command, permission);
            }
        } catch (Exception e) {
            // Ignora se não conseguir
        }
    }

    /**
     * Procura um método na hierarquia de classes
     */
    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                Method method = currentClass.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Procura um campo na hierarquia de classes
     */
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}

