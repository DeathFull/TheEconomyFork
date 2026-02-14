package com.economy.util;

import com.economy.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LanguageManager {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
    private static final Map<String, JsonObject> loadedLanguages = new HashMap<>();
    private static String currentLanguage = "EN";

    public static void initialize() {
        if (Main.CONFIG != null && Main.CONFIG.get() != null) {
            currentLanguage = Main.CONFIG.get().getLanguage();
            logger.at(Level.INFO).log("Language system initialized: " + currentLanguage);
        } else {
            logger.at(Level.WARNING).log("Config not available, using default language: " + currentLanguage);
        }
        loadLanguage(currentLanguage);
    }

    public static void loadLanguage(String lang) {
        // Remove a linguagem anterior do cache se já estiver carregada (para permitir recarregar)
        // Mas mantém se for a mesma linguagem atual
        if (loadedLanguages.containsKey(lang) && lang.equals(currentLanguage)) {
            return; // Já carregado e é a linguagem atual
        }

        try {
            Path langPath = Paths.get(FileUtils.MAIN_PATH, "Language_" + lang + ".json");
            File langFile = langPath.toFile();

            if (!langFile.exists()) {
                // Cria arquivo padrão APENAS se não existir
                createDefaultLanguageFile(langFile, lang);
            } else {
                // Arquivo existe - verifica se precisa migrar (apenas se tiver chaves antigas sem prefixo)
                // NÃO migra se o arquivo já estiver no formato novo ou se houver erro ao ler
                try (FileReader reader = new FileReader(langFile)) {
                    JsonObject existingJson = JsonParser.parseReader(reader).getAsJsonObject();
                    
                    // Verifica se o arquivo está vazio ou inválido
                    if (existingJson == null || existingJson.size() == 0) {
                        // Arquivo vazio ou inválido - cria padrão
                        logger.at(Level.WARNING).log("Language file is empty or invalid, creating default: " + langFile.getName());
                        createDefaultLanguageFile(langFile, lang);
                    } else {
                        // Arquivo existe e tem conteúdo - verifica se precisa migrar
                        boolean hasOldKeys = false;
                        boolean hasNewKeys = false;
                        
                        for (String key : existingJson.keySet()) {
                            if (key.startsWith("chat_") || key.startsWith("gui_") || key.startsWith("hud_") 
                                || key.startsWith("desc_") || key.startsWith("npc_")) {
                                hasNewKeys = true;
                            } else {
                                hasOldKeys = true;
                            }
                        }
                        
                        // Só migra se tiver chaves antigas E não tiver chaves novas
                        // Se já tiver chaves novas, não migra (preserva o arquivo editado)
                        if (hasOldKeys && !hasNewKeys) {
                            logger.at(Level.INFO).log("Language file has old format keys, migrating: " + langFile.getName());
                            migrateLanguageFile(langFile, lang);
                        } else if (hasNewKeys) {
                            // Arquivo já está no formato novo - não faz nada, preserva edições
                            logger.at(Level.FINE).log("Language file already in new format, preserving: " + langFile.getName());
                        }
                    }
                } catch (Exception e) {
                    // Se houver erro ao ler, NÃO sobrescreve - apenas loga o erro
                    // O arquivo pode estar corrompido, mas não vamos sobrescrever sem confirmação
                    logger.at(Level.SEVERE).log("Error reading language file " + langFile.getName() + ": " + e.getMessage());
                    logger.at(Level.SEVERE).log("Language file will NOT be overwritten. Please check the file manually.");
                    // NÃO chama migrateLanguageFile aqui para não sobrescrever
                }
            }

            try (FileReader reader = new FileReader(langFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loadedLanguages.put(lang, json);
                logger.at(Level.INFO).log("Language loaded: " + lang);
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error loading language " + lang + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createDefaultLanguageFile(File file, String lang) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        String jsonContent = null;
        
        if ("PT".equals(lang)) {
            jsonContent = getDefaultPTJson();
        } else if ("EN".equals(lang)) {
            jsonContent = getDefaultENJson();
        } else if ("ES".equals(lang)) {
            jsonContent = getDefaultESJson();
        } else if ("RU".equals(lang)) {
            jsonContent = getDefaultRUJson();
        } else if ("PL".equals(lang)) {
            jsonContent = getDefaultPLJson();
        } else if ("DE".equals(lang)) {
            jsonContent = getDefaultDEJson();
        } else if ("HU".equals(lang)) {
            jsonContent = getDefaultHUJson();
        } else if ("FR".equals(lang)) {
            jsonContent = getDefaultFRJson();
        }
        
        if (jsonContent != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonContent);
            }
        }
    }

    /**
     * Migra arquivos de linguagem antigos para o novo formato organizado (chat_, gui_, hud_)
     */
    private static void migrateLanguageFile(File file, String lang) {
        try {
            // Lê o arquivo existente
            JsonObject oldJson;
            try (FileReader reader = new FileReader(file)) {
                oldJson = JsonParser.parseReader(reader).getAsJsonObject();
            }

            // Verifica se já está migrado (tem pelo menos uma chave com prefixo)
            boolean needsMigration = false;
            boolean hasNewKeys = false;
            
            for (String key : oldJson.keySet()) {
                if (key.startsWith("chat_") || key.startsWith("gui_") || key.startsWith("hud_") 
                    || key.startsWith("desc_") || key.startsWith("npc_")) {
                    hasNewKeys = true;
                } else {
                    needsMigration = true;
                }
            }

            // Se já tem chaves novas, não migra (preserva o arquivo)
            if (hasNewKeys || !needsMigration) {
                // Já está migrado ou não precisa migrar, não faz nada
                logger.at(Level.FINE).log("Language file already migrated or in correct format, preserving: " + file.getName());
                return;
            }

            logger.at(Level.INFO).log("Migrating language file: " + file.getName());

            // Faz backup do arquivo antigo
            File backupFile = new File(file.getParentFile(), file.getName() + ".backup");
            try {
                Files.copy(file.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.at(Level.INFO).log("Backup created: " + backupFile.getName());
            } catch (Exception e) {
                logger.at(Level.WARNING).log("Could not create backup: " + e.getMessage());
            }

            // Carrega o JSON padrão com as novas chaves
            String defaultJsonContent = null;
            if ("PT".equals(lang)) {
                defaultJsonContent = getDefaultPTJson();
            } else if ("EN".equals(lang)) {
                defaultJsonContent = getDefaultENJson();
            } else if ("ES".equals(lang)) {
                defaultJsonContent = getDefaultESJson();
            } else if ("RU".equals(lang)) {
                defaultJsonContent = getDefaultRUJson();
            } else if ("PL".equals(lang)) {
                defaultJsonContent = getDefaultPLJson();
            } else if ("DE".equals(lang)) {
                defaultJsonContent = getDefaultDEJson();
            } else if ("HU".equals(lang)) {
                defaultJsonContent = getDefaultHUJson();
            } else if ("FR".equals(lang)) {
                defaultJsonContent = getDefaultFRJson();
            }

            if (defaultJsonContent == null) {
                return;
            }

            // Parse do JSON padrão
            JsonObject newJson = JsonParser.parseString(defaultJsonContent).getAsJsonObject();

            // Mapeamento de chaves antigas para novas (sem prefixo -> com prefixo)
            Map<String, String> keyMapping = createKeyMapping();

            // Migra valores das chaves antigas para as novas
            for (Map.Entry<String, String> entry : keyMapping.entrySet()) {
                String oldKey = entry.getKey();
                String newKey = entry.getValue();
                
                if (oldJson.has(oldKey) && !oldJson.has(newKey)) {
                    // Se a chave antiga existe e a nova não existe, migra o valor
                    newJson.addProperty(newKey, oldJson.get(oldKey).getAsString());
                    logger.at(Level.FINE).log("Migrated key: " + oldKey + " -> " + newKey);
                }
            }

            // Preserva quaisquer chaves customizadas que já tenham prefixo
            for (String key : oldJson.keySet()) {
                if ((key.startsWith("chat_") || key.startsWith("gui_") || key.startsWith("hud_")) 
                    && !newJson.has(key)) {
                    // Preserva chaves customizadas com prefixo que não estão no padrão
                    newJson.add(key, oldJson.get(key));
                }
            }

            // Salva o arquivo migrado
            try (FileWriter writer = new FileWriter(file)) {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(newJson));
            }

            logger.at(Level.INFO).log("Language file migrated successfully: " + file.getName());

        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error migrating language file " + file.getName() + ": " + e.getMessage());
            // Não lança exceção para não quebrar o carregamento
        }
    }

    /**
     * Cria o mapeamento de chaves antigas para novas chaves organizadas
     */
    private static Map<String, String> createKeyMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // Chat messages
        mapping.put("insufficient_balance", "chat_insufficient_balance");
        mapping.put("player_not_found", "chat_player_not_found");
        mapping.put("invalid_amount", "chat_invalid_amount");
        mapping.put("cannot_pay_yourself", "chat_cannot_pay_yourself");
        mapping.put("payment_sent", "chat_payment_sent");
        mapping.put("payment_received", "chat_payment_received");
        mapping.put("balance_set", "chat_balance_set");
        mapping.put("balance_added", "chat_balance_added");
        mapping.put("usage_money", "chat_usage_money");
        mapping.put("usage_pay", "chat_usage_pay");
        mapping.put("usage_set", "chat_usage_set");
        mapping.put("usage_give", "chat_usage_give");
        mapping.put("no_permission", "chat_no_permission");
        mapping.put("balance_of", "chat_balance_of");
        mapping.put("top_10_richest", "chat_top_10_richest");
        mapping.put("top_separator", "chat_top_separator");
        mapping.put("top_no_players", "chat_top_no_players");
        mapping.put("top_position", "chat_top_position");
        mapping.put("money_top_disabled", "chat_money_top_disabled");
        mapping.put("balance_set_to", "chat_balance_set_to");
        mapping.put("balance_added_to", "chat_balance_added_to");
        mapping.put("money_received", "chat_money_received");
        mapping.put("money_received_from", "chat_money_received_from");
        mapping.put("cash_of", "chat_cash_of");
        mapping.put("cash_added", "chat_cash_added");
        mapping.put("cash_added_to", "chat_cash_added_to");
        mapping.put("cash_received", "chat_cash_received");
        mapping.put("usage_cash_give", "chat_usage_cash_give");
        mapping.put("balance_set_notification", "chat_balance_set_notification");
        mapping.put("plugin_loaded", "chat_plugin_loaded");
        mapping.put("ore_rewards", "chat_ore_rewards");
        mapping.put("wood_rewards", "chat_wood_rewards");
        mapping.put("ores_configured", "chat_ores_configured");
        mapping.put("api_available", "chat_api_available");
        mapping.put("loading_balances", "chat_loading_balances");
        mapping.put("error_loading_balances", "chat_error_loading_balances");
        mapping.put("error_saving_balances", "chat_error_saving_balances");
        mapping.put("balances_saved", "chat_balances_saved");
        mapping.put("initial_balance_given", "chat_initial_balance_given");
        mapping.put("enabled", "chat_enabled");
        mapping.put("disabled", "chat_disabled");
        mapping.put("shop_item_added", "chat_shop_item_added");
        mapping.put("shop_item_removed", "chat_shop_item_removed");
        mapping.put("shop_item_updated", "chat_shop_item_updated");
        mapping.put("shop_item_not_found", "chat_shop_item_not_found");
        mapping.put("error_item_update", "chat_error_item_update");
        mapping.put("shop_insufficient_cash", "chat_shop_insufficient_cash");
        mapping.put("shop_cash_added", "chat_shop_cash_added");
        mapping.put("shop_item_bought", "chat_shop_item_bought");
        mapping.put("shop_item_sold", "chat_shop_item_sold");
        mapping.put("shop_insufficient_balance", "chat_shop_insufficient_balance");
        mapping.put("shop_insufficient_items", "chat_shop_insufficient_items");
        mapping.put("shop_tab_created", "chat_shop_tab_created");
        mapping.put("shop_tab_removed", "chat_shop_tab_removed");
        mapping.put("shop_tab_not_found", "chat_shop_tab_not_found");
        mapping.put("shop_tab_already_exists", "chat_shop_tab_already_exists");
        mapping.put("shop_tab_limit_reached", "chat_shop_tab_limit_reached");
        mapping.put("shop_tab_name_empty", "chat_shop_tab_name_empty");
        mapping.put("error_tab_create", "chat_error_tab_create");
        mapping.put("error_tab_remove", "chat_error_tab_remove");
        mapping.put("playershop_disabled", "chat_playershop_disabled");
        mapping.put("playershop_not_open", "chat_playershop_not_open");
        mapping.put("playershop_empty", "chat_playershop_empty");
        mapping.put("myshop_opened", "chat_myshop_opened");
        mapping.put("myshop_closed", "chat_myshop_closed");
        mapping.put("myshop_icon_set", "chat_myshop_icon_set");
        mapping.put("myshop_icon_set_error", "chat_myshop_icon_set_error");
        mapping.put("myshop_status_changed", "chat_myshop_status_changed");
        mapping.put("myshop_usage", "chat_myshop_usage");
        mapping.put("myshop_usage_add", "chat_myshop_usage_add");
        mapping.put("myshop_usage_remove", "chat_myshop_usage_remove");
        mapping.put("myshop_usage_rename", "chat_myshop_usage_rename");
        mapping.put("myshop_usage_tab", "chat_myshop_usage_tab");
        mapping.put("myshop_tab_has_items", "chat_myshop_tab_has_items");
        mapping.put("myshop_renamed", "chat_myshop_renamed");
        mapping.put("myshop_rename_error", "chat_myshop_rename_error");
        mapping.put("myshop_item_added", "chat_myshop_item_added");
        mapping.put("myshop_item_removed", "chat_myshop_item_removed");
        mapping.put("myshop_error_remove_item", "chat_myshop_error_remove_item");
        mapping.put("myshop_error_inventory_full", "chat_myshop_error_inventory_full");
        mapping.put("myshop_not_owner", "chat_myshop_not_owner");
        mapping.put("playershop_confirm_buy", "gui_playershop_confirm_buy");
        mapping.put("playershop_item_bought", "chat_playershop_item_bought");
        mapping.put("playershop_insufficient_stock", "chat_playershop_insufficient_stock");
        mapping.put("playershop_cannot_buy_from_self", "chat_playershop_cannot_buy_from_self");
        mapping.put("shops_empty", "gui_shops_empty");
        mapping.put("shops_player_shop", "gui_shops_player_shop");
        mapping.put("shops_items_count", "gui_shops_items_count");
        mapping.put("shop_empty", "gui_shop_empty");
        mapping.put("error_inventory_add", "chat_error_inventory_add");
        mapping.put("error_inventory_remove", "chat_error_inventory_remove");
        mapping.put("error_item_info", "chat_error_item_info");
        mapping.put("error_item_add", "chat_error_item_add");
        mapping.put("error_item_remove", "chat_error_item_remove");
        mapping.put("iteminfo_error_inventory", "chat_iteminfo_error_inventory");
        mapping.put("iteminfo_no_item", "chat_iteminfo_no_item");
        mapping.put("iteminfo_invalid_id", "chat_iteminfo_invalid_id");
        mapping.put("iteminfo_info", "chat_iteminfo_info");
        mapping.put("item_not_found", "chat_item_not_found");
        mapping.put("playershop_owner_sold", "chat_playershop_owner_sold");
        mapping.put("playershop_owner_bought", "chat_playershop_owner_bought");
        mapping.put("hud_usage", "chat_hud_usage");
        mapping.put("hud_enabled", "chat_hud_enabled");
        mapping.put("hud_disabled", "chat_hud_disabled");
        mapping.put("hud_invalid_action", "chat_hud_invalid_action");
        mapping.put("hud_server_disabled", "chat_hud_server_disabled");
        mapping.put("hud_error", "chat_hud_error");
        mapping.put("npc_moved", "chat_npc_moved");
        mapping.put("npc_movehere_usage", "chat_npc_movehere_usage");
        mapping.put("desc_shop_npc_movehere", "desc_shop_npc_movehere");
        
        // GUI messages
        mapping.put("shop_confirm_buy", "gui_shop_confirm_buy");
        mapping.put("shop_confirm_sell", "gui_shop_confirm_sell");
        mapping.put("shop_quantity_label", "gui_shop_quantity_label");
        mapping.put("shop_button_confirm", "gui_shop_button_confirm");
        mapping.put("shop_button_cancel", "gui_shop_button_cancel");
        mapping.put("shop_button_back", "gui_shop_button_back");
        mapping.put("shop_title", "gui_shop_title");
        mapping.put("shop_tooltip_unique_id", "gui_shop_tooltip_unique_id");
        mapping.put("shop_tooltip_quantity", "gui_shop_tooltip_quantity");
        mapping.put("shop_tooltip_buy", "gui_shop_tooltip_buy");
        mapping.put("shop_tooltip_sell", "gui_shop_tooltip_sell");
        mapping.put("shop_tooltip_left_click_buy", "gui_shop_tooltip_left_click_buy");
        mapping.put("shop_tooltip_right_click_sell", "gui_shop_tooltip_right_click_sell");
        mapping.put("shop_tooltip_right_click_buy", "gui_shop_tooltip_right_click_buy");
        mapping.put("shop_tooltip_left_click_sell", "gui_shop_tooltip_left_click_sell");
        mapping.put("shop_tooltip_middle_click_remove", "gui_shop_tooltip_middle_click_remove");
        mapping.put("shop_tooltip_buy_disabled", "gui_shop_tooltip_buy_disabled");
        mapping.put("shop_tooltip_sell_disabled", "gui_shop_tooltip_sell_disabled");
        mapping.put("shop_tooltip_not_available", "gui_shop_tooltip_not_available");
        mapping.put("shop_manager_title", "gui_shop_manager_title");
        mapping.put("shop_manager_add_item", "gui_shop_manager_add_item");
        mapping.put("shop_manager_add_console", "gui_shop_manager_add_console");
        mapping.put("shop_manager_add_cash", "gui_shop_manager_add_cash");
        mapping.put("shop_manager_add_tab", "gui_shop_manager_add_tab");
        mapping.put("shop_manager_payment_type", "gui_shop_manager_payment_type");
        mapping.put("shop_manager_payment_money", "gui_shop_manager_payment_money");
        mapping.put("shop_manager_payment_cash", "gui_shop_manager_payment_cash");
        mapping.put("shop_manager_price_buy_cash", "gui_shop_manager_price_buy_cash");
        mapping.put("shop_manager_remove_item_hint", "gui_shop_manager_remove_item_hint");
        mapping.put("shop_manager_edit_item_hint", "gui_shop_manager_edit_item_hint");
        mapping.put("shop_manager_add_item_title", "gui_shop_manager_add_item_title");
        mapping.put("shop_manager_add_item_title_simple", "gui_shop_manager_add_item_title_simple");
        mapping.put("shop_manager_edit_item_title", "gui_shop_manager_edit_item_title");
        mapping.put("shop_manager_edit_item_title_simple", "gui_shop_manager_edit_item_title_simple");
        mapping.put("shop_manager_item_id", "gui_shop_manager_item_id");
        mapping.put("shop_manager_item_id_empty", "chat_shop_manager_item_id_empty");
        mapping.put("shop_manager_quantity", "gui_shop_manager_quantity");
        mapping.put("shop_manager_price_buy", "gui_shop_manager_price_buy");
        mapping.put("shop_manager_price_sell", "gui_shop_manager_price_sell");
        mapping.put("shop_manager_hotbar_items", "gui_shop_manager_hotbar_items");
        mapping.put("shop_manager_add_tab_title", "gui_shop_manager_add_tab_title");
        mapping.put("shop_manager_tab_name", "gui_shop_manager_tab_name");
        mapping.put("shop_manager_confirm_remove_tab", "gui_shop_manager_confirm_remove_tab");
        mapping.put("myshop_manager_title", "gui_myshop_manager_title");
        mapping.put("myshop_rename_shop", "gui_myshop_rename_shop");
        mapping.put("myshop_rename_shop_title", "gui_myshop_rename_shop_title");
        mapping.put("myshop_rename_shop_label", "gui_myshop_rename_shop_label");
        mapping.put("myshop_open_shop", "gui_myshop_open_shop");
        mapping.put("myshop_close_shop", "gui_myshop_close_shop");
        mapping.put("myshop_set_icon", "gui_myshop_set_icon");
        mapping.put("myshop_confirm_remove", "gui_myshop_confirm_remove");
        mapping.put("playershop_tooltip_stock", "gui_playershop_tooltip_stock");
        mapping.put("playershop_tooltip_durability", "gui_playershop_tooltip_durability");
        mapping.put("shops_tooltip_items", "gui_shops_tooltip_items");
        mapping.put("shops_tooltip_click", "gui_shops_tooltip_click");
        mapping.put("shop_title_admin", "gui_shop_title_admin");
        mapping.put("shops_title_player", "gui_shops_title_player");
        mapping.put("playershop_title", "gui_playershop_title");
        mapping.put("shop_price_buy_label", "gui_shop_price_buy_label");
        mapping.put("shop_price_sell_label", "gui_shop_price_sell_label");
        mapping.put("shop_price_separator", "gui_shop_price_separator");
        mapping.put("playershop_stock_label", "gui_playershop_stock_label");
        mapping.put("playershop_tooltip_click_remove", "gui_playershop_tooltip_click_remove");
        
        // HUD messages
        mapping.put("hud_nick", "hud_nick");
        mapping.put("hud_money", "hud_money");
        mapping.put("hud_cash", "hud_cash");
        mapping.put("hud_top_rank", "hud_top_rank");
        mapping.put("hud_rank", "hud_rank");
        mapping.put("hud_shop_status", "hud_shop_status");
        mapping.put("hud_shop_open", "hud_shop_open");
        mapping.put("hud_shop_closed", "hud_shop_closed");
        mapping.put("hud_rank_unknown", "hud_rank_unknown");
        mapping.put("hud_gain", "hud_gain");
        
        return mapping;
    }

    private static String getDefaultPTJson() {
        return "{\n" +
                // ========== CHAT (Mensagens no Chat) ==========
                "  \"chat_insufficient_balance\": \"Você não tem saldo suficiente!\",\n" +
                "  \"chat_player_not_found\": \"Jogador não encontrado!\",\n" +
                "  \"chat_invalid_amount\": \"Valor inválido!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"Você não pode pagar a si mesmo!\",\n" +
                "  \"chat_payment_sent\": \"Pagamento enviado com sucesso!\",\n" +
                "  \"chat_payment_received\": \"Você recebeu um pagamento!\",\n" +
                "  \"chat_balance_set\": \"Saldo definido com sucesso!\",\n" +
                "  \"chat_balance_added\": \"Saldo adicionado com sucesso!\",\n" +
                "  \"chat_usage_money\": \"Uso: /money [nick]\",\n" +
                "  \"chat_usage_pay\": \"Uso: /money pay <nick> <valor>\",\n" +
                "  \"chat_usage_set\": \"Uso: /money set <nick> <valor>\",\n" +
                "  \"chat_usage_give\": \"Uso: /money give <nick> <valor>\",\n" +
                "  \"chat_no_permission\": \"Você não tem permissão para usar este comando!\",\n" +
                "  \"chat_balance_of\": \"Saldo de {player}\",\n" +
                "  \"chat_top_10_richest\": \"Top 10 Mais Ricos\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"Nenhum jogador encontrado no ranking.\",\n" +
                "  \"chat_money_top_disabled\": \"O ranking de dinheiro está desabilitado.\",\n" +
                "  \"chat_top_position\": \"{position}º {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Saldo de {player} definido para {amount}\",\n" +
                "  \"chat_balance_added_to\": \"Adicionado {amount} a {player}. Novo saldo: {balance}\",\n" +
                "  \"chat_money_received\": \"Você recebeu {amount}.\",\n" +
                "  \"chat_money_received_from\": \"Você recebeu {amount} de {player}.\",\n" +
                "  \"chat_cash_of\": \"Cash de {player}\",\n" +
                "  \"chat_cash_added\": \"Cash adicionado com sucesso!\",\n" +
                "  \"chat_cash_added_to\": \"Adicionado {amount} de cash a {player}. Novo cash: {cash}\",\n" +
                "  \"chat_cash_received\": \"Você recebeu {amount} de cash.\",\n" +
                "  \"chat_usage_cash_give\": \"Uso: /cash give <nick> <valor>\",\n" +
                "  \"chat_balance_set_notification\": \"Seu saldo foi definido para {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"EconomySystem carregado com sucesso!\",\n" +
                "  \"chat_ore_rewards\": \"Recompensas por minérios: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Recompensas por madeiras: {status}\",\n" +
                "  \"chat_ores_configured\": \"Minérios configurados: {count} itens\",\n" +
                "  \"chat_api_available\": \"API pública disponível: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Carregando dados de saldo...\",\n" +
                "  \"chat_error_loading_balances\": \"ERRO AO CARREGAR ARQUIVO DE SALDOS\",\n" +
                "  \"chat_error_saving_balances\": \"ERRO AO SALVAR ARQUIVO DE SALDOS\",\n" +
                "  \"chat_balances_saved\": \"Salvos dados de economia\",\n" +
                "  \"chat_initial_balance_given\": \"Saldo inicial de ${amount} dado ao jogador: {player}\",\n" +
                "  \"chat_enabled\": \"Ativado\",\n" +
                "  \"chat_disabled\": \"Desativado\",\n" +
                "  \"chat_shop_item_added\": \"Item adicionado à loja! ID Único: {uniqueid}, Item: {itemid}, Quantidade: {quantity}, Preço de Venda: {pricesell}, Preço de Compra: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Item removido da loja! ID Único: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Preços do item atualizados! ID Único: {uniqueid}, Item: {item}, Preço de Compra: {pricebuy}, Preço de Venda: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Item não encontrado! ID Único: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Erro ao atualizar item: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Argumentos inválidos!\",\n" +
                "  \"chat_shop_item_bought\": \"Você comprou {quantity}x {item} por {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"Você comprou {added}x de {requested}x {item} (inventário cheio)!\",\n" +
                "  \"chat_shop_item_sold\": \"Você vendeu {quantity}x {item} por {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"Você não tem saldo suficiente! Necessário: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"Você não tem itens suficientes! Necessário: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Item não encontrado! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"Tab '{tab}' criada com sucesso!\",\n" +
                "  \"chat_shop_tab_removed\": \"Tab '{tab}' removida com sucesso!\",\n" +
                "  \"chat_shop_tab_not_found\": \"Tab '{tab}' não encontrada!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"Tab '{tab}' já existe!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Limite de 7 tabs atingido!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"O nome da tab não pode estar vazio!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Erro ao criar a tab!\",\n" +
                "  \"chat_error_tab_create\": \"Erro ao criar tab: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Erro ao remover tab: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Erro ao remover a tab!\",\n" +
                "  \"chat_playershop_disabled\": \"Lojas de jogadores estão desabilitadas.\",\n" +
                "  \"chat_playershop_not_open\": \"A loja deste jogador está fechada.\",\n" +
                "  \"chat_playershop_empty\": \"Esta loja está vazia.\",\n" +
                "  \"chat_myshop_opened\": \"Sua loja foi aberta!\",\n" +
                "  \"chat_myshop_closed\": \"Sua loja foi fechada!\",\n" +
                "  \"chat_myshop_icon_set\": \"Ícone da loja definido para: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Erro ao definir o ícone da loja.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Uso: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Uso: /myshop add <tab> <preço_compra> <preço_venda>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Uso: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Uso: /myshop rename <nome>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Uso: /myshop tab <create|remove> <nome>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Não é possível remover a tab '{tab}' pois ela contém itens. Remova os itens primeiro.\",\n" +
                "  \"chat_myshop_renamed\": \"Loja renomeada para: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Erro ao renomear a loja.\",\n" +
                "  \"chat_myshop_item_added\": \"Item adicionado à sua loja! ID Único: {uniqueid}, Item: {item}, Quantidade: {quantity}, Preço: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"Item removido da sua loja! ID Único: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Erro ao remover item do inventário.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Inventário cheio! Item não foi devolvido.\",\n" +
                "  \"chat_myshop_not_owner\": \"Este item não pertence à sua loja.\",\n" +
                "  \"chat_playershop_item_bought\": \"Você comprou {quantity}x {item} por {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"Você comprou {added}x de {requested}x {item} (inventário cheio)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"Estoque insuficiente!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"Você não pode comprar da sua própria loja!\",\n" +
                "  \"chat_shops_empty\": \"Nenhuma loja aberta no momento.\",\n" +
                "  \"chat_error_inventory_add\": \"Erro ao adicionar item ao inventário. Dinheiro devolvido.\",\n" +
                "  \"chat_error_inventory_remove\": \"Erro ao remover item do inventário.\",\n" +
                "  \"chat_error_item_info\": \"Erro ao obter informações do item: {error}\",\n" +
                "  \"chat_error_item_add\": \"Erro ao adicionar item: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Erro ao remover item: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Você vendeu {quantity}x {item} para {player} por {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"Você comprou {quantity}x {item} de {player} por {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Erro ao acessar inventário.\",\n" +
                "  \"chat_iteminfo_no_item\": \"Você não está segurando nenhum item.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"Item não possui ID válido.\",\n" +
                "  \"chat_iteminfo_info\": \"Item ID: {itemid} | Nome: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"O ID do item não pode estar vazio!\",\n" +
                "  \"chat_hud_usage\": \"Uso: /hud on ou /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD ativada!\",\n" +
                "  \"chat_hud_disabled\": \"HUD desativada!\",\n" +
                "  \"chat_hud_invalid_action\": \"Ação inválida! Use 'on' ou 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"A HUD está desabilitada no servidor.\",\n" +
                "  \"chat_hud_error\": \"Erro ao alterar preferência da HUD.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cErro ao obter posição do jogador.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cErro ao obter UUID do mundo.\",\n" +
                "  \"chat_npc_created\": \"&aNPC da loja criado na sua posição! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aNPC da loja criado na sua posição! ID: {npcId} | Nome: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cNenhum NPC encontrado.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== NPCs da Loja ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7Shop ID: &f{shopId} &7| NPC ID: &f{npcId} &7| Mundo: &f{worldId} &7| Pos: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cUso: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cUse /shop npc list para ver os Shop IDs dos NPCs\",\n" +
                "  \"chat_npc_removed\": \"&aNPC removido com sucesso!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC não encontrado!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cUUID inválido!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cShop ID inválido! Use um número.\",\n" +
                "  \"chat_npc_all_removed\": \"&aTodos os NPCs foram removidos!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC movido com sucesso para a sua posição!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cUso: /shop npc movehere <shopId>\",\n" +
                // ========== Descrições de Comandos ==========
                "  \"desc_shop\": \"Abre a loja\",\n" +
                "  \"desc_shop_add\": \"Adiciona um item à loja\",\n" +
                "  \"desc_shop_remove\": \"Remove um item da loja\",\n" +
                "  \"desc_shop_tab\": \"Gerencia tabs da loja\",\n" +
                "  \"desc_shop_tab_create\": \"Cria uma nova tab na loja\",\n" +
                "  \"desc_shop_tab_remove\": \"Remove uma tab da loja e todos os seus itens\",\n" +
                "  \"desc_shop_manager\": \"Abre a interface de gerenciamento da loja\",\n" +
                "  \"desc_shop_npc\": \"Gerencia o NPC da loja\",\n" +
                "  \"desc_shop_npc_add\": \"Cria o NPC da loja na sua posição\",\n" +
                "  \"desc_shop_npc_remove\": \"Remove um NPC da loja pelo Shop ID\",\n" +
                "  \"desc_shop_npc_list\": \"Lista todos os NPCs da loja\",\n" +
                "  \"desc_shop_npc_removeall\": \"Remove todos os NPCs da loja\",\n" +
                "  \"desc_shop_npc_movehere\": \"Move o NPC da loja para sua posição\",\n" +
                "  \"desc_shop_renameplayershop\": \"Renomeia a loja de um jogador\",\n" +
                "  \"desc_money\": \"Sistema de economia\",\n" +
                "  \"desc_money_pay\": \"Transfere dinheiro para outro jogador\",\n" +
                "  \"desc_money_top\": \"Mostra o top 10 jogadores mais ricos\",\n" +
                "  \"desc_money_set\": \"Define o saldo de um jogador\",\n" +
                "  \"desc_money_give\": \"Adiciona saldo a um jogador\",\n" +
                "  \"desc_cash\": \"Sistema de cash\",\n" +
                "  \"desc_cash_give\": \"Adiciona cash a um jogador\",\n" +
                "  \"desc_shops\": \"Lista todas as lojas abertas\",\n" +
                "  \"desc_playershop\": \"Abre a loja de um jogador\",\n" +
                "  \"desc_myshop\": \"Gerencia sua loja pessoal\",\n" +
                "  \"desc_myshop_open\": \"Abre sua loja pessoal\",\n" +
                "  \"desc_myshop_close\": \"Fecha sua loja pessoal\",\n" +
                "  \"desc_myshop_add\": \"Adiciona um item à sua loja\",\n" +
                "  \"desc_myshop_remove\": \"Remove um item da sua loja\",\n" +
                "  \"desc_myshop_rename\": \"Renomeia sua loja\",\n" +
                "  \"desc_myshop_tab\": \"Gerencia as tabs da sua loja\",\n" +
                "  \"desc_myshop_tab_create\": \"Cria uma nova tab na sua loja\",\n" +
                "  \"desc_myshop_tab_remove\": \"Remove uma tab da sua loja e todos os seus itens\",\n" +
                "  \"desc_myshop_manager\": \"Gerencia sua loja pessoal\",\n" +
                "  \"desc_iteminfo\": \"Mostra informações do item na mão\",\n" +
                "  \"desc_hud\": \"Ativa/desativa a HUD do EconomySystem\",\n" +
                "  \"desc_hud_on\": \"Ativa a HUD\",\n" +
                "  \"desc_hud_off\": \"Desativa a HUD\",\n" +
                // ========== GUI (Textos das Janelas) ==========
                "  \"gui_shop_confirm_buy\": \"Deseja comprar {quantity}x {item} por {price}?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Deseja vender {quantity}x {item} por {price}?\",\n" +
                "  \"gui_shop_quantity_label\": \"Quantidade:\",\n" +
                "  \"gui_shop_button_confirm\": \"Confirmar\",\n" +
                "  \"gui_shop_button_cancel\": \"Cancelar\",\n" +
                "  \"gui_shop_button_back\": \"Voltar\",\n" +
                "  \"gui_shop_title\": \"Loja Admin - The Economy\",\n" +
                "  \"gui_shop_npc_add_title\": \"Adicionar NPC da Loja\",\n" +
                "  \"gui_shop_npc_add_name\": \"Nome do NPC:\",\n" +
                "  \"gui_shop_npc_add_model\": \"Modelo do NPC:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"ID Único\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Quantidade\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Comprar\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Vender\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Botão Esquerdo: Comprar\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Botão Direito: Vender\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Botão Direito: Comprar\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Botão Esquerdo: Vender\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Clique com o botão do meio para Remover\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Compra desabilitada\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Venda desabilitada\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Item não disponível para compra ou venda\",\n" +
                "  \"gui_shop_manager_title\": \"Gerenciar Loja\",\n" +
                "  \"gui_shop_manager_add_item\": \"Adicionar Item\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Adicionar Tab\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Clique para remover o item\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Clique direito para editar preço\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Adicionar Item: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Adicionar Item\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Editar Preço: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Editar Preço do Item\",\n" +
                "  \"gui_shop_manager_item_id\": \"ID do Item:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"O ID do item não pode estar vazio!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Quantidade:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Preço de Compra:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Preço de Venda:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Itens da Hotbar\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Adicionar Nova Tab\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Nome da Tab:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Deseja remover a tab '{tab}'? Todos os itens serão removidos.\",\n" +
                "  \"gui_myshop_manager_title\": \"Gerenciar Minha Loja\",\n" +
                "  \"gui_myshop_rename_shop\": \"Renomear Loja\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Renomear Loja\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Nome da Loja:\",\n" +
                "  \"gui_myshop_open_shop\": \"Abrir Loja\",\n" +
                "  \"gui_myshop_close_shop\": \"Fechar Loja\",\n" +
                "  \"gui_myshop_set_icon\": \"Escolher Ícone\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Deseja remover o item {item} (ID: {uniqueid}) da sua loja?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Estoque\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Deseja comprar {quantity}x {item} por {price}?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Estoque\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Durabilidade\",\n" +
                "  \"gui_shops_empty\": \"Nenhuma loja aberta no momento.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Itens\",\n" +
                "  \"gui_shops_tooltip_click\": \"Clique para abrir a loja\",\n" +
                "  \"gui_shops_player_shop\": \"Loja de {player}\",\n" +
                "  \"gui_shops_items_count\": \"itens\",\n" +
                "  \"gui_shop_empty\": \"Nenhum item na loja\",\n" +
                "  \"gui_shops_title\": \"Você está vendo os shops dos jogadores.\",\n" +
                "  \"gui_shops_title_player\": \"Lojas dos Jogadores\",\n" +
                "  \"gui_playershop_title\": \"Esta loja pertence ao jogador {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"B: \",\n" +
                "  \"gui_shop_price_sell_label\": \"S: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Estoque: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Clique para remover o item\",\n" +
                "  \"gui_shop_manager_add_console\": \"Adicionar Console\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Adicionar Comando Console\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Adicionar Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Adicionar Item Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Adicionar Item Cash\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Tipo de Pagamento:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Dinheiro\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Preço (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Deseja comprar {quantity}x {item} por {price} Cash?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"Você não tem Cash suficiente! Necessário: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Item Cash adicionado à loja! ID Único: {uniqueid}, Item: {item}, Quantidade: {quantity}, Preço: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Nome:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Comando:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Ícone (Item da Hotbar)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Comando Console\",\n" +
                "  \"chat_shop_console_added\": \"Comando console adicionado! Nome: {name}, Comando: {command}, Preço: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"Você comprou {quantity}x {item} por {price}!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"O nome não pode estar vazio!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"O comando não pode estar vazio!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Selecione um ícone da hotbar!\",\n" +
                "  \"chat_error_console_add\": \"Erro ao adicionar comando console: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Uso: /shop renameplayershop <nick> <nome>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Loja renomeada com sucesso! Jogador: {player}, Novo nome: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Erro ao renomear a loja do jogador.\",\n" +
                "  \"npc_interaction_open_shop\": \"Pressione F para abrir a loja\",\n" +
                // ========== HUD (Textos da HUD) ==========
                "  \"hud_nick\": \"&l&6Nick&r:\",\n" +
                "  \"hud_money\": \"&l&6Money&r:\",\n" +
                "  \"hud_cash\": \"&l&6Cash&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Top Rank&r:\",\n" +
                "  \"hud_rank\": \"Rank\",\n" +
                "  \"hud_shop_status\": \"&l&6Loja\",\n" +
                "  \"hud_shop_open\": \"&aAberta\",\n" +
                "  \"hud_shop_closed\": \"&cFechada\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aVocê ganhou &l{amount}\"\n" +
                "}";
    }

    private static String getDefaultENJson() {
        return "{\n" +
                // ========== CHAT (Chat Messages) ==========
                "  \"chat_insufficient_balance\": \"You don't have enough balance!\",\n" +
                "  \"chat_player_not_found\": \"Player not found!\",\n" +
                "  \"chat_invalid_amount\": \"Invalid amount!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"You cannot pay yourself!\",\n" +
                "  \"chat_payment_sent\": \"Payment sent successfully!\",\n" +
                "  \"chat_payment_received\": \"You received a payment!\",\n" +
                "  \"chat_balance_set\": \"Balance set successfully!\",\n" +
                "  \"chat_balance_added\": \"Balance added successfully!\",\n" +
                "  \"chat_usage_money\": \"Usage: /money [nick]\",\n" +
                "  \"chat_usage_pay\": \"Usage: /money pay <nick> <amount>\",\n" +
                "  \"chat_usage_set\": \"Usage: /money set <nick> <amount>\",\n" +
                "  \"chat_usage_give\": \"Usage: /money give <nick> <amount>\",\n" +
                "  \"chat_no_permission\": \"You don't have permission to use this command!\",\n" +
                "  \"chat_balance_of\": \"Balance of {player}\",\n" +
                "  \"chat_top_10_richest\": \"Top 10 Richest\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"No players found in ranking.\",\n" +
                "  \"chat_money_top_disabled\": \"Money top ranking is disabled.\",\n" +
                "  \"chat_top_position\": \"{position} {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Balance of {player} set to {amount}\",\n" +
                "  \"chat_balance_added_to\": \"Added {amount} to {player}. New balance: {balance}\",\n" +
                "  \"chat_money_received\": \"You received {amount}.\",\n" +
                "  \"chat_money_received_from\": \"You received {amount} from {player}.\",\n" +
                "  \"chat_cash_of\": \"Cash of {player}\",\n" +
                "  \"chat_cash_added\": \"Cash added successfully!\",\n" +
                "  \"chat_cash_added_to\": \"Added {amount} cash to {player}. New cash: {cash}\",\n" +
                "  \"chat_cash_received\": \"You received {amount} cash.\",\n" +
                "  \"chat_usage_cash_give\": \"Usage: /cash give <nick> <amount>\",\n" +
                "  \"chat_balance_set_notification\": \"Your balance has been set to {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"EconomySystem loaded successfully!\",\n" +
                "  \"chat_ore_rewards\": \"Ore rewards: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Wood rewards: {status}\",\n" +
                "  \"chat_ores_configured\": \"Configured ores: {count} items\",\n" +
                "  \"chat_api_available\": \"Public API available: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Loading balance data...\",\n" +
                "  \"chat_error_loading_balances\": \"ERROR LOADING BALANCE FILE\",\n" +
                "  \"chat_error_saving_balances\": \"ERROR SAVING BALANCE FILE\",\n" +
                "  \"chat_balances_saved\": \"Economy data saved\",\n" +
                "  \"chat_initial_balance_given\": \"Initial balance of ${amount} given to player: {player}\",\n" +
                "  \"chat_enabled\": \"Enabled\",\n" +
                "  \"chat_disabled\": \"Disabled\",\n" +
                "  \"chat_shop_item_added\": \"Item added to shop! Unique ID: {uniqueid}, Item: {itemid}, Quantity: {quantity}, Sell Price: {pricesell}, Buy Price: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Item removed from shop! Unique ID: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Item prices updated! Unique ID: {uniqueid}, Item: {item}, Buy Price: {pricebuy}, Sell Price: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Item not found! Unique ID: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Error updating item: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Invalid arguments!\",\n" +
                "  \"chat_shop_item_bought\": \"You bought {quantity}x {item} for {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"You bought {added}x of {requested}x {item} (inventory full)!\",\n" +
                "  \"chat_shop_item_sold\": \"You sold {quantity}x {item} for {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"You don't have enough balance! Required: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"You don't have enough items! Required: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Item not found! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"Tab '{tab}' created successfully!\",\n" +
                "  \"chat_shop_tab_removed\": \"Tab '{tab}' removed successfully!\",\n" +
                "  \"chat_shop_tab_not_found\": \"Tab '{tab}' not found!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"Tab '{tab}' already exists!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Maximum of 7 tabs reached!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"Tab name cannot be empty!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Error creating tab!\",\n" +
                "  \"chat_error_tab_create\": \"Error creating tab: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Error removing tab: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Error removing tab!\",\n" +
                "  \"chat_playershop_disabled\": \"Player shops are disabled.\",\n" +
                "  \"chat_playershop_not_open\": \"This player's shop is closed.\",\n" +
                "  \"chat_playershop_empty\": \"This shop is empty.\",\n" +
                "  \"chat_myshop_opened\": \"Your shop has been opened!\",\n" +
                "  \"chat_myshop_closed\": \"Your shop has been closed!\",\n" +
                "  \"chat_myshop_icon_set\": \"Shop icon set to: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Error setting shop icon.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Usage: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Usage: /myshop add <tab> <priceBuy> <priceSell>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Usage: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Usage: /myshop rename <name>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Usage: /myshop tab <create|remove> <name>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Cannot remove tab '{tab}' because it contains items. Remove the items first.\",\n" +
                "  \"chat_myshop_renamed\": \"Shop renamed to: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Error renaming shop.\",\n" +
                "  \"chat_myshop_item_added\": \"Item added to your shop! Unique ID: {uniqueid}, Item: {item}, Quantity: {quantity}, Price: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"Item removed from your shop! Unique ID: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Error removing item from inventory.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Inventory full! Item was not returned.\",\n" +
                "  \"chat_myshop_not_owner\": \"This item does not belong to your shop.\",\n" +
                "  \"chat_playershop_item_bought\": \"You bought {quantity}x {item} for {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"You bought {added}x of {requested}x {item} (inventory full)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"Insufficient stock!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"You cannot buy from your own shop!\",\n" +
                "  \"chat_shops_empty\": \"No shops open at the moment.\",\n" +
                "  \"chat_error_inventory_add\": \"Error adding item to inventory. Money refunded.\",\n" +
                "  \"chat_error_inventory_remove\": \"Error removing item from inventory.\",\n" +
                "  \"chat_error_item_info\": \"Error getting item information: {error}\",\n" +
                "  \"chat_error_item_add\": \"Error adding item: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Error removing item: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"You sold {quantity}x {item} to {player} for {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"You bought {quantity}x {item} from {player} for {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Error accessing inventory.\",\n" +
                "  \"chat_iteminfo_no_item\": \"You are not holding any item.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"Item does not have a valid ID.\",\n" +
                "  \"chat_iteminfo_info\": \"Item ID: {itemid} | Name: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"Item ID cannot be empty!\",\n" +
                "  \"chat_hud_usage\": \"Usage: /hud on or /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD enabled!\",\n" +
                "  \"chat_hud_disabled\": \"HUD disabled!\",\n" +
                "  \"chat_hud_invalid_action\": \"Invalid action! Use 'on' or 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"HUD is disabled on the server.\",\n" +
                "  \"chat_hud_error\": \"Error changing HUD preference.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cError getting player position.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cError getting world UUID.\",\n" +
                "  \"chat_npc_created\": \"&aShop NPC created at your position! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aShop NPC created at your position! ID: {npcId} | Name: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cNo NPCs found.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== Shop NPCs ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7Shop ID: &f{shopId} &7| NPC ID: &f{npcId} &7| World: &f{worldId} &7| Pos: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cUsage: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cUse /shop npc list to see Shop IDs\",\n" +
                "  \"chat_npc_removed\": \"&aNPC removed successfully!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC not found!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cInvalid UUID!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cInvalid Shop ID! Use a number.\",\n" +
                "  \"chat_npc_all_removed\": \"&aAll NPCs have been removed!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC moved successfully to your position!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cUsage: /shop npc movehere <shopId>\",\n" +
                // ========== Command Descriptions ==========
                "  \"desc_shop\": \"Opens the shop\",\n" +
                "  \"desc_shop_add\": \"Adds an item to the shop\",\n" +
                "  \"desc_shop_remove\": \"Removes an item from the shop\",\n" +
                "  \"desc_shop_tab\": \"Manages shop tabs\",\n" +
                "  \"desc_shop_tab_create\": \"Creates a new tab in the shop\",\n" +
                "  \"desc_shop_tab_remove\": \"Removes a tab from the shop and all its items\",\n" +
                "  \"desc_shop_manager\": \"Opens the shop management interface\",\n" +
                "  \"desc_shop_npc\": \"Manages the shop NPC\",\n" +
                "  \"desc_shop_npc_add\": \"Creates the shop NPC at your position\",\n" +
                "  \"desc_shop_npc_remove\": \"Removes a shop NPC by Shop ID\",\n" +
                "  \"desc_shop_npc_list\": \"Lists all shop NPCs\",\n" +
                "  \"desc_shop_npc_removeall\": \"Removes all shop NPCs\",\n" +
                "  \"desc_shop_npc_movehere\": \"Moves the NPC to your position\",\n" +
                "  \"desc_shop_renameplayershop\": \"Renames a player's shop\",\n" +
                "  \"desc_money\": \"Economy system\",\n" +
                "  \"desc_money_pay\": \"Transfers money to another player\",\n" +
                "  \"desc_money_top\": \"Shows the top 10 richest players\",\n" +
                "  \"desc_money_set\": \"Sets a player's balance\",\n" +
                "  \"desc_money_give\": \"Adds balance to a player\",\n" +
                "  \"desc_cash\": \"Cash system\",\n" +
                "  \"desc_cash_give\": \"Adds cash to a player\",\n" +
                "  \"desc_shops\": \"Lists all open shops\",\n" +
                "  \"desc_playershop\": \"Opens a player's shop\",\n" +
                "  \"desc_myshop\": \"Manages your personal shop\",\n" +
                "  \"desc_myshop_open\": \"Opens your personal shop\",\n" +
                "  \"desc_myshop_close\": \"Closes your personal shop\",\n" +
                "  \"desc_myshop_add\": \"Adds an item to your shop\",\n" +
                "  \"desc_myshop_remove\": \"Removes an item from your shop\",\n" +
                "  \"desc_myshop_rename\": \"Renames your shop\",\n" +
                "  \"desc_myshop_tab\": \"Manages your shop tabs\",\n" +
                "  \"desc_myshop_tab_create\": \"Creates a new tab in your shop\",\n" +
                "  \"desc_myshop_tab_remove\": \"Removes a tab from your shop and all its items\",\n" +
                "  \"desc_myshop_manager\": \"Manages your personal shop\",\n" +
                "  \"desc_iteminfo\": \"Shows information about the item in hand\",\n" +
                "  \"desc_hud\": \"Enables/disables the EconomySystem HUD\",\n" +
                "  \"desc_hud_on\": \"Enables the HUD\",\n" +
                "  \"desc_hud_off\": \"Disables the HUD\",\n" +
                // ========== GUI (Window Texts) ==========
                "  \"gui_shop_confirm_buy\": \"Do you want to buy {quantity}x {item} for {price}?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Do you want to sell {quantity}x {item} for {price}?\",\n" +
                "  \"gui_shop_quantity_label\": \"Quantity:\",\n" +
                "  \"gui_shop_button_confirm\": \"Confirm\",\n" +
                "  \"gui_shop_button_cancel\": \"Cancel\",\n" +
                "  \"gui_shop_button_back\": \"Back\",\n" +
                "  \"gui_shop_title\": \"Admin Shop - The Economy\",\n" +
                "  \"gui_shop_npc_add_title\": \"Add Shop NPC\",\n" +
                "  \"gui_shop_npc_add_name\": \"NPC Name:\",\n" +
                "  \"gui_shop_npc_add_model\": \"NPC Model:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"Unique ID\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Quantity\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Buy\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Sell\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Left Click: Buy\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Right Click: Sell\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Right Click: Buy\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Left Click: Sell\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Middle Click to Remove\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Buy disabled\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Sell disabled\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Item not available for purchase or sale\",\n" +
                "  \"gui_shop_manager_title\": \"Manage Shop\",\n" +
                "  \"gui_shop_manager_add_item\": \"Add Item\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Add Tab\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Click to remove item\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Right-click to edit price\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Add Item: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Add Item\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Edit Price: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Edit Item Price\",\n" +
                "  \"gui_shop_manager_item_id\": \"Item ID:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"Item ID cannot be empty!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Quantity:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Buy Price:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Sell Price:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Hotbar Items\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Add New Tab\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Tab Name:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Do you want to remove the tab '{tab}'? All items will be removed.\",\n" +
                "  \"gui_myshop_manager_title\": \"Manage My Shop\",\n" +
                "  \"gui_myshop_rename_shop\": \"Rename Shop\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Rename Shop\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Shop Name:\",\n" +
                "  \"gui_myshop_open_shop\": \"Open Shop\",\n" +
                "  \"gui_myshop_close_shop\": \"Close Shop\",\n" +
                "  \"gui_myshop_set_icon\": \"Choose Icon\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Do you want to remove the item {item} (ID: {uniqueid}) from your shop?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Stock\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Do you want to buy {quantity}x {item} for {price}?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Stock\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Durability\",\n" +
                "  \"gui_shops_empty\": \"No shops open at the moment.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Items\",\n" +
                "  \"gui_shops_tooltip_click\": \"Click to open shop\",\n" +
                "  \"gui_shops_player_shop\": \"{player}'s Shop\",\n" +
                "  \"gui_shops_items_count\": \"items\",\n" +
                "  \"gui_shop_empty\": \"No items in shop\",\n" +
                "  \"gui_shops_title\": \"You are viewing player shops.\",\n" +
                "  \"gui_shops_title_player\": \"Player Shops\",\n" +
                "  \"gui_playershop_title\": \"This shop belongs to player {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"B: \",\n" +
                "  \"gui_shop_price_sell_label\": \"S: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Stock: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Click to remove item\",\n" +
                "  \"gui_shop_manager_add_console\": \"Add Console\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Add Console Command\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Add Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Add Cash Item\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Add Cash Item\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Payment Type:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Money\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Price (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Do you want to buy {quantity}x {item} for {price} Cash?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"You don't have enough Cash! Required: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Cash item added to shop! Unique ID: {uniqueid}, Item: {item}, Quantity: {quantity}, Price: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Name:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Command:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Icon (Hotbar Item)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Console Command\",\n" +
                "  \"chat_shop_console_added\": \"Console command added! Name: {name}, Command: {command}, Price: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"You bought {quantity}x {item} for {price}!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"Name cannot be empty!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"Command cannot be empty!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Select an icon from hotbar!\",\n" +
                "  \"chat_error_console_add\": \"Error adding console command: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Usage: /shop renameplayershop <nick> <name>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Shop renamed successfully! Player: {player}, New name: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Error renaming player shop.\",\n" +
                "  \"npc_interaction_open_shop\": \"Press F to open the shop\",\n" +
                // ========== HUD (HUD Texts) ==========
                "  \"hud_nick\": \"&l&6Nick&r:\",\n" +
                "  \"hud_money\": \"&l&6Money&r:\",\n" +
                "  \"hud_cash\": \"&l&6Cash&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Top Rank&r:\",\n" +
                "  \"hud_rank\": \"Rank\",\n" +
                "  \"hud_shop_status\": \"&l&6Shop\",\n" +
                "  \"hud_shop_open\": \"&aOpen\",\n" +
                "  \"hud_shop_closed\": \"&cClosed\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aYou earned &l{amount}\"\n" +
                "}";
    }

    private static String getDefaultESJson() {
        return "{\n" +
                // ========== CHAT (Mensajes en el Chat) ==========
                "  \"chat_insufficient_balance\": \"¡No tienes saldo suficiente!\",\n" +
                "  \"chat_player_not_found\": \"¡Jugador no encontrado!\",\n" +
                "  \"chat_invalid_amount\": \"¡Cantidad inválida!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"¡No puedes pagarte a ti mismo!\",\n" +
                "  \"chat_payment_sent\": \"¡Pago enviado con éxito!\",\n" +
                "  \"chat_payment_received\": \"¡Has recibido un pago!\",\n" +
                "  \"chat_money_received_from\": \"Has recibido {amount} de {player}.\",\n" +
                "  \"chat_balance_set\": \"¡Saldo definido con éxito!\",\n" +
                "  \"chat_balance_added\": \"¡Saldo agregado con éxito!\",\n" +
                "  \"chat_usage_money\": \"Uso: /money [nick]\",\n" +
                "  \"chat_usage_pay\": \"Uso: /money pay <nick> <valor>\",\n" +
                "  \"chat_usage_set\": \"Uso: /money set <nick> <valor>\",\n" +
                "  \"chat_usage_give\": \"Uso: /money give <nick> <valor>\",\n" +
                "  \"chat_no_permission\": \"¡No tienes permiso para usar este comando!\",\n" +
                "  \"chat_balance_of\": \"Saldo de {player}\",\n" +
                "  \"chat_top_10_richest\": \"Top 10 Más Ricos\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"No se encontraron jugadores en el ranking.\",\n" +
                "  \"chat_money_top_disabled\": \"El ranking de dinero está deshabilitado.\",\n" +
                "  \"chat_top_position\": \"{position}º {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Saldo de {player} definido a {amount}\",\n" +
                "  \"chat_balance_added_to\": \"Agregado {amount} a {player}. Nuevo saldo: {balance}\",\n" +
                "  \"chat_money_received\": \"Recibiste {amount}.\",\n" +
                "  \"chat_money_received_from\": \"Has recibido {amount} de {player}.\",\n" +
                "  \"chat_cash_of\": \"Cash de {player}\",\n" +
                "  \"chat_cash_added\": \"¡Cash agregado con éxito!\",\n" +
                "  \"chat_cash_added_to\": \"Agregado {amount} de cash a {player}. Nuevo cash: {cash}\",\n" +
                "  \"chat_cash_received\": \"Recibiste {amount} de cash.\",\n" +
                "  \"chat_usage_cash_give\": \"Uso: /cash give <nick> <valor>\",\n" +
                "  \"chat_balance_set_notification\": \"Tu saldo ha sido definido a {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"¡EconomySystem cargado con éxito!\",\n" +
                "  \"chat_ore_rewards\": \"Recompensas por minerales: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Recompensas por maderas: {status}\",\n" +
                "  \"chat_ores_configured\": \"Minerales configurados: {count} elementos\",\n" +
                "  \"chat_api_available\": \"API pública disponible: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Cargando datos de saldo...\",\n" +
                "  \"chat_error_loading_balances\": \"ERROR AL CARGAR ARCHIVO DE SALDOS\",\n" +
                "  \"chat_error_saving_balances\": \"ERROR AL GUARDAR ARCHIVO DE SALDOS\",\n" +
                "  \"chat_balances_saved\": \"Datos de economía guardados\",\n" +
                "  \"chat_initial_balance_given\": \"Saldo inicial de ${amount} dado al jugador: {player}\",\n" +
                "  \"chat_enabled\": \"Activado\",\n" +
                "  \"chat_disabled\": \"Desactivado\",\n" +
                "  \"chat_shop_item_added\": \"¡Item agregado a la tienda! ID Único: {uniqueid}, Item: {itemid}, Cantidad: {quantity}, Precio de Venta: {pricesell}, Precio de Compra: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"¡Item removido de la tienda! ID Único: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"¡Precios del item actualizados! ID Único: {uniqueid}, Item: {item}, Precio de Compra: {pricebuy}, Precio de Venta: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"¡Item no encontrado! ID Único: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Error al actualizar item: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"¡Argumentos inválidos!\",\n" +
                "  \"chat_shop_item_bought\": \"¡Compraste {quantity}x {item} por {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"¡Compraste {added}x de {requested}x {item} (inventario lleno)!\",\n" +
                "  \"chat_shop_item_sold\": \"¡Vendiste {quantity}x {item} por {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"¡No tienes saldo suficiente! Requerido: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"¡No tienes suficientes items! Requerido: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"¡Item no encontrado! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"¡Tab '{tab}' creada con éxito!\",\n" +
                "  \"chat_shop_tab_removed\": \"¡Tab '{tab}' eliminada con éxito!\",\n" +
                "  \"chat_shop_tab_not_found\": \"¡Tab '{tab}' no encontrada!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"¡Tab '{tab}' ya existe!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"¡Se alcanzó el límite de 7 tabs!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"¡El nombre de la tab no puede estar vacío!\",\n" +
                "  \"chat_shop_tab_create_error\": \"¡Error al crear la tab!\",\n" +
                "  \"chat_error_tab_create\": \"Error al crear tab: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Error al eliminar tab: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"¡Error al eliminar la tab!\",\n" +
                "  \"chat_playershop_disabled\": \"Las tiendas de jugadores están deshabilitadas.\",\n" +
                "  \"chat_playershop_not_open\": \"La tienda de este jugador está cerrada.\",\n" +
                "  \"chat_playershop_empty\": \"Esta tienda está vacía.\",\n" +
                "  \"chat_myshop_opened\": \"¡Tu tienda ha sido abierta!\",\n" +
                "  \"chat_myshop_closed\": \"¡Tu tienda ha sido cerrada!\",\n" +
                "  \"chat_myshop_icon_set\": \"Ícono de la tienda establecido en: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Error al establecer el ícono de la tienda.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Uso: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Uso: /myshop add <tab> <precio_compra> <precio_venta>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Uso: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Uso: /myshop rename <nombre>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Uso: /myshop tab <create|remove> <nombre>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"No se puede eliminar la tab '{tab}' porque contiene items. Elimina los items primero.\",\n" +
                "  \"chat_myshop_renamed\": \"Tienda renombrada a: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Error al renombrar la tienda.\",\n" +
                "  \"chat_myshop_item_added\": \"¡Item agregado a tu tienda! ID Único: {uniqueid}, Item: {item}, Cantidad: {quantity}, Precio: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"¡Item removido de tu tienda! ID Único: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Error al remover item del inventario.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"¡Inventario lleno! El item no fue devuelto.\",\n" +
                "  \"chat_myshop_not_owner\": \"Este item no pertenece a tu tienda.\",\n" +
                "  \"chat_playershop_item_bought\": \"¡Compraste {quantity}x {item} por {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"¡Compraste {added}x de {requested}x {item} (inventario lleno)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"¡Stock insuficiente!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"¡No puedes comprar de tu propia tienda!\",\n" +
                "  \"chat_shops_empty\": \"No hay tiendas abiertas en este momento.\",\n" +
                "  \"chat_error_inventory_add\": \"Error al agregar item al inventario. Dinero devuelto.\",\n" +
                "  \"chat_error_inventory_remove\": \"Error al remover item del inventario.\",\n" +
                "  \"chat_error_item_info\": \"Error al obtener información del item: {error}\",\n" +
                "  \"chat_error_item_add\": \"Error al agregar item: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Error al remover item: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Vendiste {quantity}x {item} a {player} por {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"Compraste {quantity}x {item} de {player} por {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Error al acceder al inventario.\",\n" +
                "  \"chat_iteminfo_no_item\": \"No estás sosteniendo ningún item.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"El item no tiene un ID válido.\",\n" +
                "  \"chat_iteminfo_info\": \"Item ID: {itemid} | Nombre: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"¡El ID del item no puede estar vacío!\",\n" +
                "  \"chat_hud_usage\": \"Uso: /hud on o /hud off\",\n" +
                "  \"chat_hud_enabled\": \"¡HUD activada!\",\n" +
                "  \"chat_hud_disabled\": \"¡HUD desactivada!\",\n" +
                "  \"chat_hud_invalid_action\": \"¡Acción inválida! Usa 'on' o 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"La HUD está deshabilitada en el servidor.\",\n" +
                "  \"chat_hud_error\": \"Error al cambiar la preferencia de la HUD.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cError al obtener la posición del jugador.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cError al obtener UUID del mundo.\",\n" +
                "  \"chat_npc_created\": \"&aNPC de la tienda creado en tu posición! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aNPC de la tienda creado en tu posición! ID: {npcId} | Nombre: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cNo se encontraron NPCs.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== NPCs de la Tienda ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7Shop ID: &f{shopId} &7| NPC ID: &f{npcId} &7| Mundo: &f{worldId} &7| Pos: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cUso: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cUsa /shop npc list para ver los Shop IDs de los NPCs\",\n" +
                "  \"chat_npc_removed\": \"&aNPC eliminado con éxito!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC no encontrado!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cUUID inválido!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cShop ID inválido! Usa un número.\",\n" +
                "  \"chat_npc_all_removed\": \"&aTodos los NPCs han sido eliminados!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC movido con éxito a tu posición!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cUso: /shop npc movehere <shopId>\",\n" +
                // ========== Descripciones de Comandos ==========
                "  \"desc_shop\": \"Abre la tienda\",\n" +
                "  \"desc_shop_add\": \"Añade un artículo a la tienda\",\n" +
                "  \"desc_shop_remove\": \"Elimina un artículo de la tienda\",\n" +
                "  \"desc_shop_tab\": \"Gestiona las pestañas de la tienda\",\n" +
                "  \"desc_shop_tab_create\": \"Crea una nueva pestaña en la tienda\",\n" +
                "  \"desc_shop_tab_remove\": \"Elimina una pestaña de la tienda y todos sus artículos\",\n" +
                "  \"desc_shop_manager\": \"Abre la interfaz de gestión de la tienda\",\n" +
                "  \"desc_shop_npc\": \"Gestiona el NPC de la tienda\",\n" +
                "  \"desc_shop_npc_add\": \"Crea el NPC de la tienda en tu posición\",\n" +
                "  \"desc_shop_npc_remove\": \"Elimina un NPC de la tienda por Shop ID\",\n" +
                "  \"desc_shop_npc_list\": \"Lista todos los NPCs de la tienda\",\n" +
                "  \"desc_shop_npc_removeall\": \"Elimina todos los NPCs de la tienda\",\n" +
                "  \"desc_shop_npc_movehere\": \"Mueve el NPC a tu posición\",\n" +
                "  \"desc_shop_renameplayershop\": \"Renombra la tienda de un jugador\",\n" +
                "  \"desc_money\": \"Sistema de economía\",\n" +
                "  \"desc_money_pay\": \"Transfiere dinero a otro jugador\",\n" +
                "  \"desc_money_top\": \"Muestra el top 10 de jugadores más ricos\",\n" +
                "  \"desc_money_set\": \"Establece el saldo de un jugador\",\n" +
                "  \"desc_money_give\": \"Añade saldo a un jugador\",\n" +
                "  \"desc_cash\": \"Sistema de cash\",\n" +
                "  \"desc_cash_give\": \"Añade cash a un jugador\",\n" +
                "  \"desc_shops\": \"Lista todas las tiendas abiertas\",\n" +
                "  \"desc_playershop\": \"Abre la tienda de un jugador\",\n" +
                "  \"desc_myshop\": \"Gestiona tu tienda personal\",\n" +
                "  \"desc_myshop_open\": \"Abre tu tienda personal\",\n" +
                "  \"desc_myshop_close\": \"Cierra tu tienda personal\",\n" +
                "  \"desc_myshop_add\": \"Añade un artículo a tu tienda\",\n" +
                "  \"desc_myshop_remove\": \"Elimina un artículo de tu tienda\",\n" +
                "  \"desc_myshop_rename\": \"Renombra tu tienda\",\n" +
                "  \"desc_myshop_tab\": \"Gestiona las pestañas de tu tienda\",\n" +
                "  \"desc_myshop_tab_create\": \"Crea una nueva pestaña en tu tienda\",\n" +
                "  \"desc_myshop_tab_remove\": \"Elimina una pestaña de tu tienda y todos sus artículos\",\n" +
                "  \"desc_myshop_manager\": \"Gestiona tu tienda personal\",\n" +
                "  \"desc_iteminfo\": \"Muestra información del artículo en la mano\",\n" +
                "  \"desc_hud\": \"Activa/desactiva la HUD del EconomySystem\",\n" +
                "  \"desc_hud_on\": \"Activa la HUD\",\n" +
                "  \"desc_hud_off\": \"Desactiva la HUD\",\n" +
                // ========== GUI (Textos de las Ventanas) ==========
                "  \"gui_shop_confirm_buy\": \"¿Deseas comprar {quantity}x {item} por {price}?\",\n" +
                "  \"gui_shop_confirm_sell\": \"¿Deseas vender {quantity}x {item} por {price}?\",\n" +
                "  \"gui_shop_quantity_label\": \"Cantidad:\",\n" +
                "  \"gui_shop_button_confirm\": \"Confirmar\",\n" +
                "  \"gui_shop_button_cancel\": \"Cancelar\",\n" +
                "  \"gui_shop_button_back\": \"Voltar\",\n" +
                "  \"gui_shop_title\": \"Tienda Admin - The Economy\",\n" +
                "  \"gui_shop_npc_add_title\": \"Agregar NPC de Tienda\",\n" +
                "  \"gui_shop_npc_add_name\": \"Nombre del NPC:\",\n" +
                "  \"gui_shop_npc_add_model\": \"Modelo del NPC:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"ID Único\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Cantidad\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Comprar\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Vender\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Botón Izquierdo: Comprar\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Botón Derecho: Vender\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Botón Derecho: Comprar\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Botón Izquierdo: Vender\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Clic con el botón del medio para Remover\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Compra deshabilitada\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Venta deshabilitada\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Item no disponible para compra o venta\",\n" +
                "  \"gui_shop_manager_title\": \"Gestionar Tienda\",\n" +
                "  \"gui_shop_manager_add_item\": \"Agregar Item\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Agregar Tab\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Clic para eliminar item\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Clic derecho para editar precio\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Agregar Item: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Agregar Item\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Editar Precio: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Editar Precio del Item\",\n" +
                "  \"gui_shop_manager_item_id\": \"ID del Item:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"¡El ID del item no puede estar vacío!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Cantidad:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Precio de Compra:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Precio de Venta:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Items de la Barra\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Agregar Nueva Tab\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Nombre de la Tab:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"¿Desea eliminar la tab '{tab}'? Todos los items serán eliminados.\",\n" +
                "  \"gui_myshop_manager_title\": \"Gestionar Mi Tienda\",\n" +
                "  \"gui_myshop_rename_shop\": \"Renombrar Tienda\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Renombrar Tienda\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Nombre de la Tienda:\",\n" +
                "  \"gui_myshop_open_shop\": \"Abrir Tienda\",\n" +
                "  \"gui_myshop_close_shop\": \"Cerrar Tienda\",\n" +
                "  \"gui_myshop_set_icon\": \"Elegir Ícono\",\n" +
                "  \"gui_myshop_confirm_remove\": \"¿Desea eliminar el item {item} (ID: {uniqueid}) de su tienda?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Stock\",\n" +
                "  \"gui_playershop_confirm_buy\": \"¿Deseas comprar {quantity}x {item} por {price}?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Stock\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Durabilidad\",\n" +
                "  \"gui_shops_empty\": \"No hay tiendas abiertas en este momento.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Items\",\n" +
                "  \"gui_shops_tooltip_click\": \"Clic para abrir la tienda\",\n" +
                "  \"gui_shops_player_shop\": \"Tienda de {player}\",\n" +
                "  \"gui_shops_items_count\": \"items\",\n" +
                "  \"gui_shop_empty\": \"No hay items en la tienda\",\n" +
                "  \"gui_shops_title\": \"Estás viendo las tiendas de los jugadores.\",\n" +
                "  \"gui_shops_title_player\": \"Tiendas de Jugadores\",\n" +
                "  \"gui_playershop_title\": \"Esta tienda pertenece al jugador {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"C: \",\n" +
                "  \"gui_shop_price_sell_label\": \"V: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Stock: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Clic para remover item\",\n" +
                "  \"gui_shop_manager_add_console\": \"Agregar Console\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Agregar Comando Console\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Agregar Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Agregar Item Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Agregar Item Cash\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Tipo de Pago:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Dinero\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Precio (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"¿Deseas comprar {quantity}x {item} por {price} Cash?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"¡No tienes suficiente Cash! Necesario: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"¡Item Cash agregado a la tienda! ID Único: {uniqueid}, Item: {item}, Cantidad: {quantity}, Precio: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Nombre:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Comando:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Ícono (Item de Hotbar)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Comando Console\",\n" +
                "  \"chat_shop_console_added\": \"¡Comando console agregado! Nombre: {name}, Comando: {command}, Precio: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"¡Compraste {quantity}x {item} por {price}!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"¡El nombre no puede estar vacío!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"¡El comando no puede estar vacío!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"¡Selecciona un ícono de la hotbar!\",\n" +
                "  \"chat_error_console_add\": \"Error al agregar comando console: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Uso: /shop renameplayershop <nick> <nombre>\",\n" +
                "  \"chat_shop_rename_player_success\": \"¡Tienda renombrada con éxito! Jugador: {player}, Nuevo nombre: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Error al renombrar la tienda del jugador.\",\n" +
                "  \"npc_interaction_open_shop\": \"Presiona F para abrir la tienda\",\n" +
                // ========== HUD (Textos de la HUD) ==========
                "  \"hud_nick\": \"&l&6Nick&r:\",\n" +
                "  \"hud_money\": \"&l&6Dinero&r:\",\n" +
                "  \"hud_cash\": \"&l&6Cash&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Top Rango&r:\",\n" +
                "  \"hud_rank\": \"Rango\",\n" +
                "  \"hud_shop_status\": \"&l&6Tienda\",\n" +
                "  \"hud_shop_open\": \"&aAbierta\",\n" +
                "  \"hud_shop_closed\": \"&cCerrada\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aGanaste &l{amount}\"\n" +
                "}";
    }

    private static String getDefaultRUJson() {
        return "{\n" +
                // ========== CHAT (Сообщения в чате) ==========
                "  \"chat_insufficient_balance\": \"На вашем кошельке недостаточно средств!\",\n" +
                "  \"chat_player_not_found\": \"Игрок не найден!\",\n" +
                "  \"chat_invalid_amount\": \"Недопустимая сумма!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"Ты не можешь заплатить самому себе!\",\n" +
                "  \"chat_payment_sent\": \"Платёж успешно отправлен!\",\n" +
                "  \"chat_payment_received\": \"Вы получили платёж!\",\n" +
                "  \"chat_balance_set\": \"Баланс успешно установлен!\",\n" +
                "  \"chat_balance_added\": \"Баланс успешно пополнен!\",\n" +
                "  \"chat_usage_money\": \"Используй: /money [nick]\",\n" +
                "  \"chat_usage_pay\": \"Используй: /money pay <nick> <amount>\",\n" +
                "  \"chat_usage_set\": \"Используй: /money set <nick> <amount>\",\n" +
                "  \"chat_usage_give\": \"Используй: /money give <nick> <amount>\",\n" +
                "  \"chat_no_permission\": \"У вас нет разрешения использовать эту команду!\",\n" +
                "  \"chat_balance_of\": \"Баланс кошелька {player}\",\n" +
                "  \"chat_top_10_richest\": \"Топ-10 самых богатых\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"Игроки в рейтинге не найдены.\",\n" +
                "  \"chat_money_top_disabled\": \"Рейтинг денег отключен.\",\n" +
                "  \"chat_top_position\": \"{position} {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Баланс {player} Пополнен {amount}\",\n" +
                "  \"chat_money_received_from\": \"Вы получили {amount} от {player}.\",\n" +
                "  \"chat_balance_added_to\": \"Добавить {amount} для {player}. Новый баланс: {balance}\",\n" +
                "  \"chat_money_received\": \"Вы получили {amount}.\",\n" +
                "  \"chat_money_received_from\": \"Вы получили {amount} от {player}.\",\n" +
                "  \"chat_cash_of\": \"Кеш {player}\",\n" +
                "  \"chat_cash_added\": \"Кеш успешно добавлен!\",\n" +
                "  \"chat_cash_added_to\": \"Добавлено {amount} кеша для {player}. Новый кеш: {cash}\",\n" +
                "  \"chat_cash_received\": \"Вы получили {amount} кеша.\",\n" +
                "  \"chat_usage_cash_give\": \"Использование: /cash give <nick> <сумма>\",\n" +
                "  \"chat_balance_set_notification\": \"Ваш баланс был установлен на {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"Экономическая Система успешно загружена!\",\n" +
                "  \"chat_ore_rewards\": \"Награды за руду: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Награды за древесину: {status}\",\n" +
                "  \"chat_ores_configured\": \"Настроенные руды: {count} items\",\n" +
                "  \"chat_api_available\": \"Public API available: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Загрузка данных баланса...\",\n" +
                "  \"chat_error_loading_balances\": \"ОШИБКА ЗАГРУЗКИ ФАЙЛА БАЛАНСА\",\n" +
                "  \"chat_error_saving_balances\": \"ОШИБКА СОХРАНЕНИЯ ФАЙЛА БАЛАНСА\",\n" +
                "  \"chat_balances_saved\": \"Данные экономики сохранены\",\n" +
                "  \"chat_initial_balance_given\": \"Начальный баланс ${amount} выдано игроку: {player}\",\n" +
                "  \"chat_enabled\": \"Включено\",\n" +
                "  \"chat_disabled\": \"Выключено\",\n" +
                "  \"chat_shop_item_added\": \"Товар добавлен в магазин! Уникальный Айди: {uniqueid}, Предмет: {itemid}, Количество: {quantity}, Цена продажи: {pricesell}, Цена покупки: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Товар удалён из магазина! Уникальный Айди: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Цены товара обновлены! Уникальный Айди: {uniqueid}, Предмет: {item}, Цена покупки: {pricebuy}, Цена продажи: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Товар не найден! Уникальный ID: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Ошибка при обновлении товара: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Неверные аргументы!\",\n" +
                "  \"chat_shop_item_bought\": \"Вы приобрели {quantity}x {item} за {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"Вы приобрели {added}x из {requested}x {item} (инвентарь полон)!\",\n" +
                "  \"chat_shop_item_sold\": \"Вы продали {quantity}x {item} за {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"У вас недостаточно средств! Требуется: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"У вас недостаточно предметов! Требуется: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Предмет не найден! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"Вкладка '{tab}' успешно создана!\",\n" +
                "  \"chat_shop_tab_removed\": \"Вкладка '{tab}' успешно удалена!\",\n" +
                "  \"chat_shop_tab_not_found\": \"Вкладка '{tab}' не найдена!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"Вкладка '{tab}' уже существует!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Достигнут лимит в 7 вкладок!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"Имя вкладки не может быть пустым!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Ошибка при создании вкладки!\",\n" +
                "  \"chat_error_tab_create\": \"Ошибка при создании вкладки: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Ошибка при удалении вкладки: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Ошибка при удалении вкладки!\",\n" +
                "  \"chat_playershop_disabled\": \"Магазины игроков отключены.\",\n" +
                "  \"chat_playershop_empty\": \"Этот магазин пуст.\",\n" +
                "  \"chat_myshop_usage\": \"Используй: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Используй: /myshop add <tab> <цена_покупки> <цена_продажи>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Используй: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Используй: /myshop rename <имя>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Используй: /myshop tab <create|remove> <имя>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Невозможно удалить вкладку '{tab}', так как она содержит предметы. Сначала удалите предметы.\",\n" +
                "  \"chat_myshop_renamed\": \"Магазин переименован в: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Ошибка при переименовании магазина.\",\n" +
                "  \"chat_myshop_item_removed\": \"Предмет удалён из вашего магазина! Уникальный ID: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Ошибка при удалении предмета из инвентаря.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Инвентарь полон! Предмет не был возвращён.\",\n" +
                "  \"chat_myshop_not_owner\": \"Этот предмет не принадлежит вашему магазину.\",\n" +
                "  \"chat_playershop_item_bought\": \"Вы приобрели {quantity}x {item} за {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"Вы приобрели {added}x из {requested}x {item} (инвентарь полон)!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"Вы не можете покупать в своём собственном магазине!\",\n" +
                "  \"chat_shops_empty\": \"В данный момент нет открытых магазинов.\",\n" +
                "  \"chat_error_inventory_add\": \"Ошибка при добавлении предмета в инвентарь. Деньги возвращены.\",\n" +
                "  \"chat_error_inventory_remove\": \"Ошибка при удалении предмета из инвентаря.\",\n" +
                "  \"chat_error_item_info\": \"Ошибка получения информации о предмете: {error}\",\n" +
                "  \"chat_error_item_add\": \"Ошибка добавления предмета: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Ошибка удаления предмета: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Вы продали {quantity}x {item} для {player} за {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"Вы приобрели {quantity}x {item} для {player} за {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Ошибка доступа к инвентарю.\",\n" +
                "  \"chat_iteminfo_no_item\": \"Вы не держите никакого предмета.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"Предмет не имеет действительного ID.\",\n" +
                "  \"chat_iteminfo_info\": \"ID предмета: {itemid} | Имя: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"ID предмета не может быть пустым!\",\n" +
                "  \"chat_hud_usage\": \"Использование: /hud on или /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD включена!\",\n" +
                "  \"chat_hud_disabled\": \"HUD выключена!\",\n" +
                "  \"chat_hud_invalid_action\": \"Неверное действие! Используйте 'on' или 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"HUD отключена на сервере.\",\n" +
                "  \"chat_hud_error\": \"Ошибка при изменении настройки HUD.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cОшибка получения позиции игрока.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cОшибка получения UUID мира.\",\n" +
                "  \"chat_npc_created\": \"&aNPC магазина создан на вашей позиции! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aNPC магазина создан на вашей позиции! ID: {npcId} | Имя: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cNPC не найдены.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== NPCs Магазина ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7ID: &f{npcId} &7| Мир: &f{worldId} &7| Поз: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cИспользование: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cИспользуйте /shop npc list для просмотра Shop ID NPCs\",\n" +
                "  \"chat_npc_removed\": \"&aNPC успешно удалён!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC не найден!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cНеверный UUID!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cНеверный Shop ID! Используйте число.\",\n" +
                "  \"chat_npc_all_removed\": \"&aВсе NPCs были удалены!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC успешно перемещён на вашу позицию!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cИспользование: /shop npc movehere <shopId>\",\n" +
                // ========== Описания Команд ==========
                "  \"desc_shop\": \"Открывает магазин\",\n" +
                "  \"desc_shop_add\": \"Добавляет предмет в магазин\",\n" +
                "  \"desc_shop_remove\": \"Удаляет предмет из магазина\",\n" +
                "  \"desc_shop_tab\": \"Управляет вкладками магазина\",\n" +
                "  \"desc_shop_tab_create\": \"Создает новую вкладку в магазине\",\n" +
                "  \"desc_shop_tab_remove\": \"Удаляет вкладку из магазина и все её предметы\",\n" +
                "  \"desc_shop_manager\": \"Открывает интерфейс управления магазином\",\n" +
                "  \"desc_shop_npc\": \"Управляет NPC магазина\",\n" +
                "  \"desc_shop_npc_add\": \"Создает NPC магазина на вашей позиции\",\n" +
                "  \"desc_shop_npc_remove\": \"Удаляет NPC магазина по Shop ID\",\n" +
                "  \"desc_shop_npc_list\": \"Список всех NPC магазина\",\n" +
                "  \"desc_shop_npc_removeall\": \"Удаляет всех NPC магазина\",\n" +
                "  \"desc_shop_npc_movehere\": \"Перемещает NPC на вашу позицию\",\n" +
                "  \"desc_shop_renameplayershop\": \"Переименовывает магазин игрока\",\n" +
                "  \"desc_money\": \"Экономическая система\",\n" +
                "  \"desc_money_pay\": \"Переводит деньги другому игроку\",\n" +
                "  \"desc_money_top\": \"Показывает топ 10 самых богатых игроков\",\n" +
                "  \"desc_money_set\": \"Устанавливает баланс игрока\",\n" +
                "  \"desc_money_give\": \"Добавляет баланс игроку\",\n" +
                "  \"desc_cash\": \"Система кеша\",\n" +
                "  \"desc_cash_give\": \"Добавляет кеш игроку\",\n" +
                "  \"desc_shops\": \"Список всех открытых магазинов\",\n" +
                "  \"desc_playershop\": \"Открывает магазин игрока\",\n" +
                "  \"desc_myshop\": \"Управляет вашим личным магазином\",\n" +
                "  \"desc_myshop_open\": \"Открывает ваш личный магазин\",\n" +
                "  \"desc_myshop_close\": \"Закрывает ваш личный магазин\",\n" +
                "  \"desc_myshop_add\": \"Добавляет предмет в ваш магазин\",\n" +
                "  \"desc_myshop_remove\": \"Удаляет предмет из вашего магазина\",\n" +
                "  \"desc_myshop_rename\": \"Переименовывает ваш магазин\",\n" +
                "  \"desc_myshop_tab\": \"Управляет вкладками вашего магазина\",\n" +
                "  \"desc_myshop_tab_create\": \"Создает новую вкладку в вашем магазине\",\n" +
                "  \"desc_myshop_tab_remove\": \"Удаляет вкладку из вашего магазина и все её предметы\",\n" +
                "  \"desc_myshop_manager\": \"Управляет вашим личным магазином\",\n" +
                "  \"desc_iteminfo\": \"Показывает информацию о предмете в руке\",\n" +
                "  \"desc_hud\": \"Включает/выключает HUD EconomySystem\",\n" +
                "  \"desc_hud_on\": \"Включает HUD\",\n" +
                "  \"desc_hud_off\": \"Выключает HUD\",\n" +
                // ========== GUI (Тексты окон) ==========
                "  \"gui_shop_confirm_buy\": \"Вы хотите приобрести {quantity}x {item} за {price}?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Вы хотите продать {quantity}x {item} за {price}?\",\n" +
                "  \"gui_shop_quantity_label\": \"Количество:\",\n" +
                "  \"gui_shop_button_confirm\": \"Подтвердить\",\n" +
                "  \"gui_shop_button_cancel\": \"Отменить\",\n" +
                "  \"gui_shop_button_back\": \"Назад\",\n" +
                "  \"gui_shop_title\": \"Магазин администраторов - The Economy\",\n" +
                "  \"gui_shop_npc_add_title\": \"Добавить NPC Магазина\",\n" +
                "  \"gui_shop_npc_add_name\": \"Имя NPC:\",\n" +
                "  \"gui_shop_npc_add_model\": \"Модель NPC:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"Уникальный ID\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Количество\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Купить\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Продать\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Левый клик: Купить\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Правый клик: Продать\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Правый клик: Купить\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Левый клик: Продать\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Средний клик: Удалить\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Покупка отключена\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Продажа отключена\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Предмет недоступен для покупки или продажи\",\n" +
                "  \"gui_shop_manager_title\": \"Управление магазином\",\n" +
                "  \"gui_shop_manager_add_item\": \"Добавить предмет\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Добавить вкладку\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Нажмите, чтобы удалить предмет\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Правый клик для редактирования цены\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Добавить предмет: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Добавить предмет\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Редактировать цену: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Редактировать цену предмета\",\n" +
                "  \"gui_shop_manager_item_id\": \"ID предмета:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"ID предмета не может быть пустым!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Количество:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Цена покупки:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Цена продажи:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Предметы на панели\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Добавить новую вкладку\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Название вкладки:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Вы хотите удалить вкладку '{tab}'? Все предметы будут удалены.\",\n" +
                "  \"gui_myshop_manager_title\": \"Управление моим магазином\",\n" +
                "  \"gui_myshop_rename_shop\": \"Переименовать магазин\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Переименовать магазин\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Название магазина:\",\n" +
                "  \"gui_myshop_open_shop\": \"Открыть магазин\",\n" +
                "  \"gui_myshop_close_shop\": \"Закрыть магазин\",\n" +
                "  \"gui_myshop_set_icon\": \"Выбрать иконку\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Вы хотите удалить предмет {item} (ID: {uniqueid}) из вашего магазина?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Склад\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Вы хотите приобрести {quantity}x {item} за {price}?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Склад\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Прочность\",\n" +
                "  \"gui_shops_empty\": \"В данный момент нет открытых магазинов.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Предметы\",\n" +
                "  \"gui_shops_tooltip_click\": \"Нажмите, чтобы открыть магазин\",\n" +
                "  \"gui_shops_player_shop\": \"Магазин {player}\",\n" +
                "  \"gui_shops_items_count\": \"предметов\",\n" +
                "  \"gui_shop_empty\": \"В магазине нет товаров\",\n" +
                "  \"gui_shops_title\": \"Вы просматриваете магазины игроков.\",\n" +
                "  \"gui_shops_title_player\": \"Магазины игроков\",\n" +
                "  \"gui_playershop_title\": \"Этот магазин принадлежит игроку {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"К: \",\n" +
                "  \"gui_shop_price_sell_label\": \"П: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Склад: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Нажмите, чтобы удалить предмет\",\n" +
                "  \"gui_shop_manager_add_console\": \"Добавить Консоль\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Добавить Команду Консоли\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Добавить Кеш\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Добавить Кеш Предмет\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Добавить Кеш Предмет\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Тип оплаты:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Деньги\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Кеш\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Цена (Кеш):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Хотите купить {quantity}x {item} за {price} Кеш?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"У вас недостаточно Кеша! Требуется: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Кеш предмет добавлен в магазин! Уникальный Айди: {uniqueid}, Предмет: {item}, Количество: {quantity}, Цена: {price} Кеш\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Имя:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Команда:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Иконка (Предмет из Хотбара)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Команда Консоли\",\n" +
                "  \"chat_shop_console_added\": \"Команда консоли добавлена! Имя: {name}, Команда: {command}, Цена: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"Вы приобрели {quantity}x {item} за {price}!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"Имя не может быть пустым!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"Команда не может быть пустой!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Выберите иконку из хотбара!\",\n" +
                "  \"chat_error_console_add\": \"Ошибка добавления команды консоли: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Использование: /shop renameplayershop <ник> <имя>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Магазин успешно переименован! Игрок: {player}, Новое имя: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Ошибка переименования магазина игрока.\",\n" +
                "  \"npc_interaction_open_shop\": \"Нажмите F, чтобы открыть магазин\",\n" +
                // ========== HUD (Тексты HUD) ==========
                "  \"hud_nick\": \"&l&6Ник&r:\",\n" +
                "  \"hud_money\": \"&l&6Деньги&r:\",\n" +
                "  \"hud_cash\": \"&l&6Кеш&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Топ Ранг&r:\",\n" +
                "  \"hud_rank\": \"Ранг\",\n" +
                "  \"hud_shop_status\": \"&l&6Магазин\",\n" +
                "  \"hud_shop_open\": \"&aОткрыт\",\n" +
                "  \"hud_shop_closed\": \"&cЗакрыт\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aВы заработали &l{amount}\"\n" +
                "}";
    }

    private static String getDefaultPLJson() {
        return "{\n" +
                // ========== CHAT (Wiadomości na czacie) ==========
                "  \"chat_insufficient_balance\": \"Nie masz wystarczającego salda!\",\n" +
                "  \"chat_player_not_found\": \"Gracz nie znaleziony!\",\n" +
                "  \"chat_invalid_amount\": \"Nieprawidłowa kwota!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"Nie możesz zapłacić sobie!\",\n" +
                "  \"chat_payment_sent\": \"Płatność wysłana pomyślnie!\",\n" +
                "  \"chat_payment_received\": \"Otrzymałeś płatność!\",\n" +
                "  \"chat_balance_set\": \"Saldo ustawione pomyślnie!\",\n" +
                "  \"chat_balance_added\": \"Saldo dodane pomyślnie!\",\n" +
                "  \"chat_usage_money\": \"Użycie: /money [nick]\",\n" +
                "  \"chat_usage_pay\": \"Użycie: /money pay <nick> <kwota>\",\n" +
                "  \"chat_usage_set\": \"Użycie: /money set <nick> <kwota>\",\n" +
                "  \"chat_usage_give\": \"Użycie: /money give <nick> <kwota>\",\n" +
                "  \"chat_no_permission\": \"Nie masz uprawnień do użycia tego polecenia!\",\n" +
                "  \"chat_balance_of\": \"Saldo {player}\",\n" +
                "  \"chat_top_10_richest\": \"Top 10 Najbogatszych\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"Nie znaleziono graczy w rankingu.\",\n" +
                "  \"chat_money_top_disabled\": \"Ranking pieniędzy jest wyłączony.\",\n" +
                "  \"chat_top_position\": \"{position} {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Saldo {player} ustawione na {amount}\",\n" +
                "  \"chat_balance_added_to\": \"Dodano {amount} do {player}. Nowe saldo: {balance}\",\n" +
                "  \"chat_money_received\": \"Otrzymałeś {amount}.\",\n" +
                "  \"chat_money_received_from\": \"Otrzymałeś {amount} od {player}.\",\n" +
                "  \"chat_cash_of\": \"Gotówka {player}\",\n" +
                "  \"chat_cash_added\": \"Gotówka dodana pomyślnie!\",\n" +
                "  \"chat_cash_added_to\": \"Dodano {amount} gotówki do {player}. Nowa gotówka: {cash}\",\n" +
                "  \"chat_cash_received\": \"Otrzymałeś {amount} gotówki.\",\n" +
                "  \"chat_usage_cash_give\": \"Użycie: /cash give <nick> <wartość>\",\n" +
                "  \"chat_balance_set_notification\": \"Twoje saldo zostało ustawione na {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"EconomySystem załadowany pomyślnie!\",\n" +
                "  \"chat_ore_rewards\": \"Nagrody za rudy: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Nagrody za drewno: {status}\",\n" +
                "  \"chat_ores_configured\": \"Skonfigurowane rudy: {count} przedmiotów\",\n" +
                "  \"chat_api_available\": \"Publiczne API dostępne: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Ładowanie danych salda...\",\n" +
                "  \"chat_error_loading_balances\": \"BŁĄD ŁADOWANIA PLIKU SALDA\",\n" +
                "  \"chat_error_saving_balances\": \"BŁĄD ZAPISYWANIA PLIKU SALDA\",\n" +
                "  \"chat_balances_saved\": \"Dane ekonomiczne zapisane\",\n" +
                "  \"chat_initial_balance_given\": \"Początkowe saldo ${amount} przyznane graczowi: {player}\",\n" +
                "  \"chat_enabled\": \"Włączone\",\n" +
                "  \"chat_disabled\": \"Wyłączone\",\n" +
                "  \"chat_shop_item_added\": \"Przedmiot dodany do sklepu! Unikalne ID: {uniqueid}, Przedmiot: {itemid}, Ilość: {quantity}, Cena sprzedaży: {pricesell}, Cena kupna: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Przedmiot usunięty ze sklepu! Unikalne ID: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Ceny przedmiotu zaktualizowane! Unikalne ID: {uniqueid}, Przedmiot: {item}, Cena kupna: {pricebuy}, Cena sprzedaży: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Przedmiot nie znaleziony! Unikalne ID: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Błąd podczas aktualizacji przedmiotu: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Nieprawidłowe argumenty!\",\n" +
                "  \"chat_shop_item_bought\": \"Kupiłeś {quantity}x {item} za {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"Kupiłeś {added}x z {requested}x {item} (ekwipunek pełny)!\",\n" +
                "  \"chat_shop_item_sold\": \"Sprzedałeś {quantity}x {item} za {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"Nie masz wystarczającego salda! Wymagane: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"Nie masz wystarczającej ilości przedmiotów! Wymagane: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Przedmiot nie znaleziony! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"Zakładka '{tab}' utworzona pomyślnie!\",\n" +
                "  \"chat_shop_tab_removed\": \"Zakładka '{tab}' usunięta pomyślnie!\",\n" +
                "  \"chat_shop_tab_not_found\": \"Zakładka '{tab}' nie znaleziona!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"Zakładka '{tab}' już istnieje!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Osiągnięto maksimum 7 zakładek!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"Nazwa zakładki nie może być pusta!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Błąd tworzenia zakładki!\",\n" +
                "  \"chat_error_tab_create\": \"Błąd tworzenia zakładki: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Błąd usuwania zakładki: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Błąd usuwania zakładki!\",\n" +
                "  \"chat_playershop_disabled\": \"Sklepy graczy są wyłączone.\",\n" +
                "  \"chat_playershop_not_open\": \"Sklep tego gracza jest zamknięty.\",\n" +
                "  \"chat_playershop_empty\": \"Ten sklep jest pusty.\",\n" +
                "  \"chat_myshop_opened\": \"Twój sklep został otwarty!\",\n" +
                "  \"chat_myshop_closed\": \"Twój sklep został zamknięty!\",\n" +
                "  \"chat_myshop_icon_set\": \"Ikona sklepu ustawiona na: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Błąd ustawiania ikony sklepu.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Użycie: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Użycie: /myshop add <tab> <cenaKupna> <cenaSprzedaży>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Użycie: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Użycie: /myshop rename <nazwa>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Użycie: /myshop tab <create|remove> <nazwa>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Nie można usunąć zakładki '{tab}', ponieważ zawiera przedmioty. Najpierw usuń przedmioty.\",\n" +
                "  \"chat_myshop_renamed\": \"Sklep przemianowany na: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Błąd przemianowywania sklepu.\",\n" +
                "  \"chat_myshop_item_added\": \"Przedmiot dodany do twojego sklepu! Unikalne ID: {uniqueid}, Przedmiot: {item}, Ilość: {quantity}, Cena: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"Przedmiot usunięty z twojego sklepu! Unikalne ID: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Błąd usuwania przedmiotu z ekwipunku.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Ekwipunek pełny! Przedmiot nie został zwrócony.\",\n" +
                "  \"chat_myshop_not_owner\": \"Ten przedmiot nie należy do twojego sklepu.\",\n" +
                "  \"chat_playershop_item_bought\": \"Kupiłeś {quantity}x {item} za {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"Kupiłeś {added}x z {requested}x {item} (ekwipunek pełny)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"Niewystarczający stan magazynowy!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"Nie możesz kupować w swoim własnym sklepie!\",\n" +
                "  \"chat_shops_empty\": \"Brak otwartych sklepów w tej chwili.\",\n" +
                "  \"chat_error_inventory_add\": \"Błąd dodawania przedmiotu do ekwipunku. Pieniądze zwrócone.\",\n" +
                "  \"chat_error_inventory_remove\": \"Błąd usuwania przedmiotu z ekwipunku.\",\n" +
                "  \"chat_error_item_info\": \"Błąd pobierania informacji o przedmiocie: {error}\",\n" +
                "  \"chat_error_item_add\": \"Błąd dodawania przedmiotu: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Błąd usuwania przedmiotu: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Sprzedałeś {quantity}x {item} dla {player} za {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"Kupiłeś {quantity}x {item} od {player} za {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Błąd dostępu do ekwipunku.\",\n" +
                "  \"chat_iteminfo_no_item\": \"Nie trzymasz żadnego przedmiotu.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"Przedmiot nie ma prawidłowego ID.\",\n" +
                "  \"chat_iteminfo_info\": \"ID przedmiotu: {itemid} | Nazwa: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"ID przedmiotu nie może być puste!\",\n" +
                "  \"chat_hud_usage\": \"Użycie: /hud on lub /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD włączona!\",\n" +
                "  \"chat_hud_disabled\": \"HUD wyłączona!\",\n" +
                "  \"chat_hud_invalid_action\": \"Nieprawidłowa akcja! Użyj 'on' lub 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"HUD jest wyłączona na serwerze.\",\n" +
                "  \"chat_hud_error\": \"Błąd podczas zmiany preferencji HUD.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cBłąd pobierania pozycji gracza.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cBłąd pobierania UUID świata.\",\n" +
                "  \"chat_npc_created\": \"&aNPC sklepu utworzony na twojej pozycji! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aNPC sklepu utworzony na twojej pozycji! ID: {npcId} | Nazwa: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cNie znaleziono NPCs.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== NPCs Sklepu ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7ID: &f{npcId} &7| Świat: &f{worldId} &7| Pozycja: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cUżycie: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cUżyj /shop npc list aby zobaczyć Shop ID NPCs\",\n" +
                "  \"chat_npc_removed\": \"&aNPC usunięty pomyślnie!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC nie znaleziony!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cNieprawidłowy UUID!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cNieprawidłowy Shop ID! Użyj liczby.\",\n" +
                "  \"chat_npc_all_removed\": \"&aWszystkie NPCs zostały usunięte!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC pomyślnie przeniesiony na twoją pozycję!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cUżycie: /shop npc movehere <shopId>\",\n" +
                // ========== Opisy Poleceń ==========
                "  \"desc_shop\": \"Otwiera sklep\",\n" +
                "  \"desc_shop_add\": \"Dodaje przedmiot do sklepu\",\n" +
                "  \"desc_shop_remove\": \"Usuwa przedmiot ze sklepu\",\n" +
                "  \"desc_shop_tab\": \"Zarządza zakładkami sklepu\",\n" +
                "  \"desc_shop_tab_create\": \"Tworzy nową zakładkę w sklepie\",\n" +
                "  \"desc_shop_tab_remove\": \"Usuwa zakładkę ze sklepu i wszystkie jej przedmioty\",\n" +
                "  \"desc_shop_manager\": \"Otwiera interfejs zarządzania sklepem\",\n" +
                "  \"desc_shop_npc\": \"Zarządza NPC sklepu\",\n" +
                "  \"desc_shop_npc_add\": \"Tworzy NPC sklepu na twojej pozycji\",\n" +
                "  \"desc_shop_npc_remove\": \"Usuwa NPC sklepu według Shop ID\",\n" +
                "  \"desc_shop_npc_list\": \"Wyświetla listę wszystkich NPC sklepu\",\n" +
                "  \"desc_shop_npc_removeall\": \"Usuwa wszystkich NPC sklepu\",\n" +
                "  \"desc_shop_npc_movehere\": \"Przenosi NPC na twoją pozycję\",\n" +
                "  \"desc_shop_renameplayershop\": \"Zmienia nazwę sklepu gracza\",\n" +
                "  \"desc_money\": \"System ekonomiczny\",\n" +
                "  \"desc_money_pay\": \"Przekazuje pieniądze innemu graczowi\",\n" +
                "  \"desc_money_top\": \"Pokazuje top 10 najbogatszych graczy\",\n" +
                "  \"desc_money_set\": \"Ustawia saldo gracza\",\n" +
                "  \"desc_money_give\": \"Dodaje saldo graczowi\",\n" +
                "  \"desc_cash\": \"System gotówki\",\n" +
                "  \"desc_cash_give\": \"Dodaje gotówkę graczowi\",\n" +
                "  \"desc_shops\": \"Wyświetla listę wszystkich otwartych sklepów\",\n" +
                "  \"desc_playershop\": \"Otwiera sklep gracza\",\n" +
                "  \"desc_myshop\": \"Zarządza twoim osobistym sklepem\",\n" +
                "  \"desc_myshop_open\": \"Otwiera twój osobisty sklep\",\n" +
                "  \"desc_myshop_close\": \"Zamyka twój osobisty sklep\",\n" +
                "  \"desc_myshop_add\": \"Dodaje przedmiot do twojego sklepu\",\n" +
                "  \"desc_myshop_remove\": \"Usuwa przedmiot z twojego sklepu\",\n" +
                "  \"desc_myshop_rename\": \"Zmienia nazwę twojego sklepu\",\n" +
                "  \"desc_myshop_tab\": \"Zarządza zakładkami twojego sklepu\",\n" +
                "  \"desc_myshop_tab_create\": \"Tworzy nową zakładkę w twoim sklepie\",\n" +
                "  \"desc_myshop_tab_remove\": \"Usuwa zakładkę z twojego sklepu i wszystkie jej przedmioty\",\n" +
                "  \"desc_myshop_manager\": \"Zarządza twoim osobistym sklepem\",\n" +
                "  \"desc_iteminfo\": \"Pokazuje informacje o przedmiocie w ręce\",\n" +
                "  \"desc_hud\": \"Włącza/wyłącza HUD EconomySystem\",\n" +
                "  \"desc_hud_on\": \"Włącza HUD\",\n" +
                "  \"desc_hud_off\": \"Wyłącza HUD\",\n" +
                // ========== GUI (Teksty okien) ==========
                "  \"gui_shop_confirm_buy\": \"Czy chcesz kupić {quantity}x {item} za {price}?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Czy chcesz sprzedać {quantity}x {item} za {price}?\",\n" +
                "  \"gui_shop_quantity_label\": \"Ilość:\",\n" +
                "  \"gui_shop_button_confirm\": \"Potwierdź\",\n" +
                "  \"gui_shop_button_cancel\": \"Anuluj\",\n" +
                "  \"gui_shop_button_back\": \"Wstecz\",\n" +
                "  \"gui_shop_title\": \"Sklep Admina - The Economy\",\n" +
                "  \"gui_shop_npc_add_title\": \"Dodaj NPC Sklepu\",\n" +
                "  \"gui_shop_npc_add_name\": \"Nazwa NPC:\",\n" +
                "  \"gui_shop_npc_add_model\": \"Model NPC:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"Unikalne ID\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Ilość\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Kup\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Sprzedaj\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Lewy przycisk: Kup\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Prawy przycisk: Sprzedaj\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Prawy przycisk: Kup\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Lewy przycisk: Sprzedaj\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Środkowy przycisk: Usuń\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Kupno wyłączone\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Sprzedaż wyłączona\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Przedmiot niedostępny do kupna lub sprzedaży\",\n" +
                "  \"gui_shop_manager_title\": \"Zarządzaj sklepem\",\n" +
                "  \"gui_shop_manager_add_item\": \"Dodaj przedmiot\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Dodaj zakładkę\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Kliknij, aby usunąć przedmiot\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Prawy przycisk, aby edytować cenę\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Dodaj przedmiot: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Dodaj przedmiot\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Edytuj cenę: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Edytuj cenę przedmiotu\",\n" +
                "  \"gui_shop_manager_item_id\": \"ID przedmiotu:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"ID przedmiotu nie może być puste!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Ilość:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Cena kupna:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Cena sprzedaży:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Przedmioty z paska\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Dodaj nową zakładkę\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Nazwa zakładki:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Czy chcesz usunąć zakładkę '{tab}'? Wszystkie przedmioty zostaną usunięte.\",\n" +
                "  \"gui_myshop_manager_title\": \"Zarządzaj moim sklepem\",\n" +
                "  \"gui_myshop_rename_shop\": \"Przemianuj sklep\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Przemianuj sklep\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Nazwa sklepu:\",\n" +
                "  \"gui_myshop_open_shop\": \"Otwórz sklep\",\n" +
                "  \"gui_myshop_close_shop\": \"Zamknij sklep\",\n" +
                "  \"gui_myshop_set_icon\": \"Wybierz ikonę\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Czy chcesz usunąć przedmiot {item} (ID: {uniqueid}) ze swojego sklepu?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Stan magazynowy\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Czy chcesz kupić {quantity}x {item} za {price}?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Stan magazynowy\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Trwałość\",\n" +
                "  \"gui_shops_empty\": \"Brak otwartych sklepów w tej chwili.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Przedmioty\",\n" +
                "  \"gui_shops_tooltip_click\": \"Kliknij, aby otworzyć sklep\",\n" +
                "  \"gui_shops_player_shop\": \"Sklep {player}\",\n" +
                "  \"gui_shops_items_count\": \"przedmiotów\",\n" +
                "  \"gui_shop_empty\": \"Brak przedmiotów w sklepie\",\n" +
                "  \"gui_shops_title\": \"Przeglądasz sklepy graczy.\",\n" +
                "  \"gui_shops_title_player\": \"Sklepy graczy\",\n" +
                "  \"gui_playershop_title\": \"Ten sklep należy do gracza {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"K: \",\n" +
                "  \"gui_shop_price_sell_label\": \"S: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Stan: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Kliknij, aby usunąć przedmiot\",\n" +
                "  \"gui_shop_manager_add_console\": \"Dodaj Console\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Dodaj Polecenie Console\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Dodaj Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Dodaj Przedmiot Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Dodaj Przedmiot Cash\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Typ Płatności:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Pieniądze\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Cena (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Czy chcesz kupić {quantity}x {item} za {price} Cash?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"Nie masz wystarczająco Cash! Wymagane: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Przedmiot Cash dodany do sklepu! Unikalne ID: {uniqueid}, Przedmiot: {item}, Ilość: {quantity}, Cena: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Nazwa:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Polecenie:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Ikona (Przedmiot z Hotbar)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Polecenie Console\",\n" +
                "  \"chat_shop_console_added\": \"Polecenie console dodane! Nazwa: {name}, Polecenie: {command}, Cena: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"Kupiłeś {quantity}x {item} za {price}!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"Nazwa nie może być pusta!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"Polecenie nie może być puste!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Wybierz ikonę z hotbar!\",\n" +
                "  \"chat_error_console_add\": \"Błąd podczas dodawania polecenia konsoli: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Użycie: /shop renameplayershop <nick> <nazwa>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Sklep przemianowany pomyślnie! Gracz: {player}, Nowa nazwa: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Błąd przemianowywania sklepu gracza.\",\n" +
                "  \"npc_interaction_open_shop\": \"Naciśnij F, aby otworzyć sklep\",\n" +
                // ========== HUD (Teksty HUD) ==========
                "  \"hud_nick\": \"&l&6Nick&r:\",\n" +
                "  \"hud_money\": \"&l&6Pieniądze&r:\",\n" +
                "  \"hud_cash\": \"&l&6Gotówka&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Top Rank&r:\",\n" +
                "  \"hud_rank\": \"Ranking\",\n" +
                "  \"hud_shop_status\": \"&l&6Sklep\",\n" +
                "  \"hud_shop_open\": \"&aOtwarty\",\n" +
                "  \"hud_shop_closed\": \"&cZamknięty\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aZarobiłeś &l{amount}\"\n" +
                "}";
    }

    private static String getDefaultDEJson() {
        return "{\n" +
                // ========== CHAT (Chat-Nachrichten) ==========
                "  \"chat_insufficient_balance\": \"Du hast nicht genug Guthaben!\",\n" +
                "  \"chat_player_not_found\": \"Spieler nicht gefunden!\",\n" +
                "  \"chat_invalid_amount\": \"Ungültiger Betrag!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"Du kannst dir nicht selbst Geld zahlen!\",\n" +
                "  \"chat_payment_sent\": \"Zahlung erfolgreich gesendet!\",\n" +
                "  \"chat_payment_received\": \"Du hast eine Zahlung erhalten!\",\n" +
                "  \"chat_balance_set\": \"Guthaben erfolgreich festgelegt!\",\n" +
                "  \"chat_balance_added\": \"Guthaben erfolgreich hinzugefügt!\",\n" +
                "  \"chat_usage_money\": \"Verwendung: /money [Spielername]\",\n" +
                "  \"chat_usage_pay\": \"Verwendung: /money pay <Spielername> <Betrag>\",\n" +
                "  \"chat_usage_set\": \"Verwendung: /money set <Spielername> <Betrag>\",\n" +
                "  \"chat_usage_give\": \"Verwendung: /money give <Spielername> <Betrag>\",\n" +
                "  \"chat_no_permission\": \"Du hast keine Berechtigung für diesen Befehl!\",\n" +
                "  \"chat_balance_of\": \"Guthaben von {player}\",\n" +
                "  \"chat_top_10_richest\": \"Top 10 Reichste\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"Keine Spieler in der Rangliste gefunden.\",\n" +
                "  \"chat_money_top_disabled\": \"Geld-Rangliste ist deaktiviert.\",\n" +
                "  \"chat_top_position\": \"{position} {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Guthaben von {player} auf {amount} gesetzt\",\n" +
                "  \"chat_balance_added_to\": \"{amount} zu {player} hinzugefügt. Neues Guthaben: {balance}\",\n" +
                "  \"chat_money_received\": \"Du hast {amount} erhalten.\",\n" +
                "  \"chat_money_received_from\": \"Du hast {amount} von {player} erhalten.\",\n" +
                "  \"chat_cash_of\": \"Bargeld von {player}\",\n" +
                "  \"chat_cash_added\": \"Bargeld erfolgreich hinzugefügt!\",\n" +
                "  \"chat_cash_added_to\": \"{amount} Bargeld zu {player} hinzugefügt. Neues Bargeld: {cash}\",\n" +
                "  \"chat_cash_received\": \"Du hast {amount} Bargeld erhalten.\",\n" +
                "  \"chat_usage_cash_give\": \"Verwendung: /cash give <nick> <betrag>\",\n" +
                "  \"chat_balance_set_notification\": \"Dein Guthaben wurde auf {amount} gesetzt.\",\n" +
                "  \"chat_plugin_loaded\": \"EconomySystem erfolgreich geladen!\",\n" +
                "  \"chat_ore_rewards\": \"Erz-Belohnungen: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Holz-Belohnungen: {status}\",\n" +
                "  \"chat_ores_configured\": \"Konfigurierte Erze: {count} Gegenstände\",\n" +
                "  \"chat_api_available\": \"Öffentliche API verfügbar: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Lade Guthabendaten...\",\n" +
                "  \"chat_error_loading_balances\": \"FEHLER BEIM LADEN DER GUTHABENDATEI\",\n" +
                "  \"chat_error_saving_balances\": \"FEHLER BEIM SPEICHERN DER GUTHABENDATEI\",\n" +
                "  \"chat_balances_saved\": \"Wirtschaftsdaten gespeichert\",\n" +
                "  \"chat_initial_balance_given\": \"Startguthaben von ${amount} an Spieler gegeben: {player}\",\n" +
                "  \"chat_enabled\": \"Aktiviert\",\n" +
                "  \"chat_disabled\": \"Deaktiviert\",\n" +
                "  \"chat_shop_item_added\": \"Gegenstand zum Shop hinzugefügt! Eindeutige ID: {uniqueid}, Gegenstand: {itemid}, Menge: {quantity}, Verkaufspreis: {pricesell}, Kaufpreis: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Gegenstand aus Shop entfernt! Eindeutige ID: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Gegenstandspreise aktualisiert! Eindeutige ID: {uniqueid}, Gegenstand: {item}, Kaufpreis: {pricebuy}, Verkaufspreis: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Gegenstand nicht gefunden! Eindeutige ID: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Fehler beim Aktualisieren des Gegenstands: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Ungültige Argumente!\",\n" +
                "  \"chat_shop_item_bought\": \"Du hast {quantity}x {item} für {price} gekauft!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"Du hast {added}x von {requested}x {item} gekauft (Inventar voll)!\",\n" +
                "  \"chat_shop_item_sold\": \"Du hast {quantity}x {item} für {price} verkauft!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"Du hast nicht genug Guthaben! Benötigt: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"Du hast nicht genug Gegenstände! Benötigt: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Gegenstand nicht gefunden! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"Tab '{tab}' erfolgreich erstellt!\",\n" +
                "  \"chat_shop_tab_removed\": \"Tab '{tab}' erfolgreich entfernt!\",\n" +
                "  \"chat_shop_tab_not_found\": \"Tab '{tab}' nicht gefunden!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"Tab '{tab}' existiert bereits!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Maximum von 7 Tabs erreicht!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"Tab-Name darf nicht leer sein!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Fehler beim Erstellen des Tabs!\",\n" +
                "  \"chat_error_tab_create\": \"Fehler beim Erstellen des Tabs: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Fehler beim Entfernen des Tabs: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Fehler beim Entfernen des Tabs!\",\n" +
                "  \"chat_playershop_disabled\": \"Spieler-Shops sind deaktiviert.\",\n" +
                "  \"chat_playershop_not_open\": \"Der Shop dieses Spielers ist geschlossen.\",\n" +
                "  \"chat_playershop_empty\": \"Dieser Shop ist leer.\",\n" +
                "  \"chat_myshop_opened\": \"Dein Shop wurde geöffnet!\",\n" +
                "  \"chat_myshop_closed\": \"Dein Shop wurde geschlossen!\",\n" +
                "  \"chat_myshop_icon_set\": \"Shop-Icon gesetzt auf: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Fehler beim Setzen des Shop-Icons.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Verwendung: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Verwendung: /myshop add <tab> <priceBuy> <priceSell>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Verwendung: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Verwendung: /myshop rename <name>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Verwendung: /myshop tab <create|remove> <name>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Tab '{tab}' kann nicht entfernt werden, da er Gegenstände enthält. Entferne zuerst die Gegenstände.\",\n" +
                "  \"chat_myshop_renamed\": \"Shop umbenannt in: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Fehler beim Umbenennen des Shops.\",\n" +
                "  \"chat_myshop_item_added\": \"Gegenstand zu deinem Shop hinzugefügt! Eindeutige ID: {uniqueid}, Gegenstand: {item}, Menge: {quantity}, Preis: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"Gegenstand aus deinem Shop entfernt! Eindeutige ID: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Fehler beim Entfernen des Gegenstands aus dem Inventar.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Inventar voll! Gegenstand wurde nicht zurückgegeben.\",\n" +
                "  \"chat_myshop_not_owner\": \"Dieser Gegenstand gehört nicht zu deinem Shop.\",\n" +
                "  \"chat_playershop_item_bought\": \"Du hast {quantity}x {item} für {price} gekauft!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"Du hast {added}x von {requested}x {item} gekauft (Inventar voll)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"Nicht genug Vorrat!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"Du kannst nicht aus deinem eigenen Shop kaufen!\",\n" +
                "  \"chat_shops_empty\": \"Derzeit sind keine Shops geöffnet.\",\n" +
                "  \"chat_error_inventory_add\": \"Fehler beim Hinzufügen des Gegenstands zum Inventar. Geld wurde zurückerstattet.\",\n" +
                "  \"chat_error_inventory_remove\": \"Fehler beim Entfernen des Gegenstands aus dem Inventar.\",\n" +
                "  \"chat_error_item_info\": \"Fehler beim Abrufen der Gegenstandsinformationen: {error}\",\n" +
                "  \"chat_error_item_add\": \"Fehler beim Hinzufügen des Gegenstands: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Fehler beim Entfernen des Gegenstands: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Du hast {quantity}x {item} an {player} für {money} verkauft\",\n" +
                "  \"chat_playershop_owner_bought\": \"Du hast {quantity}x {item} von {player} für {money} gekauft\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Fehler beim Zugriff auf das Inventar.\",\n" +
                "  \"chat_iteminfo_no_item\": \"Du hältst keinen Gegenstand in der Hand.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"Der Gegenstand hat keine gültige ID.\",\n" +
                "  \"chat_iteminfo_info\": \"Gegenstand ID: {itemid} | Name: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"Gegenstand-ID darf nicht leer sein!\",\n" +
                "  \"chat_hud_usage\": \"Verwendung: /hud on oder /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD aktiviert!\",\n" +
                "  \"chat_hud_disabled\": \"HUD deaktiviert!\",\n" +
                "  \"chat_hud_invalid_action\": \"Ungültige Aktion! Verwende 'on' oder 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"HUD ist auf dem Server deaktiviert.\",\n" +
                "  \"chat_hud_error\": \"Fehler beim Ändern der HUD-Einstellung.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cFehler beim Abrufen der Spielerposition.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cFehler beim Abrufen der Welt-UUID.\",\n" +
                "  \"chat_npc_created\": \"&aShop-NPC an deiner Position erstellt! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aShop-NPC an deiner Position erstellt! ID: {npcId} | Name: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cKeine NPCs gefunden.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== Shop-NPCs ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7Shop ID: &f{shopId} &7| NPC ID: &f{npcId} &7| Welt: &f{worldId} &7| Pos: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cVerwendung: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cVerwende /shop npc list, um Shop-IDs anzuzeigen\",\n" +
                "  \"chat_npc_removed\": \"&aNPC erfolgreich entfernt!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC nicht gefunden!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cUngültige UUID!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cUngültige Shop-ID! Verwende eine Zahl.\",\n" +
                "  \"chat_npc_all_removed\": \"&aAlle NPCs wurden entfernt!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC erfolgreich an deine Position verschoben!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cVerwendung: /shop npc movehere <shopId>\",\n" +
                // ========== Befehlsbeschreibungen ==========
                "  \"desc_shop\": \"Öffnet den Shop\",\n" +
                "  \"desc_shop_add\": \"Fügt einen Gegenstand zum Shop hinzu\",\n" +
                "  \"desc_shop_remove\": \"Entfernt einen Gegenstand aus dem Shop\",\n" +
                "  \"desc_shop_tab\": \"Verwaltet Shop-Tabs\",\n" +
                "  \"desc_shop_tab_create\": \"Erstellt einen neuen Tab im Shop\",\n" +
                "  \"desc_shop_tab_remove\": \"Entfernt einen Tab aus dem Shop und alle seine Gegenstände\",\n" +
                "  \"desc_shop_manager\": \"Öffnet die Shop-Verwaltungsoberfläche\",\n" +
                "  \"desc_shop_npc\": \"Verwaltet den Shop-NPC\",\n" +
                "  \"desc_shop_npc_add\": \"Erstellt den Shop-NPC an deiner Position\",\n" +
                "  \"desc_shop_npc_remove\": \"Entfernt einen Shop-NPC nach Shop-ID\",\n" +
                "  \"desc_shop_npc_list\": \"Listet alle Shop-NPCs auf\",\n" +
                "  \"desc_shop_npc_removeall\": \"Entfernt alle Shop-NPCs\",\n" +
                "  \"desc_shop_npc_movehere\": \"Verschiebt den NPC an deine Position\",\n" +
                "  \"desc_shop_renameplayershop\": \"Benennt den Shop eines Spielers um\",\n" +
                "  \"desc_money\": \"Wirtschaftssystem\",\n" +
                "  \"desc_money_pay\": \"Überweist Geld an einen anderen Spieler\",\n" +
                "  \"desc_money_top\": \"Zeigt die Top 10 reichsten Spieler\",\n" +
                "  \"desc_money_set\": \"Setzt das Guthaben eines Spielers\",\n" +
                "  \"desc_money_give\": \"Fügt einem Spieler Guthaben hinzu\",\n" +
                "  \"desc_cash\": \"Bargeld-System\",\n" +
                "  \"desc_cash_give\": \"Fügt einem Spieler Bargeld hinzu\",\n" +
                "  \"desc_shops\": \"Listet alle geöffneten Shops auf\",\n" +
                "  \"desc_playershop\": \"Öffnet den Shop eines Spielers\",\n" +
                "  \"desc_myshop\": \"Verwaltet deinen persönlichen Shop\",\n" +
                "  \"desc_myshop_open\": \"Öffnet deinen persönlichen Shop\",\n" +
                "  \"desc_myshop_close\": \"Schließt deinen persönlichen Shop\",\n" +
                "  \"desc_myshop_add\": \"Fügt einen Gegenstand zu deinem Shop hinzu\",\n" +
                "  \"desc_myshop_remove\": \"Entfernt einen Gegenstand aus deinem Shop\",\n" +
                "  \"desc_myshop_rename\": \"Benennt deinen Shop um\",\n" +
                "  \"desc_myshop_tab\": \"Verwaltet die Tabs deines Shops\",\n" +
                "  \"desc_myshop_tab_create\": \"Erstellt einen neuen Tab in deinem Shop\",\n" +
                "  \"desc_myshop_tab_remove\": \"Entfernt einen Tab aus deinem Shop und alle seine Gegenstände\",\n" +
                "  \"desc_myshop_manager\": \"Verwaltet deinen persönlichen Shop\",\n" +
                "  \"desc_iteminfo\": \"Zeigt Informationen über den Gegenstand in der Hand\",\n" +
                "  \"desc_hud\": \"Aktiviert/deaktiviert die EconomySystem-HUD\",\n" +
                "  \"desc_hud_on\": \"Aktiviert die HUD\",\n" +
                "  \"desc_hud_off\": \"Deaktiviert die HUD\",\n" +
                // ========== GUI (Fenstertexte) ==========
                "  \"gui_shop_confirm_buy\": \"Möchtest du {quantity}x {item} für {price} kaufen?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Möchtest du {quantity}x {item} für {price} verkaufen?\",\n" +
                "  \"gui_shop_quantity_label\": \"Menge:\",\n" +
                "  \"gui_shop_button_confirm\": \"Bestätigen\",\n" +
                "  \"gui_shop_button_cancel\": \"Abbrechen\",\n" +
                "  \"gui_shop_button_back\": \"Zurück\",\n" +
                "  \"gui_shop_title\": \"Admin Shop - Die Wirtschaft\",\n" +
                "  \"gui_shop_npc_add_title\": \"Shop-NPC hinzufügen\",\n" +
                "  \"gui_shop_npc_add_name\": \"NPC-Name:\",\n" +
                "  \"gui_shop_npc_add_model\": \"NPC-Modell:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"Eindeutige ID\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Menge\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Kaufen\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Verkaufen\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Linksklick: Kaufen\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Rechtsklick: Verkaufen\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Rechtsklick: Kaufen\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Linksklick: Verkaufen\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Mittelklick zum Entfernen\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Kaufen deaktiviert\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Verkaufen deaktiviert\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Gegenstand nicht zum Kauf oder Verkauf verfügbar\",\n" +
                "  \"gui_shop_manager_title\": \"Shop verwalten\",\n" +
                "  \"gui_shop_manager_add_item\": \"Gegenstand hinzufügen\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Tab hinzufügen\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Klicken zum Entfernen des Gegenstands\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Rechtsklick zum Bearbeiten des Preises\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Gegenstand hinzufügen: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Gegenstand hinzufügen\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Preis bearbeiten: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Gegenstandspreis bearbeiten\",\n" +
                "  \"gui_shop_manager_item_id\": \"Gegenstand-ID:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"Gegenstand-ID darf nicht leer sein!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Menge:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Kaufpreis:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Verkaufspreis:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Hotbar-Gegenstände\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Neuen Tab hinzufügen\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Tab-Name:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Möchtest du den Tab '{tab}' entfernen? Alle Gegenstände werden entfernt.\",\n" +
                "  \"gui_myshop_manager_title\": \"Meinen Shop verwalten\",\n" +
                "  \"gui_myshop_rename_shop\": \"Shop umbenennen\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Shop umbenennen\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Shop-Name:\",\n" +
                "  \"gui_myshop_open_shop\": \"Shop öffnen\",\n" +
                "  \"gui_myshop_close_shop\": \"Shop schließen\",\n" +
                "  \"gui_myshop_set_icon\": \"Icon wählen\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Möchtest du den Gegenstand {item} (ID: {uniqueid}) aus deinem Shop entfernen?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Vorrat\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Möchtest du {quantity}x {item} für {price} kaufen?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Vorrat\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Haltbarkeit\",\n" +
                "  \"gui_shops_empty\": \"Derzeit sind keine Shops geöffnet.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Gegenstände\",\n" +
                "  \"gui_shops_tooltip_click\": \"Klicken zum Öffnen des Shops\",\n" +
                "  \"gui_shops_player_shop\": \"{player}s Shop\",\n" +
                "  \"gui_shops_items_count\": \"Gegenstände\",\n" +
                "  \"gui_shop_empty\": \"Keine Gegenstände im Shop\",\n" +
                "  \"gui_shops_title\": \"Du siehst Spieler-Shops.\",\n" +
                "  \"gui_shops_title_player\": \"Spieler-Shops\",\n" +
                "  \"gui_playershop_title\": \"Dieser Shop gehört dem Spieler {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"K: \",\n" +
                "  \"gui_shop_price_sell_label\": \"V: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Vorrat: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Klicken zum Entfernen des Gegenstands\",\n" +
                "  \"gui_shop_manager_add_console\": \"Console hinzufügen\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Console-Befehl hinzufügen\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Cash hinzufügen\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Cash Gegenstand hinzufügen\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Cash Gegenstand hinzufügen\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Zahlungsart:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Geld\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Preis (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Möchten Sie {quantity}x {item} für {price} Cash kaufen?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"Sie haben nicht genug Cash! Erforderlich: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Cash Gegenstand zum Shop hinzugefügt! Eindeutige ID: {uniqueid}, Gegenstand: {item}, Menge: {quantity}, Preis: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Name:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Befehl:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Symbol (Hotbar-Gegenstand)\",\n" +
                "  \"chat_shop_console_added\": \"Console-Befehl hinzugefügt! Name: {name}, Befehl: {command}, Preis: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"Du hast {quantity}x {item} für {price} gekauft!\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Console-Befehl\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"Der Name darf nicht leer sein!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"Der Befehl darf nicht leer sein!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Wähle ein Symbol aus der Hotbar!\",\n" +
                "  \"chat_error_console_add\": \"Fehler beim Hinzufügen des Console-Befehls: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Verwendung: /shop renameplayershop <Spielername> <Name>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Shop erfolgreich umbenannt! Spieler: {player}, Neuer Name: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Fehler beim Umbenennen des Spieler-Shops.\",\n" +
                "  \"npc_interaction_open_shop\": \"Drücke F, um den Shop zu öffnen\",\n" +
                // ========== HUD (HUD-Texte) ==========
                "  \"hud_nick\": \"&l&6Name&r:\",\n" +
                "  \"hud_money\": \"&l&6Geld&r:\",\n" +
                "  \"hud_cash\": \"&l&6Bargeld&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Top-Rang&r:\",\n" +
                "  \"hud_rank\": \"Rang\",\n" +
                "  \"hud_shop_status\": \"&l&6Shop\",\n" +
                "  \"hud_shop_open\": \"&aGeöffnet\",\n" +
                "  \"hud_shop_closed\": \"&cGeschlossen\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aDu hast &l{amount}&a verdient\"\n" +
                "}";
    }

    private static String getDefaultHUJson() {
        return "{\n" +
                // ========== CHAT (Chat üzenetek) ==========
                "  \"chat_insufficient_balance\": \"Nincs elég egyenleged!\",\n" +
                "  \"chat_player_not_found\": \"Játékos nem található!\",\n" +
                "  \"chat_invalid_amount\": \"Érvénytelen összeg!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"Saját magadnak nem fizethetsz!\",\n" +
                "  \"chat_payment_sent\": \"Sikeres utalás!\",\n" +
                "  \"chat_payment_received\": \"Pénzt kaptál!\",\n" +
                "  \"chat_balance_set\": \"Egyenleg sikeresen beállitva!\",\n" +
                "  \"chat_balance_added\": \"Összeg sikeresen hozzáadva!\",\n" +
                "  \"chat_usage_money\": \"Használat: /money [név]\",\n" +
                "  \"chat_usage_pay\": \"Használat: /money pay <név> <összeg>\",\n" +
                "  \"chat_usage_set\": \"Használat: /money set <név> <összeg>\",\n" +
                "  \"chat_usage_give\": \"Használat: /money give <név> <összeg>\",\n" +
                "  \"chat_no_permission\": \"Nincs jogosultságod a parancs használatához!\",\n" +
                "  \"chat_balance_of\": \"{player} egyenlege\",\n" +
                "  \"chat_top_10_richest\": \"A 10 leggazdagabb kalandor\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"Nem találhatók játékosok a rangsorban.\",\n" +
                "  \"chat_money_top_disabled\": \"A pénz rangsor le van tiltva.\",\n" +
                "  \"chat_top_position\": \"{position} {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"{player} egyenlege módositva: {amount}\",\n" +
                "  \"chat_balance_added_to\": \"{amount} hozzáadva {player} számlájához. Új egyenleg: {balance}\",\n" +
                "  \"chat_money_received\": \"Kaptál {amount} összeget.\",\n" +
                "  \"chat_money_received_from\": \"Kaptál {amount} összeget töle: {player}.\",\n" +
                "  \"chat_cash_of\": \"Készpénz {player}\",\n" +
                "  \"chat_cash_added\": \"Készpénz sikeresen hozzáadva!\",\n" +
                "  \"chat_cash_added_to\": \"{amount} készpénz hozzáadva {player} számlájához. Új készpénz: {cash}\",\n" +
                "  \"chat_cash_received\": \"Kaptál {amount} készpénzt.\",\n" +
                "  \"chat_usage_cash_give\": \"Használat: /cash give <nick> <összeg>\",\n" +
                "  \"chat_balance_set_notification\": \"Az egyenleged módositva lett: {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"EconomySystem sikeresen betöltve!\",\n" +
                "  \"chat_ore_rewards\": \"Érc jutalmak: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Fa jutalmak: {status}\",\n" +
                "  \"chat_ores_configured\": \"Beállitott ércek: {count} típus\",\n" +
                "  \"chat_api_available\": \"Publikus API elérhetö: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Egyenlegek betöltése...\",\n" +
                "  \"chat_error_loading_balances\": \"HIBA AZ EGYENLEGEK BETÖLTÉSEKOR\",\n" +
                "  \"chat_error_saving_balances\": \"HIBA AZ EGYENLEGEK MENTÉSEKOR\",\n" +
                "  \"chat_balances_saved\": \"Gazdasági adatok mentve\",\n" +
                "  \"chat_initial_balance_given\": \"Kezdötöke ({amount}) kiosztva: {player}\",\n" +
                "  \"chat_enabled\": \"Bekapcsolva\",\n" +
                "  \"chat_disabled\": \"Kikapcsolva\",\n" +
                "  \"chat_shop_item_added\": \"Tárgy hozzáadva a bolthoz! ID: {uniqueid}, Tárgy: {itemid}, Mennyiség: {quantity}, Eladási ár: {pricesell}, Vételi ár: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Tárgy eltávolitva a boltból! ID: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Tárgy árai frissítve! ID: {uniqueid}, Tárgy: {item}, Vételi ár: {pricebuy}, Eladási ár: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Tárgy nem található! ID: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Hiba a tárgy frissítésekor: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Érvénytelen paraméterek!\",\n" +
                "  \"chat_shop_item_bought\": \"Vettél {quantity}x {item}-t ennyiért: {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"Vettél {added}x-et a kért {requested}x {item} tárgyból (tele a táskád)!\",\n" +
                "  \"chat_shop_item_sold\": \"Eladtál {quantity}x {item}-t ennyiért: {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"Nincs elég pénzed! Szükséges: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"Nincs elég tárgyad! Szükséges: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Tárgy nem található! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"'{tab}' fül sikeresen létrehozva!\",\n" +
                "  \"chat_shop_tab_removed\": \"'{tab}' fül eltávolitva!\",\n" +
                "  \"chat_shop_tab_not_found\": \"'{tab}' fül nem található!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"'{tab}' fül már létezik!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Maximum 7 fül engedélyezett!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"A fül neve nem lehet üres!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Hiba a fül létrehozásakor!\",\n" +
                "  \"chat_error_tab_create\": \"Hiba a fül létrehozásakor: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Hiba a fül eltávolitásakor: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Hiba a fül törlésekor!\",\n" +
                "  \"chat_playershop_disabled\": \"A játékos boltok ki vannak kapcsolva.\",\n" +
                "  \"chat_playershop_not_open\": \"Ez a bolt jelenleg zárva van.\",\n" +
                "  \"chat_playershop_empty\": \"Ez a bolt üres.\",\n" +
                "  \"chat_myshop_opened\": \"A boltod kinyitott!\",\n" +
                "  \"chat_myshop_closed\": \"A boltod bezárt!\",\n" +
                "  \"chat_myshop_icon_set\": \"Bolt ikonja beállitva: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Hiba az ikon beállitásakor.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Használat: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Használat: /myshop add <fül> <vételiÁr> <eladásiÁr>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Használat: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Használat: /myshop rename <név>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Használat: /myshop tab <create|remove> <név>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Nem törölheted a(z) '{tab}' fület, mert tárgyakat tartalmaz. Elöbb üritsd ki!\",\n" +
                "  \"chat_myshop_renamed\": \"Bolt új neve: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Hiba a bolt átnevezésekor.\",\n" +
                "  \"chat_myshop_item_added\": \"Tárgy hozzáadva a boltodhoz! ID: {uniqueid}, Tárgy: {item}, Mennyiség: {quantity}, Ár: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"Tárgy eltávolitva a boltodból! ID: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Hiba a tárgy eltávolitásakor az inventoryból.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Az inventoryd tele van! A tárgy nem került vissza.\",\n" +
                "  \"chat_myshop_not_owner\": \"Ez a tárgy nem a te boltodhoz tartozik.\",\n" +
                "  \"chat_playershop_item_bought\": \"Vettél {quantity}x {item}-t ennyiért: {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"Vettél {added}x-et a kért {requested}x {item} tárgyból (tele a táskád)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"Nincs elég készlet!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"Nem vásárolhatsz a saját boltodban!\",\n" +
                "  \"chat_shops_empty\": \"Jelenleg nincs nyitva tartó bolt.\",\n" +
                "  \"chat_error_inventory_add\": \"Hiba a tárgy hozzáadásakor. A pénz visszajár.\",\n" +
                "  \"chat_error_inventory_remove\": \"Hiba a tárgy eltávolitásakor.\",\n" +
                "  \"chat_error_item_info\": \"Hiba a tárgy információinak lekérésekor: {error}\",\n" +
                "  \"chat_error_item_add\": \"Hiba a tárgy hozzáadásakor: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Hiba a tárgy eltávolitásakor: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Eladtál {quantity}x {item}-t {player} részére ennyiért: {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"Vettél {quantity}x {item}-t {player} játékostól ennyiért: {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Hiba az inventory elérésekor.\",\n" +
                "  \"chat_iteminfo_no_item\": \"Nincs semmi a kezedben.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"A tárgynak nincs érvényes azonositója.\",\n" +
                "  \"chat_iteminfo_info\": \"Tárgy ID: {itemid} | Név: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"A tárgy ID nem lehet üres!\",\n" +
                "  \"chat_hud_usage\": \"Használat: /hud on vagy /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD bekapcsolva!\",\n" +
                "  \"chat_hud_disabled\": \"HUD kikapcsolva!\",\n" +
                "  \"chat_hud_invalid_action\": \"Érvénytelen művelet! Használd az 'on' vagy 'off' szót.\",\n" +
                "  \"chat_hud_server_disabled\": \"A HUD le van tiltva ezen a szerveren.\",\n" +
                "  \"chat_hud_error\": \"Hiba a HUD beállitásakor.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cHiba a játékos poziciójának lekérésekor.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cHiba a világ azonositójának lekérésekor.\",\n" +
                "  \"chat_npc_created\": \"&aBoltos NPC létrehozva! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aBoltos NPC létrehozva! ID: {npcId} | Név: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cNem található NPC.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== Boltos NPC-k ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7ID: &f{npcId} &7| Világ: &f{worldId} &7| Poz: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cHasználat: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cHasználd a /shop npc list parancsot a Shop ID-k megtekintéséhez\",\n" +
                "  \"chat_npc_removed\": \"&aNPC sikeresen eltávolitva!\",\n" +
                "  \"chat_npc_not_found\": \"&cNPC nem található!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cÉrvénytelen UUID!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cÉrvénytelen Shop ID! Használj számot.\",\n" +
                "  \"chat_npc_all_removed\": \"&aMinden NPC eltávolitva!\",\n" +
                "  \"chat_npc_moved\": \"&aNPC sikeresen áthelyezve a pozíciódra!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cHasználat: /shop npc movehere <shopId>\",\n" +
                // ========== Parancs Leírások ==========
                "  \"desc_shop\": \"Megnyitja a boltot\",\n" +
                "  \"desc_shop_add\": \"Hozzáad egy tárgyat a bolthoz\",\n" +
                "  \"desc_shop_remove\": \"Eltávolit egy tárgyat a boltból\",\n" +
                "  \"desc_shop_tab\": \"Kezeli a bolt füleit\",\n" +
                "  \"desc_shop_tab_create\": \"Új fület hoz létre a boltban\",\n" +
                "  \"desc_shop_tab_remove\": \"Eltávolit egy fület a boltból és minden tárgyát\",\n" +
                "  \"desc_shop_manager\": \"Megnyitja a bolt kezelőfelületét\",\n" +
                "  \"desc_shop_npc\": \"Kezeli a bolt NPC-jét\",\n" +
                "  \"desc_shop_npc_add\": \"Létrehozza a bolt NPC-jét a pozíciódban\",\n" +
                "  \"desc_shop_npc_remove\": \"Eltávolit egy bolt NPC-t Shop ID alapján\",\n" +
                "  \"desc_shop_npc_list\": \"Listázza az összes bolt NPC-t\",\n" +
                "  \"desc_shop_npc_removeall\": \"Eltávolitja az összes bolt NPC-t\",\n" +
                "  \"desc_shop_npc_movehere\": \"Áthelyezi az NPC-t a pozíciódra\",\n" +
                "  \"desc_shop_renameplayershop\": \"Átnevezi egy játékos boltját\",\n" +
                "  \"desc_money\": \"Gazdasági rendszer\",\n" +
                "  \"desc_money_pay\": \"Pénzt utal egy másik játékosnak\",\n" +
                "  \"desc_money_top\": \"Megmutatja a top 10 leggazdagabb játékost\",\n" +
                "  \"desc_money_set\": \"Beállitja egy játékos egyenlegét\",\n" +
                "  \"desc_money_give\": \"Hozzáad egyenleget egy játékoshoz\",\n" +
                "  \"desc_cash\": \"Készpénz rendszer\",\n" +
                "  \"desc_cash_give\": \"Hozzáad készpénzt egy játékoshoz\",\n" +
                "  \"desc_shops\": \"Listázza az összes nyitott boltot\",\n" +
                "  \"desc_playershop\": \"Megnyitja egy játékos boltját\",\n" +
                "  \"desc_myshop\": \"Kezeli a személyes boltodat\",\n" +
                "  \"desc_myshop_open\": \"Megnyitja a személyes boltodat\",\n" +
                "  \"desc_myshop_close\": \"Bezárja a személyes boltodat\",\n" +
                "  \"desc_myshop_add\": \"Hozzáad egy tárgyat a boltodhoz\",\n" +
                "  \"desc_myshop_remove\": \"Eltávolit egy tárgyat a boltodból\",\n" +
                "  \"desc_myshop_rename\": \"Átnevezi a boltodat\",\n" +
                "  \"desc_myshop_tab\": \"Kezeli a boltod füleit\",\n" +
                "  \"desc_myshop_tab_create\": \"Új fület hoz létre a boltodban\",\n" +
                "  \"desc_myshop_tab_remove\": \"Eltávolit egy fület a boltodból és minden tárgyát\",\n" +
                "  \"desc_myshop_manager\": \"Kezeli a személyes boltodat\",\n" +
                "  \"desc_iteminfo\": \"Információkat mutat a kezedben lévő tárgyról\",\n" +
                "  \"desc_hud\": \"Bekapcsolja/kikapcsolja az EconomySystem HUD-ot\",\n" +
                "  \"desc_hud_on\": \"Bekapcsolja a HUD-ot\",\n" +
                "  \"desc_hud_off\": \"Kikapcsolja a HUD-ot\",\n" +
                // ========== GUI (Ablak szövegek) ==========
                "  \"gui_shop_confirm_buy\": \"Szeretnél venni {quantity}x {item}-t {price} áron?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Szeretnél eladni {quantity}x {item}-t {price} áron?\",\n" +
                "  \"gui_shop_quantity_label\": \"Mennyiség:\",\n" +
                "  \"gui_shop_button_confirm\": \"Megerösités\",\n" +
                "  \"gui_shop_button_cancel\": \"Mégse\",\n" +
                "  \"gui_shop_button_back\": \"Vissza\",\n" +
                "  \"gui_shop_title\": \"Admin Bolt - Gazdaság\",\n" +
                "  \"gui_shop_npc_add_title\": \"Bolt NPC hozzáadása\",\n" +
                "  \"gui_shop_npc_add_name\": \"NPC neve:\",\n" +
                "  \"gui_shop_npc_add_model\": \"NPC modell:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"Egyedi ID\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Mennyiség\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Vásárlás\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Eladás\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Bal klikk: Vásárlás\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Jobb klikk: Eladás\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Jobb klikk: Vásárlás\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Bal klikk: Eladás\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Görgö kattintás: Törlés\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Vásárlás letiltva\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Eladás letiltva\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Ez a tárgy nem vehetö meg és nem adható el\",\n" +
                "  \"gui_shop_manager_title\": \"Bolt Kezelése\",\n" +
                "  \"gui_shop_manager_add_item\": \"Tárgy hozzáadása\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Fül hozzáadása\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Kattints az eltávolitáshoz\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Jobb klikk az ár szerkesztéséhez\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Tárgy: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Tárgy hozzáadása\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Ár szerkesztése: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Tárgy árának szerkesztése\",\n" +
                "  \"gui_shop_manager_item_id\": \"Tárgy ID:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"Az ID nem lehet üres!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Mennyiség:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Vételi ár:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Eladási ár:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Hotbar tárgyak\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Új fül hozzáadása\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Fül neve:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Törölni szeretnéd a(z) '{tab}' fület? Minden benne lévö tárgy törlödni fog.\",\n" +
                "  \"gui_myshop_manager_title\": \"Saját boltom kezelése\",\n" +
                "  \"gui_myshop_rename_shop\": \"Bolt átnevezése\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Bolt átnevezése\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Bolt neve:\",\n" +
                "  \"gui_myshop_open_shop\": \"Bolt kinyitása\",\n" +
                "  \"gui_myshop_close_shop\": \"Bolt bezárása\",\n" +
                "  \"gui_myshop_set_icon\": \"Ikon választása\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Eltávolitod a(z) {item} (ID: {uniqueid}) tárgyat a boltodból?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Készlet\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Szeretnél venni {quantity}x {item}-t {price} áron?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Készlet\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Tartósság\",\n" +
                "  \"gui_shops_empty\": \"Jelenleg nincs nyitva tartó bolt.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Tárgyak\",\n" +
                "  \"gui_shops_tooltip_click\": \"Kattints a megnyitáshoz\",\n" +
                "  \"gui_shops_player_shop\": \"{player} boltja\",\n" +
                "  \"gui_shops_items_count\": \"tárgy\",\n" +
                "  \"gui_shop_empty\": \"A bolt üres\",\n" +
                "  \"gui_shops_title\": \"Játékos boltok böngészése.\",\n" +
                "  \"gui_shops_title_player\": \"Játékos boltok\",\n" +
                "  \"gui_playershop_title\": \"Tulajdonos: {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"V: \",\n" +
                "  \"gui_shop_price_sell_label\": \"E: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Készlet: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Kattints az eltávolitáshoz\",\n" +
                "  \"gui_shop_manager_add_console\": \"Parancs hozzáadása\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Konzol parancs hozzáadása\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Cash hozzáadása\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Cash Tárgy hozzáadása\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Cash Tárgy hozzáadása\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Fizetési Típus:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Pénz\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Ár (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Szeretnél vásárolni {quantity}x {item} {price} Cash-ért?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"Nincs elég Cash-ed! Szükséges: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Cash tárgy hozzáadva a bolthoz! ID: {uniqueid}, Tárgy: {item}, Mennyiség: {quantity}, Ár: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Név:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Parancs:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Ikon (Hotbar tárgy)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Konzol parancs\",\n" +
                "  \"chat_shop_console_added\": \"Konzol parancs hozzáadva! Név: {name}, Parancs: {command}, Ár: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"Vettél {quantity}x {item}-t {price} áron!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"A név nem lehet üres!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"A parancs nem lehet üres!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Válassz egy ikont a hotbarról!\",\n" +
                "  \"chat_error_console_add\": \"Hiba a parancs hozzáadásakor: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Használat: /shop renameplayershop <név> <új név>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Bolt sikeresen átnevezve! Játékos: {player}, Új név: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Hiba a játékos bolt átnevezésekor.\",\n" +
                "  \"npc_interaction_open_shop\": \"Nyomd meg az F billentyüt a bolt megnyitásához\",\n" +
                // ========== HUD (HUD szövegek) ==========
                "  \"hud_nick\": \"&l&6Név&r:\",\n" +
                "  \"hud_money\": \"&l&6Pénz&r:\",\n" +
                "  \"hud_cash\": \"&l&6Készpénz&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Helyezés&r:\",\n" +
                "  \"hud_rank\": \"Rang\",\n" +
                "  \"hud_shop_status\": \"&l&6Bolt\",\n" +
                "  \"hud_shop_open\": \"&aNyitva\",\n" +
                "  \"hud_shop_closed\": \"&cZárva\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aKerestél: &l{amount}\"\n" +
                "}";
    }

    private static String getDefaultFRJson() {
        return "{\n" +
                // ========== CHAT (Messages du Chat) ==========
                "  \"chat_insufficient_balance\": \"Vous n'avez pas assez de solde!\",\n" +
                "  \"chat_player_not_found\": \"Joueur introuvable!\",\n" +
                "  \"chat_invalid_amount\": \"Montant invalide!\",\n" +
                "  \"chat_cannot_pay_yourself\": \"Vous ne pouvez pas vous payer vous-même!\",\n" +
                "  \"chat_payment_sent\": \"Paiement envoyé avec succès!\",\n" +
                "  \"chat_payment_received\": \"Vous avez reçu un paiement!\",\n" +
                "  \"chat_balance_set\": \"Solde défini avec succès!\",\n" +
                "  \"chat_balance_added\": \"Solde ajouté avec succès!\",\n" +
                "  \"chat_usage_money\": \"Utilisation: /money [pseudo]\",\n" +
                "  \"chat_usage_pay\": \"Utilisation: /money pay <pseudo> <montant>\",\n" +
                "  \"chat_usage_set\": \"Utilisation: /money set <pseudo> <montant>\",\n" +
                "  \"chat_usage_give\": \"Utilisation: /money give <pseudo> <montant>\",\n" +
                "  \"chat_no_permission\": \"Vous n'avez pas la permission d'utiliser cette commande!\",\n" +
                "  \"chat_balance_of\": \"Solde de {player}\",\n" +
                "  \"chat_top_10_richest\": \"Top 10 des plus riches\",\n" +
                "  \"chat_top_separator\": \"=========================================\",\n" +
                "  \"chat_top_no_players\": \"Aucun joueur trouvé dans le classement.\",\n" +
                "  \"chat_money_top_disabled\": \"Le classement d'argent est désactivé.\",\n" +
                "  \"chat_top_position\": \"{position} {player}: {balance}\",\n" +
                "  \"chat_balance_set_to\": \"Solde de {player} défini à {amount}\",\n" +
                "  \"chat_balance_added_to\": \"Ajouté {amount} à {player}. Nouveau solde: {balance}\",\n" +
                "  \"chat_money_received\": \"Vous avez reçu {amount}.\",\n" +
                "  \"chat_money_received_from\": \"Vous avez reçu {amount} de {player}.\",\n" +
                "  \"chat_cash_of\": \"Cash de {player}\",\n" +
                "  \"chat_cash_added\": \"Cash ajouté avec succès!\",\n" +
                "  \"chat_cash_added_to\": \"Ajouté {amount} de cash à {player}. Nouveau cash: {cash}\",\n" +
                "  \"chat_cash_received\": \"Vous avez reçu {amount} de cash.\",\n" +
                "  \"chat_usage_cash_give\": \"Utilisation: /cash give <nick> <montant>\",\n" +
                "  \"chat_balance_set_notification\": \"Votre solde a été défini à {amount}.\",\n" +
                "  \"chat_plugin_loaded\": \"EconomySystem chargé avec succès!\",\n" +
                "  \"chat_ore_rewards\": \"Récompenses de minerais: {status}\",\n" +
                "  \"chat_wood_rewards\": \"Récompenses de bois: {status}\",\n" +
                "  \"chat_ores_configured\": \"Minerais configurés: {count} objets\",\n" +
                "  \"chat_api_available\": \"API publique disponible: com.economy.api.EconomyAPI\",\n" +
                "  \"chat_loading_balances\": \"Chargement des données de solde...\",\n" +
                "  \"chat_error_loading_balances\": \"ERREUR LORS DU CHARGEMENT DU FICHIER DE SOLDE\",\n" +
                "  \"chat_error_saving_balances\": \"ERREUR LORS DE LA SAUVEGARDE DU FICHIER DE SOLDE\",\n" +
                "  \"chat_balances_saved\": \"Données économiques sauvegardées\",\n" +
                "  \"chat_initial_balance_given\": \"Solde initial de ${amount} donné au joueur: {player}\",\n" +
                "  \"chat_enabled\": \"Activé\",\n" +
                "  \"chat_disabled\": \"Désactivé\",\n" +
                "  \"chat_shop_item_added\": \"Objet ajouté à la boutique! ID unique: {uniqueid}, Objet: {itemid}, Quantité: {quantity}, Prix de vente: {pricesell}, Prix d'achat: {pricebuy}\",\n" +
                "  \"chat_shop_item_removed\": \"Objet retiré de la boutique! ID unique: {uniqueid}\",\n" +
                "  \"chat_shop_item_updated\": \"Prix de l'objet mis à jour! ID unique: {uniqueid}, Objet: {item}, Prix d'achat: {pricebuy}, Prix de vente: {pricesell}\",\n" +
                "  \"chat_shop_item_not_found\": \"Objet introuvable! ID unique: {uniqueid}\",\n" +
                "  \"chat_error_item_update\": \"Erreur lors de la mise à jour de l'objet: {error}\",\n" +
                "  \"chat_invalid_arguments\": \"Arguments invalides!\",\n" +
                "  \"chat_shop_item_bought\": \"Vous avez acheté {quantity}x {item} pour {price}!\",\n" +
                "  \"chat_shop_item_bought_partial\": \"Vous avez acheté {added}x sur {requested}x {item} (inventaire plein)!\",\n" +
                "  \"chat_shop_item_sold\": \"Vous avez vendu {quantity}x {item} pour {price}!\",\n" +
                "  \"chat_shop_insufficient_balance\": \"Vous n'avez pas assez de solde! Requis: {amount}\",\n" +
                "  \"chat_shop_insufficient_items\": \"Vous n'avez pas assez d'objets! Requis: {quantity}x {item}\",\n" +
                "  \"chat_item_not_found\": \"Objet introuvable! ID: {itemid}\",\n" +
                "  \"chat_shop_tab_created\": \"Onglet '{tab}' créé avec succès!\",\n" +
                "  \"chat_shop_tab_removed\": \"Onglet '{tab}' retiré avec succès!\",\n" +
                "  \"chat_shop_tab_not_found\": \"Onglet '{tab}' introuvable!\",\n" +
                "  \"chat_shop_tab_already_exists\": \"L'onglet '{tab}' existe déjà!\",\n" +
                "  \"chat_shop_tab_limit_reached\": \"Maximum de 7 onglets atteint!\",\n" +
                "  \"chat_shop_tab_name_empty\": \"Le nom de l'onglet ne peut pas être vide!\",\n" +
                "  \"chat_shop_tab_create_error\": \"Erreur lors de la création de l'onglet!\",\n" +
                "  \"chat_error_tab_create\": \"Erreur lors de la création de l'onglet: {error}\",\n" +
                "  \"chat_error_tab_remove\": \"Erreur lors de la suppression de l'onglet: {error}\",\n" +
                "  \"chat_shop_tab_remove_error\": \"Erreur lors de la suppression de l'onglet!\",\n" +
                "  \"chat_playershop_disabled\": \"Les boutiques de joueurs sont désactivées.\",\n" +
                "  \"chat_playershop_not_open\": \"La boutique de ce joueur est fermée.\",\n" +
                "  \"chat_playershop_empty\": \"Cette boutique est vide.\",\n" +
                "  \"chat_myshop_opened\": \"Votre boutique a été ouverte!\",\n" +
                "  \"chat_myshop_closed\": \"Votre boutique a été fermée!\",\n" +
                "  \"chat_myshop_icon_set\": \"Icône de la boutique définie sur: {item}\",\n" +
                "  \"chat_myshop_icon_set_error\": \"Erreur lors de la définition de l'icône de la boutique.\",\n" +
                "  \"chat_myshop_status_changed\": \"{action}\",\n" +
                "  \"chat_myshop_usage\": \"Utilisation: /myshop <open|close|add|remove|rename|tab>\",\n" +
                "  \"chat_myshop_usage_add\": \"Utilisation: /myshop add <onglet> <prixAchat> <prixVente>\",\n" +
                "  \"chat_myshop_usage_remove\": \"Utilisation: /myshop remove <uniqueid>\",\n" +
                "  \"chat_myshop_usage_rename\": \"Utilisation: /myshop rename <nom>\",\n" +
                "  \"chat_myshop_usage_tab\": \"Utilisation: /myshop tab <create|remove> <nom>\",\n" +
                "  \"chat_myshop_tab_has_items\": \"Impossible de supprimer l'onglet '{tab}' car il contient des objets. Supprimez d'abord les objets.\",\n" +
                "  \"chat_myshop_renamed\": \"Boutique renommée en: {name}\",\n" +
                "  \"chat_myshop_rename_error\": \"Erreur lors du renommage de la boutique.\",\n" +
                "  \"chat_myshop_item_added\": \"Objet ajouté à votre boutique! ID unique: {uniqueid}, Objet: {item}, Quantité: {quantity}, Prix: {price}\",\n" +
                "  \"chat_myshop_item_removed\": \"Objet retiré de votre boutique! ID unique: {uniqueid}\",\n" +
                "  \"chat_myshop_error_remove_item\": \"Erreur lors de la suppression de l'objet de l'inventaire.\",\n" +
                "  \"chat_myshop_error_inventory_full\": \"Inventaire plein! L'objet n'a pas été retourné.\",\n" +
                "  \"chat_myshop_not_owner\": \"Cet objet n'appartient pas à votre boutique.\",\n" +
                "  \"chat_playershop_item_bought\": \"Vous avez acheté {quantity}x {item} pour {price}!\",\n" +
                "  \"chat_playershop_item_bought_partial\": \"Vous avez acheté {added}x sur {requested}x {item} (inventaire plein)!\",\n" +
                "  \"chat_playershop_insufficient_stock\": \"Stock insuffisant!\",\n" +
                "  \"chat_playershop_cannot_buy_from_self\": \"Vous ne pouvez pas acheter dans votre propre boutique!\",\n" +
                "  \"chat_shops_empty\": \"Aucune boutique ouverte pour le moment.\",\n" +
                "  \"chat_error_inventory_add\": \"Erreur lors de l'ajout de l'objet à l'inventaire. Argent remboursé.\",\n" +
                "  \"chat_error_inventory_remove\": \"Erreur lors de la suppression de l'objet de l'inventaire.\",\n" +
                "  \"chat_error_item_info\": \"Erreur lors de la récupération des informations de l'objet: {error}\",\n" +
                "  \"chat_error_item_add\": \"Erreur lors de l'ajout de l'objet: {error}\",\n" +
                "  \"chat_error_item_remove\": \"Erreur lors de la suppression de l'objet: {error}\",\n" +
                "  \"chat_playershop_owner_sold\": \"Vous avez vendu {quantity}x {item} à {player} pour {money}\",\n" +
                "  \"chat_playershop_owner_bought\": \"Vous avez acheté {quantity}x {item} de {player} pour {money}\",\n" +
                "  \"chat_iteminfo_error_inventory\": \"Erreur lors de l'accès à l'inventaire.\",\n" +
                "  \"chat_iteminfo_no_item\": \"Vous ne tenez aucun objet.\",\n" +
                "  \"chat_iteminfo_invalid_id\": \"L'objet n'a pas d'ID valide.\",\n" +
                "  \"chat_iteminfo_info\": \"ID de l'objet: {itemid} | Nom: {itemname}\",\n" +
                "  \"chat_shop_manager_item_id_empty\": \"L'ID de l'objet ne peut pas être vide!\",\n" +
                "  \"chat_hud_usage\": \"Utilisation: /hud on ou /hud off\",\n" +
                "  \"chat_hud_enabled\": \"HUD activé!\",\n" +
                "  \"chat_hud_disabled\": \"HUD désactivé!\",\n" +
                "  \"chat_hud_invalid_action\": \"Action invalide! Utilisez 'on' ou 'off'.\",\n" +
                "  \"chat_hud_server_disabled\": \"Le HUD est désactivé sur le serveur.\",\n" +
                "  \"chat_hud_error\": \"Erreur lors du changement de préférence HUD.\",\n" +
                "  \"chat_npc_error_get_position\": \"&cErreur lors de la récupération de la position du joueur.\",\n" +
                "  \"chat_npc_error_get_world_uuid\": \"&cErreur lors de la récupération de l'UUID du monde.\",\n" +
                "  \"chat_npc_created\": \"&aPNJ de boutique créé à votre position! ID: {npcId}\",\n" +
                "  \"chat_npc_created_with_name\": \"&aPNJ de boutique créé à votre position! ID: {npcId} | Nom: {name}\",\n" +
                "  \"chat_npc_list_empty\": \"&cAucun PNJ trouvé.\",\n" +
                "  \"chat_npc_list_header\": \"&a=== PNJ de Boutique ({count}) ===\",\n" +
                "  \"chat_npc_list_item\": \"&e{index}. &7ID: &f{npcId} &7| Monde: &f{worldId} &7| Pos: &f{x}, &f{y}, &f{z}\",\n" +
                "  \"chat_npc_remove_usage\": \"&cUtilisation: /shop npc remove <shopId>\",\n" +
                "  \"chat_npc_remove_usage_list\": \"&cUtilisez /shop npc list pour voir les Shop ID des PNJ\",\n" +
                "  \"chat_npc_removed\": \"&aPNJ retiré avec succès!\",\n" +
                "  \"chat_npc_not_found\": \"&cPNJ introuvable!\",\n" +
                "  \"chat_npc_invalid_uuid\": \"&cUUID invalide!\",\n" +
                "  \"chat_npc_invalid_shop_id\": \"&cShop ID invalide! Utilisez un nombre.\",\n" +
                "  \"chat_npc_all_removed\": \"&aTous les PNJ ont été retirés!\",\n" +
                "  \"chat_npc_moved\": \"&aPNJ déplacé avec succès à votre position!\",\n" +
                "  \"chat_npc_movehere_usage\": \"&cUtilisation: /shop npc movehere <shopId>\",\n" +
                // ========== Descriptions de Commandes ==========
                "  \"desc_shop\": \"Ouvre la boutique\",\n" +
                "  \"desc_shop_add\": \"Ajoute un objet à la boutique\",\n" +
                "  \"desc_shop_remove\": \"Retire un objet de la boutique\",\n" +
                "  \"desc_shop_tab\": \"Gère les onglets de la boutique\",\n" +
                "  \"desc_shop_tab_create\": \"Crée un nouvel onglet dans la boutique\",\n" +
                "  \"desc_shop_tab_remove\": \"Retire un onglet de la boutique et tous ses objets\",\n" +
                "  \"desc_shop_manager\": \"Ouvre l'interface de gestion de la boutique\",\n" +
                "  \"desc_shop_npc\": \"Gère le PNJ de la boutique\",\n" +
                "  \"desc_shop_npc_add\": \"Crée le PNJ de la boutique à votre position\",\n" +
                "  \"desc_shop_npc_remove\": \"Retire un PNJ de la boutique par Shop ID\",\n" +
                "  \"desc_shop_npc_list\": \"Liste tous les PNJ de la boutique\",\n" +
                "  \"desc_shop_npc_removeall\": \"Retire tous les PNJ de la boutique\",\n" +
                "  \"desc_shop_npc_movehere\": \"Déplace le PNJ à votre position\",\n" +
                "  \"desc_shop_renameplayershop\": \"Renomme la boutique d'un joueur\",\n" +
                "  \"desc_money\": \"Système économique\",\n" +
                "  \"desc_money_pay\": \"Transfère de l'argent à un autre joueur\",\n" +
                "  \"desc_money_top\": \"Affiche le top 10 des joueurs les plus riches\",\n" +
                "  \"desc_money_set\": \"Définit le solde d'un joueur\",\n" +
                "  \"desc_money_give\": \"Ajoute du solde à un joueur\",\n" +
                "  \"desc_cash\": \"Système de cash\",\n" +
                "  \"desc_cash_give\": \"Ajoute du cash à un joueur\",\n" +
                "  \"desc_shops\": \"Liste toutes les boutiques ouvertes\",\n" +
                "  \"desc_playershop\": \"Ouvre la boutique d'un joueur\",\n" +
                "  \"desc_myshop\": \"Gère votre boutique personnelle\",\n" +
                "  \"desc_myshop_open\": \"Ouvre votre boutique personnelle\",\n" +
                "  \"desc_myshop_close\": \"Ferme votre boutique personnelle\",\n" +
                "  \"desc_myshop_add\": \"Ajoute un objet à votre boutique\",\n" +
                "  \"desc_myshop_remove\": \"Retire un objet de votre boutique\",\n" +
                "  \"desc_myshop_rename\": \"Renomme votre boutique\",\n" +
                "  \"desc_myshop_tab\": \"Gère les onglets de votre boutique\",\n" +
                "  \"desc_myshop_tab_create\": \"Crée un nouvel onglet dans votre boutique\",\n" +
                "  \"desc_myshop_tab_remove\": \"Retire un onglet de votre boutique et tous ses objets\",\n" +
                "  \"desc_myshop_manager\": \"Gère votre boutique personnelle\",\n" +
                "  \"desc_iteminfo\": \"Affiche des informations sur l'objet dans la main\",\n" +
                "  \"desc_hud\": \"Active/désactive le HUD d'EconomySystem\",\n" +
                "  \"desc_hud_on\": \"Active le HUD\",\n" +
                "  \"desc_hud_off\": \"Désactive le HUD\",\n" +
                // ========== GUI (Textes des Fenêtres) ==========
                "  \"gui_shop_confirm_buy\": \"Voulez-vous acheter {quantity}x {item} pour {price}?\",\n" +
                "  \"gui_shop_confirm_sell\": \"Voulez-vous vendre {quantity}x {item} pour {price}?\",\n" +
                "  \"gui_shop_quantity_label\": \"Quantité:\",\n" +
                "  \"gui_shop_button_confirm\": \"Confirmer\",\n" +
                "  \"gui_shop_button_cancel\": \"Annuler\",\n" +
                "  \"gui_shop_button_back\": \"Retour\",\n" +
                "  \"gui_shop_title\": \"Boutique Admin - L'Économie\",\n" +
                "  \"gui_shop_npc_add_title\": \"Ajouter un PNJ de Boutique\",\n" +
                "  \"gui_shop_npc_add_name\": \"Nom du PNJ:\",\n" +
                "  \"gui_shop_npc_add_model\": \"Modèle du PNJ:\",\n" +
                "  \"gui_shop_tooltip_unique_id\": \"ID unique\",\n" +
                "  \"gui_shop_tooltip_quantity\": \"Quantité\",\n" +
                "  \"gui_shop_tooltip_buy\": \"Acheter\",\n" +
                "  \"gui_shop_tooltip_sell\": \"Vendre\",\n" +
                "  \"gui_shop_tooltip_left_click_buy\": \"Clic gauche: Acheter\",\n" +
                "  \"gui_shop_tooltip_right_click_sell\": \"Clic droit: Vendre\",\n" +
                "  \"gui_shop_tooltip_right_click_buy\": \"Clic droit: Acheter\",\n" +
                "  \"gui_shop_tooltip_left_click_sell\": \"Clic gauche: Vendre\",\n" +
                "  \"gui_shop_tooltip_middle_click_remove\": \"Clic molette: Retirer\",\n" +
                "  \"gui_shop_tooltip_buy_disabled\": \"Achat désactivé\",\n" +
                "  \"gui_shop_tooltip_sell_disabled\": \"Vente désactivée\",\n" +
                "  \"gui_shop_tooltip_not_available\": \"Objet non disponible à l'achat ou à la vente\",\n" +
                "  \"gui_shop_manager_title\": \"Gérer la Boutique\",\n" +
                "  \"gui_shop_manager_add_item\": \"Ajouter un Objet\",\n" +
                "  \"gui_shop_manager_add_tab\": \"Ajouter un Onglet\",\n" +
                "  \"gui_shop_manager_remove_item_hint\": \"Cliquez pour retirer l'objet\",\n" +
                "  \"gui_shop_manager_edit_item_hint\": \"Clic droit pour modifier le prix\",\n" +
                "  \"gui_shop_manager_add_item_title\": \"Ajouter un Objet: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_add_item_title_simple\": \"Ajouter un Objet\",\n" +
                "  \"gui_shop_manager_edit_item_title\": \"Modifier le Prix: {item} (x{quantity})\",\n" +
                "  \"gui_shop_manager_edit_item_title_simple\": \"Modifier le Prix de l'Objet\",\n" +
                "  \"gui_shop_manager_item_id\": \"ID de l'objet:\",\n" +
                "  \"gui_shop_manager_item_id_empty\": \"L'ID de l'objet ne peut pas être vide!\",\n" +
                "  \"gui_shop_manager_quantity\": \"Quantité:\",\n" +
                "  \"gui_shop_manager_price_buy\": \"Prix d'achat:\",\n" +
                "  \"gui_shop_manager_price_sell\": \"Prix de vente:\",\n" +
                "  \"gui_shop_manager_hotbar_items\": \"Objets de la Barre d'Action\",\n" +
                "  \"gui_shop_manager_add_tab_title\": \"Ajouter un Nouvel Onglet\",\n" +
                "  \"gui_shop_manager_tab_name\": \"Nom de l'onglet:\",\n" +
                "  \"gui_shop_manager_confirm_remove_tab\": \"Voulez-vous retirer l'onglet '{tab}'? Tous les objets seront retirés.\",\n" +
                "  \"gui_myshop_manager_title\": \"Gérer Ma Boutique\",\n" +
                "  \"gui_myshop_rename_shop\": \"Renommer la Boutique\",\n" +
                "  \"gui_myshop_rename_shop_title\": \"Renommer la Boutique\",\n" +
                "  \"gui_myshop_rename_shop_label\": \"Nom de la boutique:\",\n" +
                "  \"gui_myshop_open_shop\": \"Ouvrir la Boutique\",\n" +
                "  \"gui_myshop_close_shop\": \"Fermer la Boutique\",\n" +
                "  \"gui_myshop_set_icon\": \"Choisir une Icône\",\n" +
                "  \"gui_myshop_confirm_remove\": \"Voulez-vous retirer l'objet {item} (ID: {uniqueid}) de votre boutique?\",\n" +
                "  \"gui_shop_tooltip_stock\": \"Stock\",\n" +
                "  \"gui_playershop_confirm_buy\": \"Voulez-vous acheter {quantity}x {item} pour {price}?\",\n" +
                "  \"gui_playershop_tooltip_stock\": \"Stock\",\n" +
                "  \"gui_playershop_tooltip_durability\": \"Durabilité\",\n" +
                "  \"gui_shops_empty\": \"Aucune boutique ouverte pour le moment.\",\n" +
                "  \"gui_shops_tooltip_items\": \"Objets\",\n" +
                "  \"gui_shops_tooltip_click\": \"Cliquez pour ouvrir la boutique\",\n" +
                "  \"gui_shops_player_shop\": \"Boutique de {player}\",\n" +
                "  \"gui_shops_items_count\": \"objets\",\n" +
                "  \"gui_shop_empty\": \"Aucun objet dans la boutique\",\n" +
                "  \"gui_shops_title\": \"Vous consultez les boutiques des joueurs.\",\n" +
                "  \"gui_shops_title_player\": \"Boutiques des Joueurs\",\n" +
                "  \"gui_playershop_title\": \"Cette boutique appartient au joueur {player}.\",\n" +
                "  \"gui_shop_price_buy_label\": \"A: \",\n" +
                "  \"gui_shop_price_sell_label\": \"V: \",\n" +
                "  \"gui_shop_price_separator\": \" / \",\n" +
                "  \"gui_playershop_stock_label\": \"Stock: \",\n" +
                "  \"gui_playershop_tooltip_click_remove\": \"Cliquez pour retirer l'objet\",\n" +
                "  \"gui_shop_manager_add_console\": \"Ajouter Console\",\n" +
                "  \"gui_shop_manager_add_console_title\": \"Ajouter une Commande Console\",\n" +
                "  \"gui_shop_manager_add_cash\": \"Ajouter Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title\": \"Ajouter un Objet Cash\",\n" +
                "  \"gui_shop_manager_add_cash_title_simple\": \"Ajouter un Objet Cash\",\n" +
                "  \"gui_shop_manager_payment_type\": \"Type de Paiement:\",\n" +
                "  \"gui_shop_manager_payment_money\": \"Argent\",\n" +
                "  \"gui_shop_manager_payment_cash\": \"Cash\",\n" +
                "  \"gui_shop_manager_price_buy_cash\": \"Prix (Cash):\",\n" +
                "  \"gui_shop_confirm_buy_cash\": \"Voulez-vous acheter {quantity}x {item} pour {price} Cash?\",\n" +
                "  \"chat_shop_insufficient_cash\": \"Vous n'avez pas assez de Cash! Requis: {amount}\",\n" +
                "  \"chat_shop_cash_added\": \"Objet Cash ajouté à la boutique! ID unique: {uniqueid}, Objet: {item}, Quantité: {quantity}, Prix: {price} Cash\",\n" +
                "  \"gui_shop_manager_console_display_name\": \"Nom:\",\n" +
                "  \"gui_shop_manager_console_command\": \"Commande:\",\n" +
                "  \"gui_shop_manager_console_icon\": \"Icône (Objet de la Barre d'Action)\",\n" +
                "  \"gui_shop_tooltip_console_command\": \"Commande Console\",\n" +
                "  \"chat_shop_console_added\": \"Commande console ajoutée! Nom: {name}, Commande: {command}, Prix: {pricebuy}\",\n" +
                "  \"chat_shop_console_bought\": \"Vous avez acheté {quantity}x {item} pour {price}!\",\n" +
                "  \"chat_shop_manager_console_display_name_empty\": \"Le nom ne peut pas être vide!\",\n" +
                "  \"chat_shop_manager_console_command_empty\": \"La commande ne peut pas être vide!\",\n" +
                "  \"chat_shop_manager_console_icon_empty\": \"Sélectionnez une icône de la barre d'action!\",\n" +
                "  \"chat_error_console_add\": \"Erreur lors de l'ajout de la commande console: {error}\",\n" +
                "  \"chat_shop_rename_player_usage\": \"Utilisation: /shop renameplayershop <pseudo> <nom>\",\n" +
                "  \"chat_shop_rename_player_success\": \"Boutique renommée avec succès! Joueur: {player}, Nouveau nom: {name}\",\n" +
                "  \"chat_shop_rename_player_error\": \"Erreur lors du renommage de la boutique du joueur.\",\n" +
                "  \"npc_interaction_open_shop\": \"Appuyez sur F pour ouvrir la boutique\",\n" +
                // ========== HUD (Textes de la HUD) ==========
                "  \"hud_nick\": \"&l&6Pseudo&r:\",\n" +
                "  \"hud_money\": \"&l&6Argent&r:\",\n" +
                "  \"hud_cash\": \"&l&6Cash&r:\",\n" +
                "  \"hud_top_rank\": \"&l&6Top Rang&r:\",\n" +
                "  \"hud_rank\": \"Rang\",\n" +
                "  \"hud_shop_status\": \"&l&6Boutique\",\n" +
                "  \"hud_shop_open\": \"&aOuverte\",\n" +
                "  \"hud_shop_closed\": \"&cFermée\",\n" +
                "  \"hud_rank_unknown\": \"N/A\",\n" +
                "  \"hud_gain\": \"&aVous avez gagné &l{amount}\"\n" +
                "}";
    }

    /**
     * Adiciona automaticamente uma tradução faltante ao arquivo de idioma.
     * Busca o valor padrão do JSON padrão e adiciona ao arquivo existente.
     * 
     * @param key A chave da tradução faltante
     * @param lang O código do idioma (PT, EN, ES, etc.)
     * @return O valor padrão da tradução, ou null se não encontrar
     */
    private static String addMissingTranslation(String key, String lang) {
        try {
            // Busca o JSON padrão para a linguagem
            String defaultJsonContent = getDefaultJsonForLanguage(lang);
            if (defaultJsonContent == null) {
                logger.at(Level.WARNING).log("No default JSON found for language: " + lang);
                return null;
            }
            
            // Parse do JSON padrão
            JsonObject defaultJson = JsonParser.parseString(defaultJsonContent).getAsJsonObject();
            
            // Verifica se a chave existe no JSON padrão
            if (!defaultJson.has(key)) {
                logger.at(Level.WARNING).log("Translation key not found in default JSON: " + key + " (lang: " + lang + ")");
                return null;
            }
            
            // Obtém o valor padrão
            String defaultValue = defaultJson.get(key).getAsString();
            
            // Carrega o arquivo JSON existente
            Path langPath = Paths.get(FileUtils.MAIN_PATH, "Language_" + lang + ".json");
            File langFile = langPath.toFile();
            
            if (!langFile.exists()) {
                // Se o arquivo não existe, cria um novo com todas as traduções padrão
                createDefaultLanguageFile(langFile, lang);
                return defaultValue;
            }
            
            // Lê o arquivo existente
            JsonObject existingJson;
            try (FileReader reader = new FileReader(langFile)) {
                existingJson = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            // Adiciona a chave faltante
            existingJson.addProperty(key, defaultValue);
            
            // Salva o arquivo
            try (FileWriter writer = new FileWriter(langFile)) {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                gson.toJson(existingJson, writer);
            }
            
            logger.at(Level.INFO).log("Added missing translation: " + key + " = \"" + defaultValue + "\" (lang: " + lang + ")");
            return defaultValue;
            
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error adding missing translation " + key + " (lang: " + lang + "): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Retorna o JSON padrão para uma linguagem específica.
     * 
     * @param lang O código do idioma (PT, EN, ES, etc.)
     * @return O JSON padrão como String, ou null se a linguagem não for suportada
     */
    private static String getDefaultJsonForLanguage(String lang) {
        if ("PT".equals(lang)) {
            return getDefaultPTJson();
        } else if ("EN".equals(lang)) {
            return getDefaultENJson();
        } else if ("ES".equals(lang)) {
            return getDefaultESJson();
        } else if ("RU".equals(lang)) {
            return getDefaultRUJson();
        } else if ("PL".equals(lang)) {
            return getDefaultPLJson();
        } else if ("DE".equals(lang)) {
            return getDefaultDEJson();
        } else if ("HU".equals(lang)) {
            return getDefaultHUJson();
        } else if ("FR".equals(lang)) {
            return getDefaultFRJson();
        }
        return null;
    }

    public static String getTranslation(String key) {
        // Verifica se a linguagem mudou na configuração
        updateCurrentLanguage();
        return getTranslation(key, currentLanguage);
    }

    private static void updateCurrentLanguage() {
        if (Main.CONFIG != null && Main.CONFIG.get() != null) {
            String configLanguage = Main.CONFIG.get().getLanguage();
            if (configLanguage != null && !configLanguage.equals(currentLanguage)) {
                logger.at(Level.INFO).log("Language changed from " + currentLanguage + " to " + configLanguage);
                currentLanguage = configLanguage;
                // Garante que a nova linguagem está carregada
                if (!loadedLanguages.containsKey(currentLanguage)) {
                    loadLanguage(currentLanguage);
                }
            }
        }
    }

    public static String getTranslation(String key, String lang) {
        JsonObject langData = loadedLanguages.get(lang);
        if (langData == null) {
            loadLanguage(lang);
            langData = loadedLanguages.get(lang);
        }

        if (langData == null || !langData.has(key)) {
            logger.at(Level.WARNING).log("Translation not found: " + key + " (lang: " + lang + ")");
            // Tenta adicionar automaticamente a tradução faltante
            String defaultValue = addMissingTranslation(key, lang);
            if (defaultValue != null) {
                // Recarrega a linguagem para incluir a nova tradução
                loadedLanguages.remove(lang);
                loadLanguage(lang);
                langData = loadedLanguages.get(lang);
                if (langData != null && langData.has(key)) {
                    logger.at(Level.INFO).log("Translation automatically added: " + key + " (lang: " + lang + ")");
                    return langData.get(key).getAsString();
                }
            }
            return key; // Retorna a chave se não encontrar
        }

        return langData.get(key).getAsString();
    }

    public static String getTranslation(String key, Map<String, String> placeholders) {
        String translation = getTranslation(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                translation = translation.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return translation;
    }

    public static Message getMessage(String key, Color color) {
        String translation = getTranslation(key);
        // Se a tradução contém códigos de cor, usa o MessageFormatter
        if (translation != null && (translation.contains("&") || translation.contains("§"))) {
            return MessageFormatter.format(translation);
        }
        return Message.raw(translation).color(color);
    }

    public static Message getMessage(String key, Color color, Map<String, String> placeholders) {
        String translation = getTranslation(key, placeholders);
        // Se a tradução contém códigos de cor, usa o MessageFormatter
        if (translation != null && (translation.contains("&") || translation.contains("§"))) {
            return MessageFormatter.format(translation);
        }
        return Message.raw(translation).color(color);
    }
    
    /**
     * Obtém uma mensagem formatada com códigos de cor, sem especificar cor padrão
     * Útil quando a mensagem já contém códigos de cor
     */
    public static Message getMessage(String key) {
        String translation = getTranslation(key);
        if (translation != null && (translation.contains("&") || translation.contains("§"))) {
            return MessageFormatter.format(translation);
        }
        return Message.raw(translation);
    }
    
    /**
     * Obtém uma mensagem formatada com códigos de cor e placeholders, sem especificar cor padrão
     */
    public static Message getMessage(String key, Map<String, String> placeholders) {
        String translation = getTranslation(key, placeholders);
        if (translation != null && (translation.contains("&") || translation.contains("§"))) {
            return MessageFormatter.format(translation);
        }
        return Message.raw(translation);
    }
}

