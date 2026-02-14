package com.economy.systems;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.util.CurrencyFormatter;
import com.economy.util.LanguageManager;
import com.economy.util.NotifyUtils;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.components.PlacedByInteractionComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BlockBreakRewardSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Set<Integer> ORE_BLOCK_IDS = new HashSet<>();
    private static final Set<Integer> WOOD_BLOCK_IDS = new HashSet<>();

    static {
        // IDs de minérios comuns (serão detectados dinamicamente se possível)
        // Por enquanto, vamos usar uma abordagem baseada em verificação de nome do asset
        
        // Madeiras - IDs comuns (podem variar, então vamos tentar detectar pelo tipo)
    }

    public BlockBreakRewardSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull final Store<EntityStore> store, @Nonnull final CommandBuffer<EntityStore> commandBuffer, @Nonnull final BreakBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        
        if (player == null || playerRef == null) {
            return;
        }

        try {
            var targetBlock = event.getTargetBlock();
            var world = player.getWorld();
            
            if (world == null || targetBlock == null) {
                return;
            }

            int x = targetBlock.getX();
            int y = targetBlock.getY();
            int z = targetBlock.getZ();

            // Obtém o ID do bloco na posição (antes de quebrar)
            int blockId = world.getBlock(x, y, z);
            
            if (blockId == 0) {
                return;
            }

            // Verifica se o bloco foi colocado por um jogador ANTES de processar recompensas
            // Se foi, não dá recompensa para evitar exploração (quebrar e recolocar)
            String worldName = world.getName();
            if (BlockPlaceTrackerSystem.wasBlockPlacedByPlayer(worldName, x, y, z)) {
                // Remove do rastreamento quando quebrado
                BlockPlaceTrackerSystem.removePlacedBlock(worldName, x, y, z);
                return;
            }
            
            // Também verifica usando o componente (fallback)
            if (wasBlockPlacedByPlayer(world, x, y, z)) {
                return;
            }

            // Obtém o BlockType do bloco pelo ID
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType = 
                    (com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType)
                    com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap().getAsset(blockId);

            if (blockType == null) {
                return;
            }

            // Obtém o nome do asset usando getId()
            String blockName = "";
            try {
                // Usa getId() que retorna o nome do asset (ex: "Ore_Copper_Stone")
                java.lang.reflect.Method getIdMethod = blockType.getClass().getMethod("getId");
                Object idObj = getIdMethod.invoke(blockType);
                if (idObj != null) {
                    blockName = idObj.toString().toLowerCase();
                } else {
                    blockName = "block_" + blockId;
                }
            } catch (Exception e) {
                blockName = "block_" + blockId;
            }
            
            // Verifica se é minério
            if (Main.CONFIG.get().isEnableOreRewards() && isOreBlock(blockName)) {
                // Obtém o valor específico do minério
                double reward = Main.CONFIG.get().getOreReward(blockName);
                if (reward > 0) {
                    EconomyManager.getInstance().addBalance(playerRef.getUuid(), reward);
                    
                    // Envia notificação de ganho com cores
                    java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                    placeholders.put("amount", CurrencyFormatter.format(reward));
                    String gainMessage = LanguageManager.getTranslation("hud_gain", placeholders);
                    NotifyUtils.sendSuccessNotification(playerRef, gainMessage);
                    
                    if (Main.CONFIG.get().isEnableDebugLogs()) {
                        System.out.println("[EconomySystem] Block broken: " + blockName + " - Reward: " + CurrencyFormatter.format(reward));
                    }
                }
                return;
            }

            // Verifica se é madeira
            if (Main.CONFIG.get().isEnableWoodRewards() && isWoodBlock(blockName)) {
                // Obtém o valor específico da madeira
                double reward = Main.CONFIG.get().getWoodReward(blockName);
                if (reward > 0) {
                    EconomyManager.getInstance().addBalance(playerRef.getUuid(), reward);
                    
                    // Envia notificação de ganho com cores
                    java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                    placeholders.put("amount", CurrencyFormatter.format(reward));
                    String gainMessage = LanguageManager.getTranslation("hud_gain", placeholders);
                    NotifyUtils.sendSuccessNotification(playerRef, gainMessage);
                    
                    if (Main.CONFIG.get().isEnableDebugLogs()) {
                        System.out.println("[EconomySystem] Block broken: " + blockName + " - Reward: " + CurrencyFormatter.format(reward));
                    }
                }
                return;
            }
        } catch (Exception e) {
            // Log do erro para debug
            System.err.println("[EconomySystem] Erro ao processar quebra de bloco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isOreBlock(String blockName) {
        // Verifica pelo nome (se contém "ore" ou "mineral")
        return blockName.contains("ore") || blockName.contains("mineral");
    }

    private boolean isWoodBlock(String blockName) {
        // Verifica pelo nome (se contém "log", "stem", "wood" ou "tree")
        // Exclui blocos que contêm "ore" para evitar falsos positivos
        if (blockName.contains("ore")) {
            return false;
        }
        return blockName.contains("log") || 
               blockName.contains("stem") || 
               blockName.contains("wood") || 
               blockName.contains("tree") ||
               blockName.contains("trunk");
    }

    /**
     * Verifica se um bloco foi colocado por um jogador.
     * Usa o PlacedByInteractionComponent do ChunkStore para determinar isso.
     * 
     * @param world O mundo onde o bloco está
     * @param x Coordenada X do bloco
     * @param y Coordenada Y do bloco
     * @param z Coordenada Z do bloco
     * @return true se o bloco foi colocado por um jogador, false caso contrário
     */
    private boolean wasBlockPlacedByPlayer(World world, int x, int y, int z) {
        try {
            // Verifica se o InteractionModule está disponível
            InteractionModule interactionModule = InteractionModule.get();
            if (interactionModule == null) {
                if (Main.CONFIG.get().isEnableDebugLogs()) {
                    System.out.println("[EconomySystem] InteractionModule não encontrado");
                }
                return false;
            }

            // Obtém o tipo do componente PlacedByInteractionComponent
            ComponentType<ChunkStore, PlacedByInteractionComponent> placedByComponentType = 
                interactionModule.getPlacedByComponentType();
            
            if (placedByComponentType == null) {
                if (Main.CONFIG.get().isEnableDebugLogs()) {
                    System.out.println("[EconomySystem] PlacedByInteractionComponent type não encontrado");
                }
                return false;
            }

            // Usa o BlockModule para obter o componente de forma mais confiável
            BlockModule blockModule = BlockModule.get();
            if (blockModule == null) {
                // Fallback: tenta acessar diretamente o ChunkStore
                return wasBlockPlacedByPlayerDirect(world, x, y, z, placedByComponentType);
            }

            // Usa o método getComponent do BlockModule para obter o componente
            PlacedByInteractionComponent placedByComponent = 
                blockModule.getComponent(placedByComponentType, world, x, y, z);
            
            if (placedByComponent != null) {
                if (Main.CONFIG.get().isEnableDebugLogs()) {
                    System.out.println("[EconomySystem] Bloco em (" + x + ", " + y + ", " + z + ") foi colocado por jogador: " + placedByComponent.getWhoPlacedUuid());
                }
                return true;
            }
            
            return false;
        } catch (Exception e) {
            // Em caso de erro, assume que não foi colocado por jogador para não bloquear recompensas legítimas
            if (Main.CONFIG.get().isEnableDebugLogs()) {
                System.err.println("[EconomySystem] Erro ao verificar se bloco foi colocado por jogador em (" + x + ", " + y + ", " + z + "): " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Método alternativo para verificar se o bloco foi colocado por jogador,
     * acessando diretamente o ChunkStore.
     */
    private boolean wasBlockPlacedByPlayerDirect(World world, int x, int y, int z, 
            ComponentType<ChunkStore, PlacedByInteractionComponent> placedByComponentType) {
        try {
            // Obtém o ChunkStore do mundo
            com.hypixel.hytale.server.core.universe.world.storage.ChunkStore chunkStore = world.getChunkStore();
            if (chunkStore == null) {
                return false;
            }

            // Obtém a referência do chunk
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
            if (chunkRef == null || !chunkRef.isValid()) {
                return false;
            }

            // Obtém o BlockComponentChunk para acessar a referência do bloco
            Store<ChunkStore> chunkStoreStore = chunkStore.getStore();
            com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk blockComponentChunk = 
                chunkStoreStore.getComponent(chunkRef, com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk.getComponentType());
            
            if (blockComponentChunk == null) {
                return false;
            }

            // Obtém a referência do bloco específico
            int blockIndex = ChunkUtil.indexBlockInColumn(x, y, z);
            Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndex);
            if (blockRef == null || !blockRef.isValid()) {
                return false;
            }

            // Tenta obter o PlacedByInteractionComponent
            PlacedByInteractionComponent placedByComponent = 
                chunkStoreStore.getComponent(blockRef, placedByComponentType);
            
            if (placedByComponent != null && Main.CONFIG.get().isEnableDebugLogs()) {
                System.out.println("[EconomySystem] Bloco em (" + x + ", " + y + ", " + z + ") foi colocado por jogador (método direto): " + placedByComponent.getWhoPlacedUuid());
            }
            
            // Se o componente existe, significa que o bloco foi colocado por um jogador
            return placedByComponent != null;
        } catch (Exception e) {
            if (Main.CONFIG.get().isEnableDebugLogs()) {
                System.err.println("[EconomySystem] Erro no método direto: " + e.getMessage());
            }
            return false;
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @NonNullDecl
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}

