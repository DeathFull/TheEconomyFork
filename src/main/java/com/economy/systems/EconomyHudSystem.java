package com.economy.systems;

import com.economy.Main;
import com.economy.config.HudConfig;
import com.economy.config.HudConfigManager;
import com.economy.config.HudField;
import com.economy.economy.EconomyManager;
import com.economy.playershop.PlayerShopManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.HudHelper;
import com.economy.util.LanguageManager;
import com.economy.util.MessageFormatter;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EconomyHudSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<PlayerRef, EconomyHud> huds = new ConcurrentHashMap<>();
    private final Query<EntityStore> query;

    public EconomyHudSystem() {
        this.query = Query.and(Player.getComponentType());
    }
    
    /**
     * Método estático para criar a HUD quando o jogador entra no mundo
     */
    public static void createHudForPlayer(Player player, PlayerRef playerRef) {
        // Verifica se a HUD está habilitada na configuração do servidor
        if (Main.CONFIG != null && Main.CONFIG.get() != null && !Main.CONFIG.get().isEnableHud()) {
            return;
        }
        
        // Verifica se o jogador tem a HUD habilitada nas preferências
        try {
            com.economy.hud.HudPreferenceManager preferenceManager = com.economy.hud.HudPreferenceManager.getInstance();
            if (preferenceManager != null && !preferenceManager.isHudEnabled(playerRef.getUuid())) {
                return;
            }
        } catch (Exception e) {
            // Se houver erro ao verificar preferências, assume que está habilitada
        }
        
        try {
            if (!huds.containsKey(playerRef)) {
                EconomyHud hud = new EconomyHud(playerRef);
                huds.put(playerRef, hud);
                
                // Registra usando HudHelper (compatível com MultipleHUD)
                boolean usedMultipleHud = HudHelper.setCustomHud(player, playerRef, hud);
                
                // Só chama show() se NÃO estiver usando MultipleHUD
                // O MultipleHUD gerencia o show() internamente quando registramos
                if (!usedMultipleHud) {
                    try {
                        hud.show(); // Isso constrói e mostra a HUD
                    } catch (Exception e) {
                        LOGGER.atWarning().withCause(e).log("Failed to show HUD for player: %s", playerRef.getUsername());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to create HUD via AddPlayerToWorldEvent for player: %s", playerRef.getUsername());
        }
    }

    /**
     * Método estático para remover a HUD de um jogador
     * Requer que o Player seja passado para acessar o HudManager
     */
    public static void removeHudForPlayer(Player player, PlayerRef playerRef) {
        try {
            EconomyHud existingHud = huds.remove(playerRef);
            if (existingHud != null) {
                tickCounters.remove(playerRef);
                // Remove a HUD usando HudHelper (compatível com MultipleHUD)
                try {
                    HudHelper.hideCustomHud(player, playerRef);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to remove HUD from HudManager for player: %s", playerRef.getUsername());
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to remove HUD for player: %s", playerRef.getUsername());
        }
    }
    

    private static final int UPDATE_INTERVAL = 10; // Atualiza a cada 10 ticks (~2 vezes por segundo, 20 ticks = 1 segundo)
    private static final Map<PlayerRef, Integer> tickCounters = new ConcurrentHashMap<>();

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        Holder<EntityStore> holder = EntityUtils.toHolder(entityIndex, chunk);

        Player player = holder.getComponent(Player.getComponentType());
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            return;
        }

        // Verifica se a HUD está habilitada na configuração do servidor
        if (Main.CONFIG != null && Main.CONFIG.get() != null && !Main.CONFIG.get().isEnableHud()) {
            // Se a HUD foi desabilitada no servidor, remove a HUD existente se houver
            EconomyHud existingHud = huds.remove(playerRef);
            if (existingHud != null) {
                tickCounters.remove(playerRef);
            }
            return;
        }

        // Verifica se o jogador tem a HUD habilitada nas preferências
        boolean playerHudEnabled = true;
        try {
            com.economy.hud.HudPreferenceManager preferenceManager = com.economy.hud.HudPreferenceManager.getInstance();
            if (preferenceManager != null) {
                playerHudEnabled = preferenceManager.isHudEnabled(playerRef.getUuid());
            }
        } catch (Exception e) {
            // Se houver erro ao verificar preferências, assume que está habilitada
        }
        
        // Se o jogador desabilitou a HUD, remove se existir
        if (!playerHudEnabled) {
            EconomyHud existingHud = huds.remove(playerRef);
            if (existingHud != null) {
                tickCounters.remove(playerRef);
            }
            return;
        }

        EconomyHud hud = huds.get(playerRef);
        
        if (hud == null) {
            try {
                hud = new EconomyHud(playerRef);
                huds.put(playerRef, hud);
                tickCounters.put(playerRef, 0);

                // Registra usando HudHelper (compatível com MultipleHUD)
                boolean usedMultipleHud = HudHelper.setCustomHud(player, playerRef, hud);
                
                // Só chama show() se NÃO estiver usando MultipleHUD
                // O MultipleHUD gerencia o show() internamente quando registramos
                if (!usedMultipleHud) {
                    try {
                        hud.show(); // Isso constrói e mostra a HUD
                    } catch (Exception e) {
                        LOGGER.atWarning().withCause(e).log("Failed to show HUD for player: %s", playerRef.getUsername());
                    }
                }

            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to create HUD for player: %s", playerRef.getUsername());
            }
        } else {
            // Atualiza a HUD periodicamente (cada jogador tem seu próprio contador)
            int tickCounter = tickCounters.getOrDefault(playerRef, 0);
            tickCounter++;
            if (tickCounter >= UPDATE_INTERVAL) {
                tickCounter = 0;
                try {
                    // Se o MultipleHUD está disponível, re-registra a HUD periodicamente
                    // para garantir que não seja sobrescrita por plugins que usam o método padrão
                    if (HudHelper.isMultipleHudAvailable()) {
                        HudHelper.setCustomHud(player, playerRef, hud);
                    }
                    hud.updateHud(playerRef);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to update HUD for player: %s", playerRef.getUsername());
                }
            }
            tickCounters.put(playerRef, tickCounter);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    private static class EconomyHud extends CustomUIHud {

        private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
        private boolean isInitialized = false;
        private long lastRebuildTime = 0;
        private static final long REBUILD_COOLDOWN_MS = 5000; // 5 segundos entre rebuilds
        
        // Rastreamento de saldo para detectar ganhos
        private double lastBalance = -1.0;
        private double currentGain = 0.0;
  
        public EconomyHud(PlayerRef playerRef) {
            super(playerRef);
        }

        @Override
        protected void build(@NonNullDecl UICommandBuilder builder) {
            try {
                // Usa o arquivo UI fixo (como na versão 1.0.7 que funciona)
                builder.append("Hud/EconomySystem_Hud.ui");
                
                // Atualiza o Height do painel baseado na configuração JSON
                updatePanelHeight(builder);
                
                // Atualiza os valores iniciais baseados na configuração JSON
                updateHudValues(builder);
                
                // Marca como inicializado
                if (!isInitialized) {
                    isInitialized = true;
                }
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Error building HUD for player: %s", getPlayerRef().getUsername());
                throw e;
            }
        }
        
        /**
         * Atualiza o Height e Width do painel baseado na configuração JSON
         */
        private void updatePanelHeight(@NonNullDecl UICommandBuilder builder) {
            try {
                HudConfig hudConfig = HudConfigManager.getInstance().getConfig();
                int height;
                int width;
                
                if (hudConfig.getHeight() > 0) {
                    height = hudConfig.getHeight();
                } else {
                    // Calcula altura baseada nos campos visíveis
                    int visibleFieldsCount = hudConfig.getVisibleFieldsCount();
                    height = Math.max(85, visibleFieldsCount * 20 + 20);
                }
                
                if (hudConfig.getWidth() > 0) {
                    width = hudConfig.getWidth();
                } else {
                    // Valor padrão para width
                    width = 150;
                }
                
                // Cria um objeto Anchor completo e atualiza usando setObject
                com.hypixel.hytale.server.core.ui.Anchor anchor = new com.hypixel.hytale.server.core.ui.Anchor();
                // Usa a posição horizontal da configuração
                // Se RightPosition > 0, usa do lado direito; se RightPosition = 0 e LeftPosition > 0, usa do lado esquerdo
                int rightPosition = hudConfig.getRightPosition();
                int leftPosition = hudConfig.getLeftPosition();
                if (rightPosition > 0) {
                    // Posiciona do lado direito
                    anchor.setRight(com.hypixel.hytale.server.core.ui.Value.of(rightPosition));
                } else if (leftPosition > 0) {
                    // Posiciona do lado esquerdo
                    anchor.setLeft(com.hypixel.hytale.server.core.ui.Value.of(leftPosition));
                } else {
                    // Fallback: usa lado direito com valor padrão
                    anchor.setRight(com.hypixel.hytale.server.core.ui.Value.of(20));
                }
                // Usa a posição vertical da configuração
                int topPosition = hudConfig.getTopPosition() > 0 ? hudConfig.getTopPosition() : 450;
                anchor.setTop(com.hypixel.hytale.server.core.ui.Value.of(topPosition));
                anchor.setHeight(com.hypixel.hytale.server.core.ui.Value.of(height));
                anchor.setWidth(com.hypixel.hytale.server.core.ui.Value.of(width));
                
                // Atualiza o Anchor completo do painel
                builder.setObject("#Panel.Anchor", anchor);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error updating panel size for player: %s", getPlayerRef().getUsername());
            }
        }

        /**
         * Atualiza os valores da HUD
         */
        public void updateHud(PlayerRef playerRef) {
            // Se ainda não foi inicializado, não tenta atualizar
            if (!isInitialized) {
                return;
            }
            
            try {
                UICommandBuilder builder = new UICommandBuilder();
                // Atualiza o Height do painel
                updatePanelHeight(builder);
                // Atualiza os valores dos labels
                updateHudValues(builder);
                this.update(false, builder); // false = incremental update
            } catch (Exception e) {
                // Só tenta rebuild se passou o cooldown
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRebuildTime > REBUILD_COOLDOWN_MS) {
                    lastRebuildTime = currentTime;
                    LOGGER.atWarning().withCause(e).log("Error updating HUD for player: %s, attempting rebuild", playerRef.getUsername());
                    try {
                        this.show();
                    } catch (Exception e2) {
                        LOGGER.atSevere().withCause(e2).log("Failed to rebuild HUD after update error");
                    }
                }
            }
        }

        /**
         * Atualiza os valores nos labels da HUD baseado na configuração JSON
         * Mapeia os campos do JSON para os labels fixos do UI (#NickLabel, #MoneyLabel, #RankLabel, #ShopStatusLabel)
         * Suporta linhas em branco para espaçamento
         */
        private void updateHudValues(@NonNullDecl UICommandBuilder builder) {
            try {
                UUID playerUuid = getPlayerRef().getUuid();
                EconomyManager economyManager = EconomyManager.getInstance();
                HudConfig hudConfig = HudConfigManager.getInstance().getConfig();
                
                // Cria uma cópia da lista para evitar condições de corrida
                List<HudField> fields;
                try {
                    List<HudField> originalFields = hudConfig.getFields();
                    if (originalFields == null || originalFields.isEmpty()) {
                        // Se não houver campos configurados, usa valores padrão
                        updateDefaultHudValues(builder, playerUuid, economyManager);
                        return;
                    }
                    // Cria uma cópia thread-safe da lista
                    fields = new java.util.ArrayList<>(originalFields);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Error getting HUD fields for player: %s", getPlayerRef().getUsername());
                    // Fallback para valores padrão
                    updateDefaultHudValues(builder, playerUuid, economyManager);
                    return;
                }
                
                // Mapeia os campos do JSON para os labels do UI
                // Labels disponíveis: #NickLabel, #MoneyLabel, #RankLabel, #ShopStatusLabel, #FieldLabel0-9
                String[] fixedLabelIds = {"#NickLabel", "#MoneyLabel", "#RankLabel", "#ShopStatusLabel"};
                int maxFixedLabels = fixedLabelIds.length;
                int maxDynamicLabels = 10; // FieldLabel0 até FieldLabel9
                
                // Processa todos os campos
                for (int i = 0; i < fields.size() && i < (maxFixedLabels + maxDynamicLabels); i++) {
                    try {
                        if (i >= fields.size()) {
                            break;
                        }
                        
                        HudField field = fields.get(i);
                        if (field == null) {
                            continue;
                        }
                        
                        // Escolhe o label ID baseado no índice
                        String labelId;
                        if (i < maxFixedLabels) {
                            labelId = fixedLabelIds[i];
                        } else {
                            labelId = "#FieldLabel" + (i - maxFixedLabels);
                        }
                        
                        if (field.isVisible()) {
                            String line = field.getLine();
                            
                            // Se a linha estiver vazia ou null, mostra como linha em branco (espaçamento)
                            if (line == null || line.trim().isEmpty()) {
                                // Linha em branco para espaçamento
                                try {
                                    builder.set(labelId + ".Text", " ");
                                    builder.set(labelId + ".Visible", true);
                                } catch (Exception e) {
                                    // Ignora erro
                                }
                                continue;
                            }
                            
                            // Processa a linha com placeholders e traduções
                            String displayText = parseFieldLine(line, playerUuid, economyManager);
                            
                            if (displayText == null || displayText.trim().isEmpty()) {
                                // Se após processar ainda estiver vazio, mostra como linha em branco
                                try {
                                    builder.set(labelId + ".Text", " ");
                                    builder.set(labelId + ".Visible", true);
                                } catch (Exception e) {
                                    // Ignora
                                }
                                continue;
                            }
                            
                            // Atualiza o texto do label
                            try {
                                if (displayText.contains("&") || displayText.contains("§")) {
                                    builder.set(labelId + ".TextSpans", MessageFormatter.format(displayText));
                                } else {
                                    builder.set(labelId + ".Text", displayText);
                                }
                            } catch (Exception e) {
                                // Fallback para texto simples
                                try {
                                    builder.set(labelId + ".Text", displayText);
                                } catch (Exception e2) {
                                    // Ignora se ainda falhar
                                }
                            }
                            
                            // Torna o label visível
                            try {
                                builder.set(labelId + ".Visible", true);
                            } catch (Exception e) {
                                // Ignora erro de visibilidade
                            }
                        } else {
                            // Oculta o label se o campo não estiver visível
                            try {
                                builder.set(labelId + ".Visible", false);
                            } catch (Exception e) {
                                // Ignora
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().withCause(e).log("Error processing field at index %d for player: %s", i, getPlayerRef().getUsername());
                        continue;
                    }
                }
                
                // Oculta labels fixos não utilizados
                for (int i = fields.size(); i < maxFixedLabels; i++) {
                    try {
                        builder.set(fixedLabelIds[i] + ".Visible", false);
                    } catch (Exception e) {
                        // Ignora
                    }
                }
                
                // Oculta labels dinâmicos não utilizados
                int usedDynamicLabels = Math.max(0, fields.size() - maxFixedLabels);
                for (int i = usedDynamicLabels; i < maxDynamicLabels; i++) {
                    try {
                        builder.set("#FieldLabel" + i + ".Visible", false);
                    } catch (Exception e) {
                        // Ignora
                    }
                }
                
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Error updating HUD values for player: %s", getPlayerRef().getUsername());
                // Fallback para valores padrão em caso de erro
                try {
                    updateDefaultHudValues(builder, getPlayerRef().getUuid(), EconomyManager.getInstance());
                } catch (Exception e2) {
                    // Ignora erro no fallback
                }
            }
        }
        
        /**
         * Atualiza os valores padrão da HUD (fallback quando não há configuração JSON)
         */
        private void updateDefaultHudValues(@NonNullDecl UICommandBuilder builder, UUID playerUuid, EconomyManager economyManager) {
            try {
                // Obtém o nome do jogador
                String playerName = economyManager.getPlayerName(playerUuid);
                if (playerName == null || playerName.isEmpty() || "Desconhecido".equals(playerName)) {
                    playerName = getPlayerRef().getUsername();
                }
                
                // Obtém o saldo do jogador
                double balance = economyManager.getBalance(playerUuid);
                String formattedBalance = CurrencyFormatter.format(balance);
                
                // Detecta aumento no saldo
                if (lastBalance >= 0 && balance > lastBalance) {
                    currentGain = balance - lastBalance;
                }
                lastBalance = balance;
                
                // Obtém o rank do jogador (apenas se o money top estiver habilitado)
                String rankText;
                if (Main.CONFIG != null && Main.CONFIG.get() != null && Main.CONFIG.get().isEnableMoneyTop()) {
                    int rank = economyManager.getPlayerRank(playerUuid);
                    if (rank > 0 && rank <= 500) {
                        rankText = "#" + rank;
                    } else {
                        rankText = LanguageManager.getTranslation("hud_rank_unknown");
                    }
                } else {
                    rankText = LanguageManager.getTranslation("hud_rank_unknown");
                }
                
                // Obtém o status da loja do jogador
                boolean isShopOpen = false;
                try {
                    if (Main.CONFIG.get().isEnablePlayerShop()) {
                        isShopOpen = PlayerShopManager.getInstance().isShopOpen(playerUuid);
                    }
                } catch (Exception e) {
                    // Se houver erro ao verificar, assume que está fechada
                }
                
                String shopStatusText;
                if (isShopOpen) {
                    shopStatusText = LanguageManager.getTranslation("hud_shop_status") + ": " + LanguageManager.getTranslation("hud_shop_open");
                } else {
                    shopStatusText = LanguageManager.getTranslation("hud_shop_status") + ": " + LanguageManager.getTranslation("hud_shop_closed");
                }
                
                // Atualiza os labels usando traduções com suporte a códigos de cor
                String nickText = LanguageManager.getTranslation("hud_nick") + " " + playerName;
                String moneyText = LanguageManager.getTranslation("hud_money") + " " + formattedBalance;
                String rankTextFull = LanguageManager.getTranslation("hud_top_rank") + " " + rankText;
                
                // Se contém códigos de cor, usa TextSpans, senão usa Text
                if (nickText.contains("&") || nickText.contains("§")) {
                    builder.set("#NickLabel.TextSpans", MessageFormatter.format(nickText));
                } else {
                    builder.set("#NickLabel.Text", nickText);
                }
                
                if (moneyText.contains("&") || moneyText.contains("§")) {
                    builder.set("#MoneyLabel.TextSpans", MessageFormatter.format(moneyText));
                } else {
                    builder.set("#MoneyLabel.Text", moneyText);
                }
                
                if (rankTextFull.contains("&") || rankTextFull.contains("§")) {
                    builder.set("#RankLabel.TextSpans", MessageFormatter.format(rankTextFull));
                } else {
                    builder.set("#RankLabel.Text", rankTextFull);
                }
                
                // Atualiza o status da loja
                if (shopStatusText.contains("&") || shopStatusText.contains("§")) {
                    builder.set("#ShopStatusLabel.TextSpans", MessageFormatter.format(shopStatusText));
                } else {
                    builder.set("#ShopStatusLabel.Text", shopStatusText);
                }
                
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Error updating default HUD values for player: %s", getPlayerRef().getUsername());
            }
        }

        /**
         * Faz o parsing da linha, substituindo traduções (*chave*) e placeholders (%placeholder%)
         */
        private String parseFieldLine(String line, UUID playerUuid, EconomyManager economyManager) {
            if (line == null || line.isEmpty()) {
                return "";
            }
            
            String result = line;
            
            // Substitui traduções: *chave* -> tradução
            Pattern translationPattern = Pattern.compile("\\*([^*]+)\\*");
            Matcher translationMatcher = translationPattern.matcher(result);
            StringBuffer translationBuffer = new StringBuffer();
            while (translationMatcher.find()) {
                String key = translationMatcher.group(1);
                String translation = LanguageManager.getTranslation(key);
                String replacement = translation != null ? translation : translationMatcher.group(0);
                translationMatcher.appendReplacement(translationBuffer, Matcher.quoteReplacement(replacement));
            }
            translationMatcher.appendTail(translationBuffer);
            result = translationBuffer.toString();
            
            // Substitui placeholders: %placeholder% -> valor
            Pattern placeholderPattern = Pattern.compile("%([^%]+)%");
            Matcher placeholderMatcher = placeholderPattern.matcher(result);
            StringBuffer placeholderBuffer = new StringBuffer();
            while (placeholderMatcher.find()) {
                String placeholder = placeholderMatcher.group(1).toLowerCase();
                String value = getPlaceholderValue(placeholder, playerUuid, economyManager);
                placeholderMatcher.appendReplacement(placeholderBuffer, Matcher.quoteReplacement(value));
            }
            placeholderMatcher.appendTail(placeholderBuffer);
            result = placeholderBuffer.toString();
            
            return result;
        }

        /**
         * Obtém o valor de um placeholder
         */
        private String getPlaceholderValue(String placeholder, UUID playerUuid, EconomyManager economyManager) {
            switch (placeholder) {
                case "player_name":
                    String playerName = economyManager.getPlayerName(playerUuid);
                    if (playerName == null || playerName.isEmpty() || "Desconhecido".equals(playerName)) {
                        playerName = getPlayerRef().getUsername();
                    }
                    return playerName;
                    
                case "balance":
                    double balance = economyManager.getBalance(playerUuid);
                    // Detecta aumento no saldo
                    if (lastBalance >= 0 && balance > lastBalance) {
                        currentGain = balance - lastBalance;
                    }
                    lastBalance = balance;
                    return CurrencyFormatter.format(balance);
                    
                case "cash":
                    int cash = economyManager.getCash(playerUuid);
                    return CurrencyFormatter.formatCash(cash);
                    
                case "player_rank":
                    // Verifica se o money top está habilitado
                    if (Main.CONFIG != null && Main.CONFIG.get() != null && Main.CONFIG.get().isEnableMoneyTop()) {
                        int rank = economyManager.getPlayerRank(playerUuid);
                        if (rank > 0 && rank <= 500) {
                            return "#" + rank;
                        } else {
                            return LanguageManager.getTranslation("hud_rank_unknown");
                        }
                    } else {
                        return LanguageManager.getTranslation("hud_rank_unknown");
                    }
                    
                case "player_shop":
                case "shop_status":
                    boolean isShopOpenStatus = false;
                    try {
                        if (Main.CONFIG.get().isEnablePlayerShop()) {
                            isShopOpenStatus = PlayerShopManager.getInstance().isShopOpen(playerUuid);
                        }
                    } catch (Exception e) {
                        // Se houver erro ao verificar, assume que está fechada
                    }
                    return isShopOpenStatus ? LanguageManager.getTranslation("hud_shop_open") : LanguageManager.getTranslation("hud_shop_closed");
                    
                default:
                    // Placeholder desconhecido, retorna vazio ou o próprio placeholder
                    return "";
            }
        }
    }
}
