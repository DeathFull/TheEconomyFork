package com.economy.util;

import com.economy.api.EconomyAPI;

import java.util.UUID;
import java.util.function.Function;

/**
 * Sistema de placeholders públicos para uso por outros plugins.
 * 
 * Placeholders disponíveis:
 * - %balance%: Saldo do jogador (valor numérico)
 * - %balance_formated%: Saldo do jogador formatado (com símbolo e formatação)
 * 
 * Exemplo de uso:
 * <pre>
 * String text = "Seu saldo: %balance_formated%";
 * String result = PlaceholderAPI.replacePlaceholders(playerUUID, text);
 * // Resultado: "Seu saldo: $1.000,00"
 * </pre>
 */
public class PlaceholderAPI {
    
    private static final PlaceholderAPI INSTANCE = new PlaceholderAPI();
    
    /**
     * Obtém a instância do PlaceholderAPI
     */
    public static PlaceholderAPI getInstance() {
        return INSTANCE;
    }
    
    private PlaceholderAPI() {
        // Singleton
    }
    
    /**
     * Substitui placeholders em um texto para um jogador específico.
     * 
     * @param playerUUID UUID do jogador
     * @param text Texto com placeholders
     * @return Texto com placeholders substituídos
     */
    public String replacePlaceholders(UUID playerUUID, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        double balance = EconomyAPI.getInstance().getBalance(playerUUID);
        
        // Substitui %balance%
        text = text.replace("%balance%", String.valueOf(balance));
        
        // Substitui %balance_formated%
        text = text.replace("%balance_formated%", CurrencyFormatter.format(balance));
        
        return text;
    }
    
    /**
     * Substitui placeholders em um texto para um jogador específico pelo nome.
     * 
     * @param playerName Nome do jogador
     * @param text Texto com placeholders
     * @return Texto com placeholders substituídos, ou o texto original se o jogador não for encontrado
     */
    public String replacePlaceholders(String playerName, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        UUID playerUUID = EconomyAPI.getInstance().getPlayerUUID(playerName);
        if (playerUUID == null) {
            return text; // Retorna o texto original se o jogador não for encontrado
        }
        
        return replacePlaceholders(playerUUID, text);
    }
    
    /**
     * Registra um placeholder customizado.
     * 
     * @param placeholder Nome do placeholder (ex: "%custom_placeholder%")
     * @param function Função que recebe o UUID do jogador e retorna o valor do placeholder
     */
    public void registerPlaceholder(String placeholder, Function<UUID, String> function) {
        // Por enquanto, apenas suporta os placeholders padrão
        // Pode ser expandido no futuro para suportar placeholders customizados
    }
    
    /**
     * Verifica se um texto contém placeholders suportados.
     * 
     * @param text Texto a verificar
     * @return true se contém placeholders, false caso contrário
     */
    public boolean containsPlaceholders(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return text.contains("%balance%") || text.contains("%balance_formated%");
    }
}

