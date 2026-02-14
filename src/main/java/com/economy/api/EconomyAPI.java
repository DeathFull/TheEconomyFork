package com.economy.api;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.gui.ShopGui;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * API pública do EconomySystem para uso por outros plugins
 * 
 * Exemplo de uso:
 * <pre>
 * EconomyAPI api = EconomyAPI.getInstance();
 * double balance = api.getBalance(playerUUID);
 * api.addBalance(playerUUID, 100.0);
 * api.removeBalance(playerUUID, 50.0);
 * </pre>
 */
public class EconomyAPI {

    private static final EconomyAPI INSTANCE = new EconomyAPI();

    /**
     * Obtém a instância da API
     * @return Instância única da API
     */
    public static EconomyAPI getInstance() {
        return INSTANCE;
    }

    private EconomyAPI() {
        // Singleton
    }

    /**
     * Obtém o saldo de um jogador
     * @param playerUUID UUID do jogador
     * @return Saldo do jogador (0.0 se não tiver saldo registrado)
     */
    public double getBalance(UUID playerUUID) {
        return EconomyManager.getInstance().getBalance(playerUUID);
    }

    /**
     * Obtém o saldo de um jogador pelo nome
     * @param playerName Nome do jogador
     * @return Saldo do jogador ou 0.0 se não encontrado
     */
    public double getBalance(String playerName) {
        UUID uuid = EconomyManager.getInstance().getPlayerUuidByName(playerName);
        if (uuid == null) {
            return 0.0;
        }
        return EconomyManager.getInstance().getBalance(uuid);
    }

    /**
     * Define o saldo de um jogador
     * @param playerUUID UUID do jogador
     * @param amount Valor a definir
     */
    public void setBalance(UUID playerUUID, double amount) {
        EconomyManager.getInstance().setBalance(playerUUID, amount);
    }

    /**
     * Define o saldo de um jogador pelo nome
     * @param playerName Nome do jogador
     * @param amount Valor a definir
     * @return true se o jogador foi encontrado, false caso contrário
     */
    public boolean setBalance(String playerName, double amount) {
        UUID uuid = EconomyManager.getInstance().getPlayerUuidByName(playerName);
        if (uuid == null) {
            return false;
        }
        EconomyManager.getInstance().setBalance(uuid, amount);
        return true;
    }

    /**
     * Adiciona dinheiro ao saldo de um jogador
     * @param playerUUID UUID do jogador
     * @param amount Valor a adicionar
     */
    public void addBalance(UUID playerUUID, double amount) {
        EconomyManager.getInstance().addBalance(playerUUID, amount);
    }

    /**
     * Adiciona dinheiro ao saldo de um jogador pelo nome
     * @param playerName Nome do jogador
     * @param amount Valor a adicionar
     * @return true se o jogador foi encontrado, false caso contrário
     */
    public boolean addBalance(String playerName, double amount) {
        UUID uuid = EconomyManager.getInstance().getPlayerUuidByName(playerName);
        if (uuid == null) {
            return false;
        }
        EconomyManager.getInstance().addBalance(uuid, amount);
        return true;
    }

    /**
     * Remove dinheiro do saldo de um jogador
     * @param playerUUID UUID do jogador
     * @param amount Valor a remover
     * @return true se o jogador tinha saldo suficiente e o dinheiro foi removido, false caso contrário
     */
    public boolean removeBalance(UUID playerUUID, double amount) {
        return EconomyManager.getInstance().subtractBalance(playerUUID, amount);
    }

    /**
     * Remove dinheiro do saldo de um jogador pelo nome
     * @param playerName Nome do jogador
     * @param amount Valor a remover
     * @return true se o jogador foi encontrado e tinha saldo suficiente, false caso contrário
     */
    public boolean removeBalance(String playerName, double amount) {
        UUID uuid = EconomyManager.getInstance().getPlayerUuidByName(playerName);
        if (uuid == null) {
            return false;
        }
        return EconomyManager.getInstance().subtractBalance(uuid, amount);
    }

    /**
     * Verifica se um jogador tem saldo suficiente
     * @param playerUUID UUID do jogador
     * @param amount Valor a verificar
     * @return true se o jogador tem saldo suficiente, false caso contrário
     */
    public boolean hasBalance(UUID playerUUID, double amount) {
        return EconomyManager.getInstance().hasBalance(playerUUID, amount);
    }

