package com.economy.util;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

/**
 * Utilitário para executar comandos no console do servidor.
 * Baseado na implementação do NPCQuest plugin.
 */
public final class HytaleConsoleCommands {

    private HytaleConsoleCommands() {}

    /**
     * Executa 1 comando como se fosse digitado no console do servidor.
     * Usa a mesma abordagem do NPCQuest plugin:
     * - ConsoleSender.INSTANCE de com.hypixel.hytale.server.core.console.ConsoleSender
     * - CommandManager.get().handleCommand(consoleSender, command)
     */
    public static CompletableFuture<Void> runAsConsole(String command) {
        // Remove "/" do início se presente
        String trimmed = command.trim();
        final String cmd = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;

        try {
            // Obtém CommandManager usando a API oficial
            CommandManager commandManager = CommandManager.get();
            
            if (commandManager == null) {
                com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem")
                    .at(java.util.logging.Level.WARNING).log("CommandManager is null. Command: %s", cmd);
                return CompletableFuture.completedFuture(null);
            }
            
            // Usa ConsoleSender.INSTANCE (mesma abordagem do NPCQuest)
            // Caminho correto: com.hypixel.hytale.server.core.console.ConsoleSender
            ConsoleSender consoleSender = ConsoleSender.INSTANCE;
            
            // Executa o comando usando handleCommand(CommandSender, String)
            // Conforme implementação do NPCQuest (linha 418 do QuestHandlerImpl.java)
            CompletableFuture<Void> result = commandManager.handleCommand((CommandSender)consoleSender, cmd);
            
            return result.exceptionally(ex -> {
                com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem")
                    .at(java.util.logging.Level.WARNING).log("Falha ao executar comando no console: %s", cmd);
                if (ex != null) {
                    com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem")
                        .at(java.util.logging.Level.WARNING).log("Erro: %s", ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            });
            
        } catch (Exception e) {
            com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem")
                .at(java.util.logging.Level.WARNING).log("Failed to execute console command: %s - %s", cmd, e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    /** Executa uma fila de comandos como console (um por um, na ordem) */
    public static CompletableFuture<Void> runMany(String... commands) {
        Deque<String> queue = new ArrayDeque<>();
        for (String cmd : commands) queue.add(cmd);

        // Executa em cadeia
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (String cmd : queue) {
            future = future.thenCompose(v -> runAsConsole(cmd));
        }

        return future;
    }
}

