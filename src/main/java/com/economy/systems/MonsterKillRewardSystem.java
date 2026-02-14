package com.economy.systems;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.LanguageManager;
import com.economy.util.NotifyUtils;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class MonsterKillRewardSystem extends DeathSystems.OnDeathSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull DeathComponent component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!Main.CONFIG.get().isEnableMonsterRewards()) {
            return;
        }

        try {
            // Verifica se a entidade que morreu é um jogador (ignora)
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                return; // É um jogador, ignora
            }

            // Obtém informações sobre a morte
            var deathInfo = component.getDeathInfo();
            if (deathInfo == null) {
                return;
            }

            // Tenta obter o atacante (killer)
            UUID killerUuid = null;
            PlayerRef killerPlayerRef = null;
            if (deathInfo.getSource() instanceof Damage.EntitySource entitySource) {
                var attackerRef = entitySource.getRef();
                if (attackerRef.isValid()) {
                    Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
                    if (attackerPlayer != null) {
                        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
                        if (attackerPlayerRef != null) {
                            killerUuid = attackerPlayerRef.getUuid();
                            killerPlayerRef = attackerPlayerRef;
                        }
                    }
                }
            }

            if (killerUuid == null || killerPlayerRef == null) {
                return; // Não foi morto por um jogador
            }

            // Tenta obter o nome do monstro usando DisplayNameComponent (API oficial do Hytale)
            String monsterId = "unknown_monster";
            String monsterName = "Unknown Monster";
            String monsterModel = "unknown";
            
            try {
                // Usa a API oficial do Hytale para obter DisplayNameComponent
                ComponentType<EntityStore, DisplayNameComponent> nameType = DisplayNameComponent.getComponentType();
                DisplayNameComponent nameComp = store.getComponent(ref, nameType);
                
                if (nameComp != null) {
                    // Obtém o nome de exibição (retorna Message)
                    Message name = nameComp.getDisplayName();
                    if (name != null) {
                        // Extrai o texto do Message
                        String nameStr = extractTextFromMessage(name);
                        if (nameStr != null && !nameStr.isEmpty() && !nameStr.contains("@") && !nameStr.contains("com.hypixel")) {
                            monsterName = nameStr;
                            // Usa o nome como ID também se não tiver outro ID
                            if (monsterId.equals("unknown_monster")) {
                                monsterId = nameStr;
                            }
                        }
                    }
                } else {
                    if (Main.CONFIG.get().isEnableDebugLogs()) {
                        System.out.println("[EconomySystem] DisplayNameComponent not found for entity");
                    }
                }
                
                // Obtém o Model usando ModelComponent
                ComponentType<EntityStore, ModelComponent> modelType = ModelComponent.getComponentType();
                ModelComponent modelComp = store.getComponent(ref, modelType);
                
                if (modelComp != null) {
                    // Obtém o modelo atual
                    Object currentModel = modelComp.getModel();
                    if (currentModel != null) {
                        // Tenta obter o modelAssetId do modelo
                        try {
                            java.lang.reflect.Method getModelAssetIdMethod = currentModel.getClass().getMethod("getModelAssetId");
                            Object modelAssetId = getModelAssetIdMethod.invoke(currentModel);
                            if (modelAssetId != null) {
                                monsterModel = modelAssetId.toString();
                                // Usa o modelAssetId como ID do monstro se ainda não tiver um ID
                                if (monsterId.equals("unknown_monster")) {
                                    monsterId = monsterModel;
                                }
                            }
                        } catch (Exception e) {
                            // Tenta outros métodos possíveis
                            try {
                                java.lang.reflect.Method getIdMethod = currentModel.getClass().getMethod("getId");
                                Object modelId = getIdMethod.invoke(currentModel);
                                if (modelId != null) {
                                    monsterModel = modelId.toString();
                                    if (monsterId.equals("unknown_monster")) {
                                        monsterId = monsterModel;
                                    }
                                }
                            } catch (Exception e2) {
                                // Fallback para toString() apenas para log
                                if (Main.CONFIG.get().isEnableDebugLogs()) {
                                    System.out.println("[EconomySystem] Could not extract modelAssetId, using toString: " + currentModel.toString());
                                }
                            }
                        }
                    }
                } else {
                    if (Main.CONFIG.get().isEnableDebugLogs()) {
                        System.out.println("[EconomySystem] ModelComponent not found for entity");
                    }
                }
            } catch (Exception e) {
                if (Main.CONFIG.get().isEnableDebugLogs()) {
                    System.err.println("[EconomySystem] Error getting entity display name or model: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Obtém a recompensa configurada para este monstro
            Double reward = Main.CONFIG.get().getMonsterReward(monsterId);
            if (reward != null && reward > 0) {
                EconomyManager.getInstance().addBalance(killerUuid, reward);
                
                // Envia notificação de ganho com cores
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("amount", CurrencyFormatter.format(reward));
                String gainMessage = LanguageManager.getTranslation("hud_gain", placeholders);
                NotifyUtils.sendSuccessNotification(killerPlayerRef, gainMessage);
                
                // Log com informações do monstro
                if (Main.CONFIG.get().isEnableDebugLogs()) {
                    System.out.println("[EconomySystem] Monster killed (ID: " + monsterId + ", Model: " + monsterModel + ") - Reward: " + CurrencyFormatter.format(reward));
                }
            } else {
                // Log quando o monstro não tem recompensa configurada
                if (Main.CONFIG.get().isEnableDebugLogs()) {
                    System.out.println("[EconomySystem] Monster killed (ID: " + monsterId + ", Model: " + monsterModel + ") - No Rewards yet");
                }
            }
        } catch (Exception e) {
            if (Main.CONFIG.get().isEnableDebugLogs()) {
                System.err.println("[EconomySystem] Error processing monster kill: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Extrai texto de um objeto Message/FormattedMessage
     */
    private String extractTextFromMessage(Object message) {
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
                    if (text != null && !text.isEmpty() && !text.contains("com.hypixel")) {
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
                    if (text != null && !text.isEmpty() && !text.contains("com.hypixel")) {
                        return text;
                    }
                }
            } catch (Exception e) {
                // Continua tentando
            }
            
            // Tenta toString() e verifica se não é apenas o nome da classe
            String toString = message.toString();
            if (toString != null && !toString.isEmpty() && 
                !toString.startsWith("com.hypixel") && 
                !toString.contains("@") &&
                !toString.equals(message.getClass().getName())) {
                return toString;
            }
        } catch (Exception e) {
            // Ignora
        }
        
        return null;
    }
}
