package com.economy.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema que rastreia blocos colocados por jogadores.
 * Armazena as posições dos blocos colocados para que o BlockBreakRewardSystem
 * possa verificar se um bloco foi colocado por um jogador antes de dar recompensa.
 */
public class BlockPlaceTrackerSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    // Armazena as posições dos blocos colocados por jogadores
    // Formato: "worldName:x:y:z" -> true
    private static final Set<String> PLACED_BLOCKS = ConcurrentHashMap.newKeySet();

    public BlockPlaceTrackerSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk, 
                      @Nonnull final Store<EntityStore> store, 
                      @Nonnull final CommandBuffer<EntityStore> commandBuffer, 
                      @Nonnull final PlaceBlockEvent event) {
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
            String worldName = world.getName();

            // Armazena a posição do bloco colocado
            String blockKey = createBlockKey(worldName, x, y, z);
            PLACED_BLOCKS.add(blockKey);
        } catch (Exception e) {
            // Log do erro para debug
            System.err.println("[EconomySystem] Erro ao rastrear colocação de bloco: " + e.getMessage());
        }
    }

    /**
     * Verifica se um bloco foi colocado por um jogador.
     * 
     * @param worldName Nome do mundo
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @return true se o bloco foi colocado por um jogador
     */
    public static boolean wasBlockPlacedByPlayer(String worldName, int x, int y, int z) {
        String blockKey = createBlockKey(worldName, x, y, z);
        return PLACED_BLOCKS.contains(blockKey);
    }

    /**
     * Remove um bloco do rastreamento (quando é quebrado).
     * 
     * @param worldName Nome do mundo
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     */
    public static void removePlacedBlock(String worldName, int x, int y, int z) {
        String blockKey = createBlockKey(worldName, x, y, z);
        PLACED_BLOCKS.remove(blockKey);
    }

    /**
     * Cria uma chave única para identificar um bloco.
     */
    private static String createBlockKey(String worldName, int x, int y, int z) {
        return worldName + ":" + x + ":" + y + ":" + z;
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

