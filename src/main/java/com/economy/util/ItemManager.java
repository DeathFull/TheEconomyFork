package com.economy.util;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de itens que obtém os itens diretamente da API do Hytale
 * através do evento LoadedAssetsEvent, sem depender de mods de terceiros.
 */
public class ItemManager {
    
    private static final Map<String, Item> ITEMS = new HashMap<>();
    
    /**
     * Registra o listener para carregar itens quando os assets são carregados.
     * Este método deve ser chamado pelo evento LoadedAssetsEvent.
     */
    public static void onItemAssetLoad(LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        ITEMS.clear();
        ITEMS.putAll(event.getAssetMap().getAssetMap());
    }
    
    /**
     * Obtém um Item pelo seu ID.
     * 
     * @param itemId O ID do item
     * @return O Item correspondente, ou null se não encontrado
     */
    public static Item getItem(String itemId) {
        return ITEMS.get(itemId);
    }
    
    /**
     * Obtém a durabilidade máxima de um item.
     * 
     * @param itemId O ID do item
     * @return A durabilidade máxima do item, ou 0.0 se não encontrado ou se o item não tem durabilidade
     */
    public static double getMaxDurability(String itemId) {
        Item item = getItem(itemId);
        if (item == null) {
            return 0.0;
        }
        
        try {
            // Tenta obter a durabilidade máxima usando reflexão
            java.lang.reflect.Method getMaxDurabilityMethod = item.getClass().getMethod("getMaxDurability");
            Object maxDurabilityObj = getMaxDurabilityMethod.invoke(item);
            if (maxDurabilityObj instanceof Number) {
                return ((Number) maxDurabilityObj).doubleValue();
            }
        } catch (Exception e) {
            // Se não encontrar o método, tenta outros nomes possíveis
            try {
                java.lang.reflect.Method getDurabilityMethod = item.getClass().getMethod("getDurability");
                Object durabilityObj = getDurabilityMethod.invoke(item);
                if (durabilityObj instanceof Number) {
                    return ((Number) durabilityObj).doubleValue();
                }
            } catch (Exception e2) {
                // Ignora
            }
        }
        
        return 0.0;
    }
    
    /**
     * Verifica se um item existe no mapa.
     * 
     * @param itemId O ID do item
     * @return true se o item existe, false caso contrário
     */
    public static boolean hasItem(String itemId) {
        return ITEMS.containsKey(itemId);
    }
    
    /**
     * Obtém todos os itens carregados.
     * 
     * @return Uma cópia do mapa de itens
     */
    public static Map<String, Item> getAllItems() {
        return new HashMap<>(ITEMS);
    }
    
    /**
     * Obtém o número de itens carregados.
     * 
     * @return O número de itens
     */
    public static int getItemCount() {
        return ITEMS.size();
    }
    
