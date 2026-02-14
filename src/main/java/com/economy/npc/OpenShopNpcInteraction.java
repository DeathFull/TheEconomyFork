package com.economy.npc;

import com.economy.api.EconomyAPI;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public class OpenShopNpcInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OpenShopNpcInteraction> CODEC =
            BuilderCodec.builder(OpenShopNpcInteraction.class, OpenShopNpcInteraction::new, SimpleInstantInteraction.CODEC)
                    .build();

    @Override
    protected void firstRun(InteractionType interactionType,
                            InteractionContext interactionContext,
                            CooldownHandler cooldownHandler) {

        CommandBuffer<EntityStore> buffer = interactionContext.getCommandBuffer();
        if (buffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> entityRef = interactionContext.getEntity(); // player ref (quem clicou)
        Player player = buffer.getComponent(entityRef, Player.getComponentType());

        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Tenta obter a entidade alvo (NPC que foi clicado)
        int shopId = 0; // Padrão: loja principal (/shop)
        com.hypixel.hytale.logger.HytaleLogger logger = com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("EconomySystem-NPC");
        
        try {
            // Abordagem: Tenta obter a entidade alvo de várias formas
            Ref<EntityStore> targetRef = null;
            
            // Método 1: getTarget() ou getTargetEntity()
            String[] methodNames = {"getTarget", "getTargetEntity", "getInteractedEntity", "getInteracted", "getEntityTarget"};
            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = interactionContext.getClass().getMethod(methodName);
                    Object result = method.invoke(interactionContext);
                    if (result instanceof Ref) {
                        @SuppressWarnings("unchecked")
                        Ref<EntityStore> ref = (Ref<EntityStore>) result;
                        if (ref != null && ref.isValid()) {
                            // Verifica se não é o player
                            Player testPlayer = buffer.getComponent(ref, Player.getComponentType());
                            if (testPlayer == null) {
                                targetRef = ref;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Tenta próximo método
                }
            }
            
            // Método 2: Tenta obter via campos privados
            if (targetRef == null) {
                try {
                    java.lang.reflect.Field[] fields = interactionContext.getClass().getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.contains("target") || fieldName.contains("entity") || 
                            fieldName.contains("interacted") || fieldName.contains("other")) {
                            try {
                                field.setAccessible(true);
                                Object value = field.get(interactionContext);
                                if (value instanceof Ref) {
                                    @SuppressWarnings("unchecked")
                                    Ref<EntityStore> ref = (Ref<EntityStore>) value;
                                    if (ref != null && ref.isValid()) {
                                        // Verifica se não é o player
                                        Player testPlayer = buffer.getComponent(ref, Player.getComponentType());
                                        if (testPlayer == null) {
                                            targetRef = ref;
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Ignora e tenta próximo campo
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignora
                }
            }
            
            // Método 3: Será usado como fallback mais abaixo (busca por proximidade)
            
            if (targetRef != null && targetRef.isValid()) {
                // Obtém o UUID do NPC
                UUIDComponent npcUuidComponent = buffer.getComponent(targetRef, UUIDComponent.getComponentType());
                if (npcUuidComponent != null) {
                    UUID npcId = npcUuidComponent.getUuid();
                    
                    // Busca o shopId do NPC no ShopNpcManager
                    com.economy.npc.ShopNpcManager npcManager = com.economy.Main.getInstance().getShopNpcManager();
                    com.economy.npc.ShopNpcData npcData = npcManager.getNpc(npcId);
                    
                    if (npcData != null) {
                        shopId = npcData.shopId;
                    } else {
                        logger.at(java.util.logging.Level.WARNING).log("NPC %s not found in ShopNpcManager", npcId);
                    }
                } else {
                    logger.at(java.util.logging.Level.WARNING).log("NPC UUIDComponent is null");
                }
            } else {
                
                // Abordagem alternativa: Busca o NPC mais próximo ao player
                // Isso não é ideal, mas pode funcionar como fallback
                try {
                    // Obtém a posição do player
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent playerTransform = 
                        buffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                    if (playerTransform != null) {
                        com.hypixel.hytale.math.vector.Vector3d playerPos = playerTransform.getPosition();
                        
                        // Busca o NPC mais próximo na lista de NPCs ativos
                        com.economy.npc.ShopNpcManager npcManager = com.economy.Main.getInstance().getShopNpcManager();
                        java.util.List<com.economy.npc.ShopNpcData> allNpcs = npcManager.getAllNpcs();
                        
                        double minDistance = Double.MAX_VALUE;
                        com.economy.npc.ShopNpcData closestNpc = null;
                        
                        for (com.economy.npc.ShopNpcData npc : allNpcs) {
                            double distance = Math.sqrt(
                                Math.pow(npc.x - playerPos.x, 2) + 
                                Math.pow(npc.y - playerPos.y, 2) + 
                                Math.pow(npc.z - playerPos.z, 2)
                            );
                            // Considera apenas NPCs a menos de 5 blocos de distância
                            if (distance < 5.0 && distance < minDistance) {
                                minDistance = distance;
                                closestNpc = npc;
                            }
                        }
                        
                        if (closestNpc != null) {
                            shopId = closestNpc.shopId;
                        }
                    }
                } catch (Exception e) {
                    logger.at(java.util.logging.Level.WARNING).log("Failed alternative approach: %s", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.at(java.util.logging.Level.WARNING).log("Failed to get NPC shopId from interaction: %s", e.getMessage());
        }

        // Abre a loja usando a API existente com o shopId do NPC
        EconomyAPI.getInstance().openShop(player, shopId);

        // Marca como completado (o SimpleInstantInteraction já faz isso automaticamente)
        // Não precisa definir o state manualmente
    }
}

