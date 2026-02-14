package com.economy.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;

import java.util.logging.Level;

/**
 * Classe utilitária para manipulação de inventário de jogadores
 */
public class InventoryHelper {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");

    /**
     * Adiciona um item ao inventário do jogador
     * @param player O jogador
     * @param itemId O ID do item
     * @param quantity A quantidade a adicionar
     * @return true se o item foi adicionado com sucesso, false caso contrário
     */
    public static boolean addItem(Player player, String itemId, int quantity) {
        if (player == null || itemId == null || quantity <= 0) {
            return false;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory == null) {
                logger.at(Level.WARNING).log("Player inventory is null");
                return false;
            }

            // Cria o ItemStack
            ItemStack itemStack = new ItemStack(itemId, quantity);

            // Usa o combined container (hotbar primeiro, depois storage) para adicionar automaticamente
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            ItemStackTransaction transaction = combined.addItemStack(itemStack);

            // Verifica se foi adicionado com sucesso (remainder vazio significa que tudo foi adicionado)
            ItemStack remainder = transaction.getRemainder();
            if (remainder == null || remainder.isEmpty() || remainder.getQuantity() == 0) {
                return true; // Todos os itens foram adicionados
            } else {
                // Se remainder tem a mesma quantidade, significa que NENHUM item foi adicionado
                if (remainder.getQuantity() >= quantity) {
                    logger.at(Level.WARNING).log("Inventory full: Could not add any items. Requested: " + quantity + ", Remaining: " + remainder.getQuantity());
                    return false; // Nenhum item foi adicionado, inventário cheio
                } else {
                    // Alguns itens foram adicionados, mas não todos
                    logger.at(Level.INFO).log("Item partially added. Requested: " + quantity + ", Remaining: " + remainder.getQuantity());
                    return true; // Pelo menos parte foi adicionada
                }
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error adding item to inventory: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove um item do inventário do jogador
     * @param player O jogador
     * @param itemId O ID do item
     * @param quantity A quantidade a remover
     * @return true se o item foi removido com sucesso, false caso contrário
     */
    public static boolean removeItem(Player player, String itemId, int quantity) {
        if (player == null || itemId == null || quantity <= 0) {
            return false;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory == null) {
                logger.at(Level.WARNING).log("Player inventory is null");
                return false;
            }

            // Cria o ItemStack para remover
            ItemStack itemStack = new ItemStack(itemId, quantity);

            // Usa o combined container para remover de qualquer seção
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            ItemStackTransaction transaction = combined.removeItemStack(itemStack);

            // Verifica se foi removido com sucesso
            if (transaction.succeeded()) {
                ItemStack remainder = transaction.getRemainder();
                // Se remainder está vazio ou a quantidade restante é menor que a original, significa que removeu
                if (remainder == null || remainder.isEmpty() || remainder.getQuantity() < quantity) {
                    return true;
                }
            }

            logger.at(Level.WARNING).log("Could not remove " + quantity + "x " + itemId + " from inventory");
            return false;
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error removing item from inventory: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtém a quantidade de um item no inventário do jogador
     * @param player O jogador
     * @param itemId O ID do item
     * @return A quantidade do item no inventário, ou 0 se não encontrado ou erro
     */
    public static int getItemCount(Player player, String itemId) {
        if (player == null || itemId == null) {
            return 0;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory == null) {
                logger.at(Level.WARNING).log("Player inventory is null");
                return 0;
            }

            // Usa o combined container para contar em todas as seções
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            
            // Conta itens usando countItemStacks com predicate
            int count = combined.countItemStacks(itemStack -> 
                itemStack != null && !itemStack.isEmpty() && itemStack.getItemId().equals(itemId)
            );

            return count;
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error getting item count from inventory: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * Verifica se o jogador tem uma quantidade suficiente de um item
     * @param player O jogador
     * @param itemId O ID do item
     * @param quantity A quantidade necessária
     * @return true se o jogador tem a quantidade suficiente, false caso contrário
     */
    public static boolean hasItem(Player player, String itemId, int quantity) {
        return getItemCount(player, itemId) >= quantity;
    }
    
    /**
     * Adiciona um item ao inventário do jogador com durabilidade específica
     * @param player O jogador
     * @param itemId O ID do item
     * @param quantity A quantidade a adicionar
     * @param durability A durabilidade do item (-1.0 = durabilidade cheia/máxima, 0.0 = zerada, valores > 0 = durabilidade específica)
     * @return true se o item foi adicionado com sucesso, false caso contrário
     */
    public static boolean addItem(Player player, String itemId, int quantity, double durability) {
        int added = addItemAndGetQuantity(player, itemId, quantity, durability);
        return added > 0;
    }
    
    /**
     * Adiciona um item ao inventário do jogador com durabilidade específica e retorna a quantidade realmente adicionada
     * @param player O jogador
     * @param itemId O ID do item
     * @param quantity A quantidade a adicionar
     * @param durability A durabilidade do item (-1.0 = durabilidade cheia/máxima, 0.0 = zerada, valores > 0 = durabilidade específica)
     * @return A quantidade de itens realmente adicionados (0 se nenhum foi adicionado)
     */
    public static int addItemAndGetQuantity(Player player, String itemId, int quantity, double durability) {
        if (player == null || itemId == null || quantity <= 0) {
            return 0;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory == null) {
                logger.at(Level.WARNING).log("Player inventory is null");
                return 0;
            }

            // Cria o ItemStack
            ItemStack itemStack = new ItemStack(itemId, quantity);
            
            // Define a durabilidade usando .withDurability() (conforme documentação do Hytale)
            // O método .withDurability() retorna uma nova instância (immutable pattern)
            // Se durability for 0.0, significa durabilidade zerada (item quebrado)
            // Se durability for -1.0, significa usar durabilidade máxima (durabilidade cheia)
            if (durability == -1.0) {
                // Obtém a durabilidade máxima do item para garantir durabilidade cheia
                double maxDurability = com.economy.util.ItemManager.getMaxDurability(itemId);
                if (maxDurability > 0.0) {
                    // Usa a durabilidade máxima do item
                    itemStack = itemStack.withDurability(maxDurability);
                }
                // Se maxDurability for 0.0, o item não tem durabilidade, então não define (deixa o padrão)
            } else {
                // Aplica durabilidade específica (0.0 = zerada, valores > 0 = durabilidade específica)
                itemStack = itemStack.withDurability(durability);
            }

            // Usa o combined container (hotbar primeiro, depois storage) para adicionar automaticamente
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            ItemStackTransaction transaction = combined.addItemStack(itemStack);

            // Verifica se foi adicionado com sucesso (remainder vazio significa que tudo foi adicionado)
            ItemStack remainder = transaction.getRemainder();
            if (remainder == null || remainder.isEmpty() || remainder.getQuantity() == 0) {
                return quantity; // Todos os itens foram adicionados
            } else {
                // Calcula quantos itens foram realmente adicionados
                int remainderQuantity = remainder.getQuantity();
                int added = quantity - remainderQuantity;
                
                // Log detalhado para debug
                logger.at(Level.INFO).log("Item add attempt - Requested: " + quantity + ", Remainder: " + remainderQuantity + ", Calculated Added: " + added);
                
                if (added <= 0) {
                    logger.at(Level.WARNING).log("Inventory full: Could not add any items. Requested: " + quantity + ", Remaining: " + remainderQuantity);
                    return 0; // Nenhum item foi adicionado, inventário cheio
                } else if (added > quantity) {
                    // Proteção: se o cálculo der errado, retorna 0 para evitar problemas
                    logger.at(Level.WARNING).log("Invalid calculation: Added (" + added + ") > Requested (" + quantity + "). Returning 0.");
                    return 0;
                } else {
                    // Alguns itens foram adicionados, mas não todos
                    logger.at(Level.INFO).log("Item partially added. Requested: " + quantity + ", Added: " + added + ", Remaining: " + remainderQuantity);
                    return added; // Retorna a quantidade realmente adicionada
                }
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error adding item to inventory: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Adiciona um item ao inventário do jogador com durabilidade e durabilidade máxima específicas
     * IMPORTANTE: Só restaura a durabilidade máxima se ela for maior que a durabilidade máxima padrão do item
     * Isso evita aumentar a durabilidade máxima além do que foi salvo quando o item foi adicionado
     * @param player O jogador
     * @param itemId O ID do item
     * @param quantity A quantidade a adicionar
     * @param durability A durabilidade atual do item
     * @param maxDurability A durabilidade máxima salva do item (se > 0 e > que a padrão, será aplicada)
     * @return A quantidade de itens realmente adicionados (0 se nenhum foi adicionado)
     */
    public static int addItemAndGetQuantityWithMaxDurability(Player player, String itemId, int quantity, double durability, double maxDurability) {
        if (player == null || itemId == null || quantity <= 0) {
            return 0;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory == null) {
                logger.at(Level.WARNING).log("Player inventory is null");
                return 0;
            }

            // Cria o ItemStack (já vem com durabilidade máxima padrão do item)
            ItemStack itemStack = new ItemStack(itemId, quantity);
            
            // Obtém a durabilidade máxima padrão do item
            double defaultMaxDurability = com.economy.util.ItemManager.getMaxDurability(itemId);
            
            // Sempre aplica a durabilidade máxima salva se fornecida e > 0
            // Isso preserva a durabilidade máxima do item como estava quando foi adicionado à loja
            // (pode ser maior que a padrão se foi aumentada, ou menor se foi reparado)
            if (maxDurability > 0.0) {
                // Se maxDurability > defaultMaxDurability, restaura para o valor maior salvo
                // Se maxDurability < defaultMaxDurability, restaura para o valor menor (após reparos)
                // Se maxDurability == defaultMaxDurability, mantém o padrão (sem mudança)
                itemStack = itemStack.withMaxDurability(maxDurability);
            }
            
            // Define a durabilidade atual
            if (durability >= 0.0) {
                itemStack = itemStack.withDurability(durability);
            }

            // Usa o combined container (hotbar primeiro, depois storage) para adicionar automaticamente
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            ItemStackTransaction transaction = combined.addItemStack(itemStack);

            // Verifica se foi adicionado com sucesso (remainder vazio significa que tudo foi adicionado)
            ItemStack remainder = transaction.getRemainder();
            if (remainder == null || remainder.isEmpty() || remainder.getQuantity() == 0) {
                return quantity; // Todos os itens foram adicionados
            } else {
                // Calcula quantos itens foram realmente adicionados
                int remainderQuantity = remainder.getQuantity();
                int added = quantity - remainderQuantity;
                
                if (added <= 0) {
                    return 0;
                } else if (added > quantity) {
                    return 0;
                } else {
                    return added;
                }
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error adding item to inventory with max durability: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
}

