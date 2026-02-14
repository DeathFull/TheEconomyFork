package com.economy.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;

public class BlockInteractLogSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");

    public BlockInteractLogSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk, 
                      @Nonnull final Store<EntityStore> store, 
                      @Nonnull final CommandBuffer<EntityStore> commandBuffer, 
                      @Nonnull final UseBlockEvent.Pre event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        
        if (player != null && playerRef != null) {
            try {
                var targetBlock = event.getTargetBlock();
                var world = player.getWorld();
                String playerName = player.getDisplayName();
                int blockX = targetBlock.getX();
                int blockY = targetBlock.getY();
                int blockZ = targetBlock.getZ();
                String worldName = world.getName();
                
                // Obtém o ID do bloco na posição
                int blockId = world.getBlock(blockX, blockY, blockZ);
                
                // Tenta obter o nome do bloco usando BlockType
                String blockName = "block_" + blockId;
                try {
                    com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType = 
                        (com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType)
                        com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap().getAsset(blockId);
                    
                    if (blockType != null) {
                        java.lang.reflect.Method getIdMethod = blockType.getClass().getMethod("getId");
                        Object idObj = getIdMethod.invoke(blockType);
                        if (idObj != null) {
                            blockName = idObj.toString();
                        }
                    }
                } catch (Exception e) {
                    // Se falhar, usa o ID numérico
                }
                
                // Log desativado
                // logger.at(Level.INFO).log("[BlockInteract] Player: %s | Block: %s (ID: %d) | Position: (%d, %d, %d) | World: %s", 
                //     playerName, blockName, blockId, blockX, blockY, blockZ, worldName);
            } catch (Exception e) {
                // Log de erro desativado
                // logger.at(Level.WARNING).log("Error logging block interaction: %s", e.getMessage());
            }
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

