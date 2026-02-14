package com.economy.npc;

import com.economy.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ShopNpcManager {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("EconomySystem-NPC");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    // Mapa de NPCs ativos: UUID do NPC -> Ref da entidade
    private final Map<UUID, Ref<EntityStore>> activeNpcs = new ConcurrentHashMap<>();

    public ShopNpcManager() {
        FileUtils.ensureMainDirectory();
        this.dataFile = new File(FileUtils.MAIN_PATH, "shop_npcs.json");
    }

    public boolean hasSavedNpcs() {
        return dataFile.exists() && dataFile.length() > 0;
    }

    public ShopNpcList loadAll() {
        if (!dataFile.exists()) {
            return new ShopNpcList();
        }
        try (FileReader fr = new FileReader(dataFile)) {
            ShopNpcList list = gson.fromJson(fr, ShopNpcList.getTypeToken().getType());
            if (list != null) {
                // Garante que todos os NPCs tenham nome, shopId e model (compatibilidade com formato antigo)
                int maxShopId = 0;
                boolean needsModelUpdate = false;
                for (ShopNpcData npc : list.npcs) {
                    if (npc.name == null || npc.name.trim().isEmpty()) {
                        npc.name = "§eKlops Merchant";
                    } else {
                        // Converte & para § se ainda estiver com &
                        npc.name = npc.name.replace('&', '§');
                    }
                    // Se não tiver shopId, atribui um (compatibilidade com formato antigo)
                    if (npc.shopId <= 0) {
                        maxShopId++;
                        npc.shopId = maxShopId;
                    } else if (npc.shopId > maxShopId) {
                        maxShopId = npc.shopId;
                    }
                    // Se não tiver model, define o padrão
                    if (npc.model == null || npc.model.trim().isEmpty()) {
                        npc.model = "klops_merchant";
                        needsModelUpdate = true;
                    }
                }
                // Se algum NPC recebeu shopId novo ou model, salva para persistir
                boolean needsSave = false;
                for (ShopNpcData npc : list.npcs) {
                    if (npc.shopId <= 0) {
                        maxShopId++;
                        npc.shopId = maxShopId;
                        needsSave = true;
                    }
                }
                if (needsSave || needsModelUpdate) {
                    saveAll(list);
                }
                return list;
            }
            return new ShopNpcList();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to load shop NPCs data: %s", e.getMessage());
            // Tenta carregar formato antigo (compatibilidade)
            try (FileReader fr = new FileReader(dataFile)) {
                ShopNpcData oldData = gson.fromJson(fr, ShopNpcData.class);
                if (oldData != null) {
                    ShopNpcList list = new ShopNpcList();
                    // Garante que o NPC antigo tenha nome e shopId
                    if (oldData.name == null || oldData.name.trim().isEmpty()) {
                        oldData.name = "§eKlops Merchant";
                    } else {
                        // Converte & para § se ainda estiver com &
                        oldData.name = oldData.name.replace('&', '§');
                    }
                    if (oldData.shopId <= 0) {
                        oldData.shopId = 1; // Primeiro NPC recebe shopId 1
                    }
                    // Garante que tenha model
                    if (oldData.model == null || oldData.model.trim().isEmpty()) {
                        oldData.model = "klops_merchant";
                    }
                    list.npcs.add(oldData);
                    saveAll(list); // Salva no novo formato
                    return list;
                }
            } catch (Exception e2) {
                // Ignora
            }
            return new ShopNpcList();
        }
    }

    public void saveAll(ShopNpcList list) {
        try (FileWriter fw = new FileWriter(dataFile)) {
            // Garante que todos os NPCs tenham nome antes de salvar
            for (ShopNpcData npc : list.npcs) {
                if (npc.name == null || npc.name.trim().isEmpty()) {
                    npc.name = "§eKlops Merchant";
                }
            }
            gson.toJson(list, fw);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to save shop NPCs data: %s", e.getMessage());
        }
    }

    public void spawnFromSaved() {
        ShopNpcList list = loadAll();
        if (list.npcs.isEmpty()) {
            return;
        }

        for (ShopNpcData data : list.npcs) {
            try {
                UUID worldUuid = UUID.fromString(data.worldUuid);
                World world = Universe.get().getWorld(worldUuid);
                if (world == null) {
                    continue;
                }
                spawnNpc(world, data);
            } catch (IllegalArgumentException e) {
                LOGGER.at(Level.WARNING).log("Invalid world UUID in shop NPC data: %s", data.worldUuid);
            }
        }
    }

    public void spawnNpc(World world, ShopNpcData data) {
        // Verifica se já existe um NPC com esse ID
        if (activeNpcs.containsKey(data.npcId)) {
            Ref<EntityStore> existingRef = activeNpcs.get(data.npcId);
            if (existingRef != null && existingRef.isValid()) {
                return;
            } else {
                // Remove referência inválida
                activeNpcs.remove(data.npcId);
            }
        }
        
        // Verifica se já existe uma entidade com esse UUID no mundo e remove se necessário
        // Isso será feito dentro do world.execute() abaixo para evitar problemas de thread

        Store<EntityStore> store = world.getEntityStore().getStore();
        if (store == null) {
            LOGGER.at(Level.SEVERE).log("EntityStore is null for world, cannot spawn NPC");
            return;
        }

        world.execute(() -> {
            try {
                // Verifica se já existe uma entidade com esse UUID no mundo e remove se necessário
                try {
                    com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
                    if (entityStore != null) {
                        try {
                            // Tenta obter a referência da entidade existente
                            java.lang.reflect.Method getRefFromUUIDMethod = entityStore.getClass().getMethod("getRefFromUUID", UUID.class);
                            Ref<EntityStore> existingEntityRef = (Ref<EntityStore>) getRefFromUUIDMethod.invoke(entityStore, data.npcId);
                            if (existingEntityRef != null && existingEntityRef.isValid()) {
                                try {
                                    Store<EntityStore> checkStore = entityStore.getStore();
                                    if (checkStore != null) {
                                        RemoveReason[] reasons = RemoveReason.values();
                                        if (reasons.length > 0) {
                                            checkStore.removeEntity(existingEntityRef, reasons[0]);
                                        }
                                    }
                                } catch (Exception e) {
                                    LOGGER.at(Level.WARNING).log("Failed to remove existing entity: %s", e.getMessage());
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            // Método não existe, continua normalmente
                        } catch (Exception e) {
                            LOGGER.at(Level.WARNING).log("Failed to check for existing entity: %s", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // Ignora erros na verificação
                }
                
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

                // Posição
                Vector3d pos = new Vector3d(data.x, data.y, data.z);
                // Rotação: TransformComponent espera Vector3f(x=pitch, y=yaw, z=roll)
                // Agora que corrigimos o salvamento, data.yaw contém o yaw correto e data.pitch contém o pitch correto
                // Para NPCs, geralmente queremos yaw (rotação horizontal) e pitch em 0 (cabeça reta)
                Vector3f rot = new Vector3f(data.pitch, data.yaw, 0.0f);

                TransformComponent transform = new TransformComponent();
                transform.setPosition(pos);
                transform.setRotation(rot);
                
                holder.putComponent(
                        TransformComponent.getComponentType(),
                        transform
                );

                // Adiciona HeadRotation component (hipótese R - baseado no NPCPlugin.spawnEntity)
                // O NPCPlugin.spawnEntity adiciona HeadRotation com a mesma rotação do TransformComponent
                // NPCPlugin usa: holder.addComponent(HeadRotation.getComponentType(), (Component)new HeadRotation(rotation));
                try {
                    Class<?> headRotationClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.HeadRotation");
                    java.lang.reflect.Constructor<?> headRotationConstructor = headRotationClass.getConstructor(Vector3f.class);
                    Object headRotation = headRotationConstructor.newInstance(rot);
                    java.lang.reflect.Method getComponentTypeMethod = headRotationClass.getMethod("getComponentType");
                    ComponentType<EntityStore, ?> headRotationType = (ComponentType<EntityStore, ?>) getComponentTypeMethod.invoke(null);
                    // Usa addComponent via reflexão para evitar problemas de tipo genérico
                    java.lang.reflect.Method addComponentMethod = holder.getClass().getMethod("addComponent", 
                        ComponentType.class, 
                        Class.forName("com.hypixel.hytale.component.Component"));
                    @SuppressWarnings("unchecked")
                    Object result = addComponentMethod.invoke(holder, headRotationType, headRotation);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to add HeadRotation component: %s", e.getMessage());
                }

                // UUID do NPC
                UUIDComponent uuidComponent = new UUIDComponent(data.npcId);
                holder.addComponent(UUIDComponent.getComponentType(), uuidComponent);

                holder.addComponent(
                        NetworkId.getComponentType(),
                        new NetworkId(store.getExternalData().takeNextNetworkId())
                );

                // Nome flutuando (usa o nome do NPC ou padrão)
                // O Nameplate do Hytale não processa códigos de cor da mesma forma que o chat
                // Removemos os códigos de cor para exibir o nome limpo
                String npcName = data.name != null && !data.name.trim().isEmpty() ? data.name : "Klops Merchant";
                // Remove códigos de cor (tanto § quanto &) do nome para exibição no Nameplate
                String nameplateName = npcName.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "").replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
                // Remove códigos de reset também
                nameplateName = nameplateName.replace("§r", "").replace("&r", "");
                nameplateName = nameplateName.trim();
                if (nameplateName.isEmpty()) {
                    nameplateName = "Klops Merchant";
                }
                holder.addComponent(Nameplate.getComponentType(), new Nameplate(nameplateName));

                // Obtém o ModelAsset do modelo especificado (ou padrão)
                String modelName = data.model != null && !data.model.trim().isEmpty() ? data.model : "klops_merchant";
                Object modelAsset = getModelAsset(modelName);
                if (modelAsset == null) {
                    LOGGER.at(Level.SEVERE).log("%s model asset not found! Using default klops_merchant.", modelName);
                    modelAsset = getModelAsset("klops_merchant");
                    if (modelAsset == null) {
                        LOGGER.at(Level.SEVERE).log("klops_merchant model asset not found!");
                        return;
                    }
                }

                // Cria o modelo escalado
                Class<?> modelAssetClass = modelAsset.getClass();
                Class<?> modelClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.Model");
                java.lang.reflect.Method createScaledModelMethod = modelClass.getMethod("createScaledModel", modelAssetClass, float.class);
                Object model = createScaledModelMethod.invoke(null, modelAsset, 1.0f);
                
                if (model == null) {
                    LOGGER.at(Level.SEVERE).log("Failed to create Model from ModelAsset!");
                    return;
                }

                // Adiciona ModelComponent primeiro (necessário para renderização)
                Object modelComponent = null;
                try {
                    java.lang.reflect.Constructor<?> modelComponentConstructor = ModelComponent.class.getConstructor(modelClass);
                    modelComponent = modelComponentConstructor.newInstance(model);
                    holder.addComponent(ModelComponent.getComponentType(), (ModelComponent) modelComponent);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to create ModelComponent: %s", e.getMessage());
                    holder.ensureComponent(ModelComponent.getComponentType());
                    // Obtém o ModelComponent do holder após ensureComponent
                    modelComponent = holder.getComponent(ModelComponent.getComponentType());
                }
                

                // Adiciona PersistentModel - tenta criar de forma simples
                // O ModelComponent já foi adicionado, que é suficiente para renderização
                // O PersistentModel é necessário para persistência, mas pode ser opcional
                try {
                    // Tenta obter ModelReference do Model usando método toModelReference()
                    Object modelReference = null;
                    try {
                        java.lang.reflect.Method toModelReferenceMethod = modelClass.getMethod("toModelReference");
                        modelReference = toModelReferenceMethod.invoke(model);
                    } catch (Exception e) {
                        // Método não existe, tenta criar ModelReference manualmente
                        try {
                            Class<?> modelReferenceClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.Model$ModelReference");
                            java.lang.reflect.Constructor<?> modelRefConstructor = modelReferenceClass.getDeclaredConstructor(modelClass);
                            modelRefConstructor.setAccessible(true);
                            modelReference = modelRefConstructor.newInstance(model);
                        } catch (Exception e2) {
                            // Ignora - tentaremos sem ModelReference
                        }
                    }
                    
                    // Tenta criar PersistentModel
                    if (modelReference != null) {
                        try {
                            java.lang.reflect.Constructor<?> persistentModelConstructor = PersistentModel.class.getConstructor(modelReference.getClass());
                            PersistentModel pm = (PersistentModel) persistentModelConstructor.newInstance(modelReference);
                            holder.addComponent(PersistentModel.getComponentType(), pm);
                        } catch (Exception e) {
                            // Ignora - ModelComponent é suficiente para renderização
                        }
                    }
                } catch (Exception e) {
                    // Ignora - ModelComponent é suficiente para renderização
                }

                // Adiciona BoundingBox para colisão
                holder.ensureComponent(BoundingBox.getComponentType());

                // Componentes de interação (para ser clicável)
                try {
                    // Usa Interactions do módulo de interação (como no NPCDialog)
                    Class<?> interactionsClass = Class.forName("com.hypixel.hytale.server.core.modules.interaction.Interactions");
                    java.lang.reflect.Constructor<?> interactionsConstructor = interactionsClass.getConstructor();
                    Object interactions = interactionsConstructor.newInstance();
                    
                    // Configura a interaction ID para "open_shop_npc"
                    java.lang.reflect.Method setInteractionIdMethod = interactionsClass.getMethod("setInteractionId", 
                        Class.forName("com.hypixel.hytale.protocol.InteractionType"), String.class);
                    Object useInteractionType = Enum.valueOf(
                        (Class<Enum>) Class.forName("com.hypixel.hytale.protocol.InteractionType"), 
                        "Use"
                    );
                    setInteractionIdMethod.invoke(interactions, useInteractionType, "open_shop_npc");
                    
                    // Configura o hint de interação (usando tradução)
                    try {
                        java.lang.reflect.Method setInteractionHintMethod = interactionsClass.getMethod("setInteractionHint", String.class);
                        // Usa a linguagem configurada no servidor
                        String interactionText = com.economy.util.LanguageManager.getTranslation("npc_interaction_open_shop");
                        setInteractionHintMethod.invoke(interactions, interactionText);
                    } catch (Exception e) {
                        // Ignora se não tiver o método
                    }
                    
                    // Adiciona o componente Interactions usando addComponent via reflexão
                    ComponentType<EntityStore, ?> interactionsType = (ComponentType<EntityStore, ?>) interactionsClass.getMethod("getComponentType").invoke(null);
                    // Usa reflexão para chamar addComponent (evita problemas com tipos genéricos)
                    java.lang.reflect.Method addComponentMethod = holder.getClass().getMethod("addComponent", 
                        ComponentType.class, 
                        Class.forName("com.hypixel.hytale.component.Component"));
                    @SuppressWarnings("unchecked")
                    Object result = addComponentMethod.invoke(holder, interactionsType, interactions);

                    // Adiciona Interactable - tenta diferentes caminhos possíveis
                    // Nota: Interactable pode não ser necessário se Interactions já estiver configurado
                    ComponentType<EntityStore, ?> interactableType = null;
                    String[] possiblePaths = {
                        "com.hypixel.hytale.server.core.entity.Interactable",
                        "com.hypixel.hytale.server.core.modules.interaction.Interactable",
                        "com.hypixel.hytale.server.core.modules.entity.component.Interactable"
                    };
                    for (String path : possiblePaths) {
                        try {
                            Class<?> interactableClass = Class.forName(path);
                            // Tenta obter INSTANCE primeiro (como no NPCDialog)
                            try {
                                java.lang.reflect.Field instanceField = interactableClass.getField("INSTANCE");
                                Object interactableInstance = instanceField.get(null);
                                interactableType = (ComponentType<EntityStore, ?>) interactableClass.getMethod("getComponentType").invoke(null);
                                // Usa addComponent via reflexão (reutiliza o método já obtido)
                                @SuppressWarnings("unchecked")
                                Object interactableResult = addComponentMethod.invoke(holder, interactableType, interactableInstance);
                                break; // Sucesso, sai do loop
                            } catch (Exception e1) {
                                // Se não tiver INSTANCE, tenta ensureComponent
                                interactableType = (ComponentType<EntityStore, ?>) interactableClass.getMethod("getComponentType").invoke(null);
                                holder.ensureComponent(interactableType);
                                break; // Sucesso, sai do loop
                            }
                        } catch (Exception e) {
                            // Tenta próximo caminho
                        }
                    }
                    // Interactable não é crítico se Interactions já estiver configurado
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to add interaction components: %s", e.getMessage());
                    e.printStackTrace();
                }

                // Spawn no mundo
                Ref<EntityStore> entityRef = store.addEntity(holder, AddReason.SPAWN);
                

                // Guarda a referência apenas se não for null
                if (entityRef != null && entityRef.isValid()) {
                    activeNpcs.put(data.npcId, entityRef);
                } else {
                    LOGGER.at(Level.SEVERE).log("Failed to spawn NPC %s: entityRef is null or invalid", data.npcId);
                }

            } catch (Exception ex) {
                LOGGER.at(Level.SEVERE).log("Failed to spawn shop NPC: %s", ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private Object getModelAsset(String modelName) {
        try {
            Class<?> modelAssetClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset");
            java.lang.reflect.Method getAssetMapMethod = modelAssetClass.getMethod("getAssetMap");
            Object assetMap = getAssetMapMethod.invoke(null);

            if (assetMap == null) {
                return null;
            }

            // Tenta diferentes variações do nome (começando com minúsculo)
            String[] possibleNames = {
                modelName.toLowerCase(), // Primeiro tenta minúsculo
                modelName,
                modelName.replace("_", ""),
                modelName.toLowerCase().replace("_", ""),
                "klops_merchant",
                "Klops_Merchant",
                "KLOPS_MERCHANT",
                "klopsmerchant",
                "KlopsMerchant",
                "klops_merchant_patrol", // Também tenta com _patrol caso o nome original tenha
                "Klops_Merchant_Patrol"
            };

            Object modelAsset = null;

            // Tenta como Map primeiro
            if (assetMap instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) assetMap;
                for (String name : possibleNames) {
                    modelAsset = map.get(name);
                    if (modelAsset != null) {
                        return modelAsset;
                    }
                }
            } else {
                // Tenta métodos específicos do DefaultAssetMap
                try {
                    // Tenta getAsset(String) - há dois métodos getAsset, tenta ambos
                    java.lang.reflect.Method[] getAssetMethods = assetMap.getClass().getMethods();
                    for (java.lang.reflect.Method method : getAssetMethods) {
                        if (method.getName().equals("getAsset") && method.getParameterCount() == 1) {
                            Class<?> paramType = method.getParameterTypes()[0];
                            if (paramType == String.class || paramType == Object.class || CharSequence.class.isAssignableFrom(paramType)) {
                                for (String name : possibleNames) {
                                    try {
                                        modelAsset = method.invoke(assetMap, name);
                                        if (modelAsset != null) {
                                            return modelAsset;
                                        }
                                    } catch (Exception e) {
                                        // Tenta próximo nome
                                    }
                                }
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    // Ignora erros de reflexão
                }
            }

            return null;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to get model asset: %s", e.getMessage());
            return null;
        }
    }

    public UUID addNpc(Transform playerTransform, String worldUuid, String name) {
        return addNpc(playerTransform, worldUuid, name, "klops_merchant");
    }
    
    public UUID addNpc(Transform playerTransform, String worldUuid, String name, String model) {
        ShopNpcList list = loadAll();
        
        // Gera um shopId único (começando do 1, pois 0 é para /shop)
        int nextShopId = 1;
        for (ShopNpcData npc : list.npcs) {
            if (npc.shopId >= nextShopId) {
                nextShopId = npc.shopId + 1;
            }
        }
        
        // Transform.getRotation() retorna Vector3f(x=pitch, y=yaw, z=roll)
        // Precisamos salvar corretamente: y como yaw (rotação horizontal), x como pitch (inclinação vertical)
        var playerRot = playerTransform.getRotation();
        
        // CORREÇÃO: Transform.getRotation() retorna (x=pitch, y=yaw, z=roll)
        // Salvamos y como yaw (rotação horizontal) e x como pitch (inclinação vertical)
        float yaw = playerRot != null ? playerRot.y : 0.0f;  // y = yaw (rotação horizontal)
        float pitch = playerRot != null ? playerRot.x : 0.0f; // x = pitch (inclinação vertical)
        
        ShopNpcData newNpc = new ShopNpcData(
                UUID.randomUUID(),
                worldUuid,
                playerTransform.getPosition().x,
                playerTransform.getPosition().y,
                playerTransform.getPosition().z,
                yaw,    // yaw correto (rotação horizontal)
                pitch,  // pitch (inclinação vertical)
                name,
                nextShopId,
                model != null && !model.trim().isEmpty() ? model.trim() : "klops_merchant"
        );

        list.npcs.add(newNpc);
        saveAll(list);

        try {
            UUID worldUuidObj = UUID.fromString(worldUuid);
            World world = Universe.get().getWorld(worldUuidObj);
            if (world != null) {
                spawnNpc(world, newNpc);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.at(Level.WARNING).log("Invalid world UUID: %s", worldUuid);
        }

        return newNpc.npcId;
    }
    
    /**
     * Obtém o shopId de um NPC pelo seu UUID
     * @param npcId UUID do NPC
     * @return shopId do NPC ou 0 se não encontrado
     */
    public int getShopId(UUID npcId) {
        ShopNpcData npc = getNpc(npcId);
        return npc != null ? npc.shopId : 0;
    }
    
    /**
     * Obtém o NPC pelo shopId
     * @param shopId ID da loja
     * @return ShopNpcData do NPC ou null se não encontrado
     */
    public ShopNpcData getNpcByShopId(int shopId) {
        return loadAll().npcs.stream()
                .filter(npc -> npc.shopId == shopId)
                .findFirst()
                .orElse(null);
    }

    public boolean removeNpc(UUID npcId) {
        // Obtém o shopId do NPC antes de removê-lo
        ShopNpcList list = loadAll();
        ShopNpcData npcData = list.npcs.stream()
                .filter(npc -> npc.npcId.equals(npcId))
                .findFirst()
                .orElse(null);
        
        if (npcData == null) {
            return false;
        }
        
        int shopId = npcData.shopId;
        
        // Remove do mundo - precisa executar no thread do mundo
        Ref<EntityStore> entityRef = activeNpcs.remove(npcId);
        if (entityRef != null && entityRef.isValid()) {
            try {
                Store<EntityStore> store = entityRef.getStore();
                if (store != null) {
                    World world = store.getExternalData().getWorld();
                    if (world != null) {
                        // Executa a remoção no thread do mundo
                        world.execute(() -> {
                            try {
                                if (entityRef != null && entityRef.isValid()) {
                                    RemoveReason[] reasons = RemoveReason.values();
                                    if (reasons.length > 0) {
                                        store.removeEntity(entityRef, reasons[0]);
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.at(Level.WARNING).log("Failed to remove NPC entity: %s", e.getMessage());
                            }
                        });
                    } else {
                        // Fallback se não conseguir o mundo
                        RemoveReason[] reasons = RemoveReason.values();
                        if (reasons.length > 0) {
                            store.removeEntity(entityRef, reasons[0]);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to remove NPC entity: %s", e.getMessage());
            }
        }

        // Remove do arquivo
        boolean removed = list.npcs.removeIf(npc -> npc.npcId.equals(npcId));
        if (removed) {
            saveAll(list);
            
            // Remove todas as tabs e itens da loja deste NPC
            if (shopId > 0) {
                try {
                    com.economy.shop.ShopManager.getInstance().clearShop(shopId);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to clear shop %d after removing NPC: %s", shopId, e.getMessage());
                }
            }
        }
        return removed;
    }

    public void removeAllNpcs() {
        // Obtém todos os shopIds dos NPCs antes de removê-los
        ShopNpcList list = loadAll();
        java.util.Set<Integer> shopIdsToClear = new java.util.HashSet<>();
        for (ShopNpcData npc : list.npcs) {
            if (npc.shopId > 0) {
                shopIdsToClear.add(npc.shopId);
            }
        }
        
        // Remove todos do mundo - precisa executar no thread do mundo
        List<Ref<EntityStore>> refsToRemove = new ArrayList<>(activeNpcs.values());
        activeNpcs.clear();
        
        // Agrupa por mundo para remover em lote
        Map<World, List<Ref<EntityStore>>> refsByWorld = new HashMap<>();
        for (Ref<EntityStore> ref : refsToRemove) {
            if (ref != null && ref.isValid()) {
                try {
                    Store<EntityStore> store = ref.getStore();
                    if (store != null) {
                        World world = store.getExternalData().getWorld();
                        if (world != null) {
                            refsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(ref);
                        }
                    }
                } catch (Exception e) {
                    // Ignora erros
                }
            }
        }
        
        // Remove em cada mundo
        for (Map.Entry<World, List<Ref<EntityStore>>> entry : refsByWorld.entrySet()) {
            World world = entry.getKey();
            List<Ref<EntityStore>> refs = entry.getValue();
            world.execute(() -> {
                for (Ref<EntityStore> ref : refs) {
                    if (ref != null && ref.isValid()) {
                        try {
                            Store<EntityStore> store = ref.getStore();
                            if (store != null) {
                                RemoveReason[] reasons = RemoveReason.values();
                                if (reasons.length > 0) {
                                    store.removeEntity(ref, reasons[0]);
                                }
                            }
                        } catch (Exception e) {
                            // Ignora erros
                        }
                    }
                }
            });
        }

        // Remove do arquivo
        if (dataFile.exists()) {
            dataFile.delete();
        }
        
        // Remove todas as tabs e itens de todas as lojas dos NPCs
        for (int shopId : shopIdsToClear) {
            try {
                com.economy.shop.ShopManager.getInstance().clearShop(shopId);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to clear shop %d after removing all NPCs: %s", shopId, e.getMessage());
            }
        }
    }

    public List<ShopNpcData> getAllNpcs() {
        return new ArrayList<>(loadAll().npcs);
    }

    public ShopNpcData getNpc(UUID npcId) {
        return loadAll().npcs.stream()
                .filter(npc -> npc.npcId.equals(npcId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Move um NPC para uma nova posição
     * @param npcId UUID do NPC
     * @param newTransform Nova posição e rotação
     * @param newWorldUuid UUID do novo mundo
     * @return true se o NPC foi movido com sucesso
     */
    public boolean moveNpc(UUID npcId, Transform newTransform, String newWorldUuid) {
        ShopNpcList list = loadAll();
        ShopNpcData npcData = list.npcs.stream()
                .filter(npc -> npc.npcId.equals(npcId))
                .findFirst()
                .orElse(null);
        
        if (npcData == null) {
            return false;
        }
        
        // Remove o NPC do mundo atual ANTES de atualizar os dados (se estiver spawnado)
        Ref<EntityStore> entityRef = activeNpcs.remove(npcId);
        if (entityRef != null && entityRef.isValid()) {
            try {
                Store<EntityStore> store = entityRef.getStore();
                if (store != null) {
                    World oldWorld = store.getExternalData().getWorld();
                    if (oldWorld != null) {
                        // Remove do mundo antigo de forma síncrona dentro do execute
                        oldWorld.execute(() -> {
                            try {
                                if (entityRef.isValid()) {
                                    RemoveReason[] reasons = RemoveReason.values();
                                    if (reasons.length > 0) {
                                        store.removeEntity(entityRef, reasons[0]);
                                        LOGGER.at(Level.INFO).log("Removed NPC %s from old world", npcId);
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.at(Level.WARNING).log("Failed to remove NPC from old world: %s", e.getMessage());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to remove NPC entity: %s", e.getMessage());
            }
        } else {
            LOGGER.at(Level.INFO).log("NPC %s was not in activeNpcs, skipping removal", npcId);
        }
        
        // Atualiza os dados do NPC com a nova posição
        npcData.worldUuid = newWorldUuid;
        npcData.x = newTransform.getPosition().x;
        npcData.y = newTransform.getPosition().y;
        npcData.z = newTransform.getPosition().z;
        
        // Atualiza rotação
        var rotation = newTransform.getRotation();
        if (rotation != null) {
            npcData.yaw = rotation.y; // y = yaw (rotação horizontal)
            npcData.pitch = rotation.x; // x = pitch (inclinação vertical)
        }
        
        // Salva no arquivo
        saveAll(list);
        
        // Spawna o NPC novamente na nova posição usando o método spawnNpc existente
        try {
            UUID worldUuidObj = UUID.fromString(newWorldUuid);
            World targetWorld = Universe.get().getWorld(worldUuidObj);
            if (targetWorld != null) {
                // Usa o método spawnNpc existente que já tem toda a lógica correta
                // O spawnNpc já verifica se o NPC já existe e remove antes de spawnar
                spawnNpc(targetWorld, npcData);
                LOGGER.at(Level.INFO).log("NPC %s moved and respawned in world %s at (%.2f, %.2f, %.2f)", 
                    npcId, newWorldUuid, npcData.x, npcData.y, npcData.z);
                return true;
            } else {
                LOGGER.at(Level.WARNING).log("World %s not found for NPC %s", newWorldUuid, npcId);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.at(Level.WARNING).log("Invalid world UUID %s for NPC %s: %s", newWorldUuid, npcId, e.getMessage());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to spawn NPC %s in world %s: %s", npcId, newWorldUuid, e.getMessage());
            e.printStackTrace();
        }
        
        // Se não encontrou o mundo, o NPC será spawnado na próxima vez que spawnFromSaved for chamado
        return true;
    }
    
    private void spawnNpcInWorld(ShopNpcData data, World world) {
        // Reutiliza a lógica do spawnFromSaved, mas para um único NPC em um mundo específico
        com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return;
        }
        
        Store<EntityStore> store = entityStore.getStore();
        if (store == null) {
            return;
        }
        
        world.execute(() -> {
            try {
                // Verifica se já existe uma entidade com esse UUID no mundo
                try {
                    java.lang.reflect.Method getEntityByUuidMethod = store.getClass().getMethod("getEntityByUuid", UUID.class);
                    Ref<EntityStore> existingEntityRef = (Ref<EntityStore>) getEntityByUuidMethod.invoke(store, data.npcId);
                    if (existingEntityRef != null && existingEntityRef.isValid()) {
                        // Remove a entidade existente
                        try {
                            RemoveReason[] reasons = RemoveReason.values();
                            if (reasons.length > 0) {
                                store.removeEntity(existingEntityRef, reasons[0]);
                            }
                        } catch (Exception e) {
                            LOGGER.at(Level.WARNING).log("Failed to remove existing entity: %s", e.getMessage());
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // Método não existe, continua normalmente
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to check for existing entity: %s", e.getMessage());
                }
                
                // Usa a mesma lógica do spawnFromSaved
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

                Vector3d pos = new Vector3d(data.x, data.y, data.z);
                Vector3f rot = new Vector3f(data.pitch, data.yaw, 0.0f);

                TransformComponent transform = new TransformComponent();
                transform.setPosition(pos);
                transform.setRotation(rot);
                
                holder.putComponent(TransformComponent.getComponentType(), transform);

                // HeadRotation
                try {
                    Class<?> headRotationClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.HeadRotation");
                    java.lang.reflect.Constructor<?> headRotationConstructor = headRotationClass.getConstructor(Vector3f.class);
                    Object headRotation = headRotationConstructor.newInstance(rot);
                    java.lang.reflect.Method getComponentTypeMethod = headRotationClass.getMethod("getComponentType");
                    ComponentType<EntityStore, ?> headRotationType = (ComponentType<EntityStore, ?>) getComponentTypeMethod.invoke(null);
                    java.lang.reflect.Method addComponentMethod = holder.getClass().getMethod("addComponent", 
                        ComponentType.class, 
                        Class.forName("com.hypixel.hytale.component.Component"));
                    addComponentMethod.invoke(holder, headRotationType, headRotation);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to add HeadRotation component: %s", e.getMessage());
                }

                UUIDComponent uuidComponent = new UUIDComponent(data.npcId);
                holder.addComponent(UUIDComponent.getComponentType(), uuidComponent);

                holder.addComponent(
                        NetworkId.getComponentType(),
                        new NetworkId(store.getExternalData().takeNextNetworkId())
                );

                String npcName = data.name != null && !data.name.trim().isEmpty() ? data.name : "Klops Merchant";
                String nameplateName = npcName.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "").replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
                nameplateName = nameplateName.replace("§r", "").replace("&r", "").trim();
                if (nameplateName.isEmpty()) {
                    nameplateName = "Klops Merchant";
                }
                holder.addComponent(Nameplate.getComponentType(), new Nameplate(nameplateName));

                String modelName = data.model != null && !data.model.trim().isEmpty() ? data.model : "klops_merchant";
                Object modelAsset = getModelAsset(modelName);
                if (modelAsset == null) {
                    LOGGER.at(Level.SEVERE).log("%s model asset not found! Using default klops_merchant.", modelName);
                    modelAsset = getModelAsset("klops_merchant");
                    if (modelAsset == null) {
                        LOGGER.at(Level.SEVERE).log("klops_merchant model asset not found!");
                        return;
                    }
                }

                Class<?> modelAssetClass = modelAsset.getClass();
                Class<?> modelClass = Class.forName("com.hypixel.hytale.server.core.asset.type.model.config.Model");
                java.lang.reflect.Method createScaledModelMethod = modelClass.getMethod("createScaledModel", modelAssetClass, float.class);
                Object model = createScaledModelMethod.invoke(null, modelAsset, 1.0f);
                
                if (model == null) {
                    LOGGER.at(Level.SEVERE).log("Failed to create Model from ModelAsset!");
                    return;
                }

                Object modelComponent = null;
                try {
                    java.lang.reflect.Constructor<?> modelComponentConstructor = ModelComponent.class.getConstructor(modelClass);
                    modelComponent = modelComponentConstructor.newInstance(model);
                    holder.addComponent(ModelComponent.getComponentType(), (ModelComponent) modelComponent);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to create ModelComponent: %s", e.getMessage());
                    holder.ensureComponent(ModelComponent.getComponentType());
                    modelComponent = holder.getComponent(ModelComponent.getComponentType());
                }

                holder.ensureComponent(BoundingBox.getComponentType());

                // Interactions
                try {
                    Class<?> interactionsClass = Class.forName("com.hypixel.hytale.server.core.modules.interaction.Interactions");
                    java.lang.reflect.Constructor<?> interactionsConstructor = interactionsClass.getConstructor();
                    Object interactions = interactionsConstructor.newInstance();
                    
                    java.lang.reflect.Method setInteractionIdMethod = interactionsClass.getMethod("setInteractionId", 
                        Class.forName("com.hypixel.hytale.protocol.InteractionType"), String.class);
                    Object useInteractionType = Enum.valueOf(
                        (Class<Enum>) Class.forName("com.hypixel.hytale.protocol.InteractionType"), 
                        "Use"
                    );
                    setInteractionIdMethod.invoke(interactions, useInteractionType, "open_shop_npc");
                    
                    try {
                        java.lang.reflect.Method setInteractionHintMethod = interactionsClass.getMethod("setInteractionHint", String.class);
                        String interactionText = com.economy.util.LanguageManager.getTranslation("npc_interaction_open_shop");
                        setInteractionHintMethod.invoke(interactions, interactionText);
                    } catch (Exception e) {
                        // Ignora
                    }
                    
                    ComponentType<EntityStore, ?> interactionsType = (ComponentType<EntityStore, ?>) interactionsClass.getMethod("getComponentType").invoke(null);
                    java.lang.reflect.Method addComponentMethod = holder.getClass().getMethod("addComponent", 
                        ComponentType.class, 
                        Class.forName("com.hypixel.hytale.component.Component"));
                    addComponentMethod.invoke(holder, interactionsType, interactions);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to add interaction components: %s", e.getMessage());
                }

                Ref<EntityStore> entityRef = store.addEntity(holder, AddReason.SPAWN);
                
                if (entityRef != null && entityRef.isValid()) {
                    activeNpcs.put(data.npcId, entityRef);
                    LOGGER.at(Level.INFO).log("NPC %s moved and respawned", data.npcId);
                } else {
                    LOGGER.at(Level.SEVERE).log("Failed to spawn NPC %s: entityRef is null or invalid", data.npcId);
                }

            } catch (Exception ex) {
                LOGGER.at(Level.SEVERE).log("Failed to spawn shop NPC: %s", ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
}