    /**
     * Obtém o nome do item traduzido como String.
     * Usa o mesmo sistema que as lojas para obter nomes de itens.
     * 
     * @param itemId O ID do item
     * @return O nome traduzido do item, ou o itemId formatado se não encontrado
     */
    public static String getItemName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return itemId;
        }
        
        Item itemConfig = getItem(itemId);
        if (itemConfig != null) {
            try {
                com.hypixel.hytale.server.core.Message itemName = 
                    com.hypixel.hytale.server.core.Message.translation(itemConfig.getTranslationKey());
                String itemNameText = extractTextFromMessage(itemName);
                
                if (itemNameText != null && !itemNameText.isEmpty() && 
                    !itemNameText.startsWith("item.") && !itemNameText.contains("com.hypixel")) {
                    return itemNameText;
                }
            } catch (Exception e) {
                // Continua para fallback
            }
        }
        
        // Fallback: tenta usar o padrão de tradução do Hytale
        try {
            com.hypixel.hytale.server.core.Message itemName = 
                com.hypixel.hytale.server.core.Message.translation("item." + itemId);
            String itemNameText = extractTextFromMessage(itemName);
            
            if (itemNameText != null && !itemNameText.isEmpty() && 
                !itemNameText.startsWith("item.") && !itemNameText.contains("com.hypixel")) {
                return itemNameText;
            }
        } catch (Exception e) {
            // Continua para formatação final
        }
        
        // Fallback final: formata o itemId
        return formatItemId(itemId);
    }
    
    /**
     * Extrai o texto de um objeto Message.
     * 
     * @param message O objeto Message
     * @return O texto extraído, ou null se não conseguir
     */
    private static String extractTextFromMessage(com.hypixel.hytale.server.core.Message message) {
        if (message == null) {
            return null;
        }
        
        try {
            // Tenta getText()
            try {
                java.lang.reflect.Method getTextMethod = message.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(message);
                if (textObj != null) {
                    String text = textObj.toString();
                    if (text != null && !text.isEmpty() && !text.startsWith("item.") && !text.contains("com.hypixel")) {
                        return text;
                    }
                }
            } catch (Exception e) {
                // Continua tentando
            }
            
            // Tenta getString()
            try {
                java.lang.reflect.Method getStringMethod = message.getClass().getMethod("getString");
                Object textObj = getStringMethod.invoke(message);
                if (textObj != null) {
                    String text = textObj.toString();
                    if (text != null && !text.isEmpty() && !text.startsWith("item.") && !text.contains("com.hypixel")) {
                        return text;
                    }
                }
            } catch (Exception e) {
                // Continua tentando
            }
            
            // Tenta toString() mas verifica se não é apenas a classe
            String toString = message.toString();
            if (toString != null && !toString.isEmpty() && 
                !toString.startsWith("item.") && 
                !toString.contains("com.hypixel") &&
                !toString.startsWith("Message@")) {
                return toString;
            }
            
            // Tenta acessar spans internos
            try {
                java.lang.reflect.Method getSpansMethod = message.getClass().getMethod("getSpans");
                Object spansObj = getSpansMethod.invoke(message);
                if (spansObj != null) {
                    if (spansObj instanceof java.util.List) {
                        java.util.List<?> spans = (java.util.List<?>) spansObj;
                        StringBuilder textBuilder = new StringBuilder();
                        for (Object span : spans) {
                            if (span != null) {
                                try {
                                    java.lang.reflect.Method getTextMethod = span.getClass().getMethod("getText");
                                    Object spanText = getTextMethod.invoke(span);
                                    if (spanText != null) {
                                        textBuilder.append(spanText.toString());
                                    }
                                } catch (Exception e) {
                                    // Ignora este span
                                }
                            }
                        }
                        String result = textBuilder.toString();
                        if (result != null && !result.isEmpty() && !result.startsWith("item.") && !result.contains("com.hypixel")) {
                            return result;
                        }
                    }
                }
            } catch (Exception e) {
                // Continua
            }
            
        } catch (Exception e) {
            // Falha ao extrair texto
        }
        
        return null;
    }
    
    /**
     * Formata o itemId para exibição.
     * 
     * @param itemId O ID do item
     * @return O itemId formatado
     */
    private static String formatItemId(String itemId) {
        // Formata o itemId para um nome mais legível
        // Ex: "Tool_Hatchet_Adamantite" -> "Adamantite Hatchet"
        if (itemId == null || itemId.isEmpty()) {
            return itemId;
        }
        
        // Remove prefixos comuns
        String formatted = itemId;
        if (formatted.startsWith("Tool_")) {
            formatted = formatted.substring(5);
        }
        if (formatted.startsWith("Item_")) {
            formatted = formatted.substring(5);
        }
        if (formatted.startsWith("Block_")) {
            formatted = formatted.substring(6);
        }
        
        // Se contém dois-pontos, pega a parte depois do último dois-pontos
        if (formatted.contains(":")) {
            String[] parts = formatted.split(":");
            if (parts.length > 1) {
                formatted = parts[parts.length - 1];
            }
        }
        
        // Substitui underscores por espaços e capitaliza
        String[] words = formatted.split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(words[i].substring(0, 1).toUpperCase());
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
}