    /**
     * Verifica se um jogador tem saldo suficiente pelo nome
     * @param playerName Nome do jogador
     * @param amount Valor a verificar
     * @return true se o jogador foi encontrado e tem saldo suficiente, false caso contrário
     */
    public boolean hasBalance(String playerName, double amount) {
        UUID uuid = EconomyManager.getInstance().getPlayerUuidByName(playerName);
        if (uuid == null) {
            return false;
        }
        return EconomyManager.getInstance().hasBalance(uuid, amount);
    }

    /**
     * Obtém o nome de um jogador pelo UUID
     * @param playerUUID UUID do jogador
     * @return Nome do jogador ou "Desconhecido" se não encontrado
     */
    public String getPlayerName(UUID playerUUID) {
        return EconomyManager.getInstance().getPlayerName(playerUUID);
    }

    /**
     * Obtém o UUID de um jogador pelo nome
     * @param playerName Nome do jogador
     * @return UUID do jogador ou null se não encontrado
     */
    public UUID getPlayerUUID(String playerName) {
        return EconomyManager.getInstance().getPlayerUuidByName(playerName);
    }
    
    /**
     * Obtém o saldo formatado de um jogador (com símbolo e formatação)
     * @param playerUUID UUID do jogador
     * @return Saldo formatado (ex: "$1.000,00")
     */
    public String getFormattedBalance(UUID playerUUID) {
        double balance = getBalance(playerUUID);
        return com.economy.util.CurrencyFormatter.format(balance);
    }
    
    /**
     * Obtém o saldo formatado de um jogador pelo nome
     * @param playerName Nome do jogador
     * @return Saldo formatado ou "0" se o jogador não for encontrado
     */
    public String getFormattedBalance(String playerName) {
        UUID uuid = getPlayerUUID(playerName);
        if (uuid == null) {
            return "0";
        }
        return getFormattedBalance(uuid);
    }

    /**
     * Abre a loja administrativa para um jogador (loja padrão - shopId 0)
     * 
     * @param player Objeto Player do jogador
     * @return true se a loja foi aberta com sucesso, false caso contrário
     * 
     * @example
     * <pre>
     * // Em um evento ou comando de outro plugin:
     * if (sender instanceof Player player) {
     *     EconomyAPI api = EconomyAPI.getInstance();
     *     api.openShop(player);
     * }
     * </pre>
     */
    public boolean openShop(Player player) {
        return openShop(player, 0);
    }
    
    /**
     * Abre a loja administrativa para um jogador
     * 
     * @param player Objeto Player do jogador
     * @param shopId ID da loja (0 para /shop, 1+ para NPCs)
     * @return true se a loja foi aberta com sucesso, false caso contrário
     * 
     * @example
     * <pre>
     * // Em um evento ou comando de outro plugin:
     * if (sender instanceof Player player) {
     *     EconomyAPI api = EconomyAPI.getInstance();
     *     api.openShop(player, 1); // Abre a loja do NPC com shopId 1
     * }
     * </pre>
     */
    public boolean openShop(Player player, int shopId) {
        if (player == null) {
            return false;
        }

        // Verifica se a loja está habilitada
        if (!Main.CONFIG.get().isEnableShop()) {
            return false;
        }

        try {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                return false;
            }

            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return false;
            }

            World world = store.getExternalData().getWorld();
            if (world == null) {
                return false;
            }

            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return false;
            }

            // Abre a loja de forma assíncrona
            world.execute(() -> {
                try {
                    player.getPageManager().openCustomPage(ref, store, 
                        new com.economy.gui.ShopGui(playerRef, CustomPageLifetime.CanDismiss, shopId));
                } catch (Exception e) {
                    // Log do erro se necessário
                    Main.getInstance().getLogger().at(java.util.logging.Level.WARNING)
                        .log("Error opening shop: %s", e.getMessage());
                }
            });

            return true;
        } catch (Exception e) {
            Main.getInstance().getLogger().at(java.util.logging.Level.WARNING)
                .log("Error opening shop for player: %s", e.getMessage());
            return false;
        }
    }
}

