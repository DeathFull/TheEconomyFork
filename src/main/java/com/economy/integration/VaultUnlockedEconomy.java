package com.economy.integration;

import com.economy.Main;
import com.economy.economy.EconomyManager;
import com.economy.util.CurrencyFormatter;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Implementação do provedor de economia para VaultUnlocked.
 * Permite que outros plugins acessem o sistema de economia do TheEconomy através do VaultUnlocked.
 * Baseado em: https://tne.gitbook.io/vaultunlocked/hytale/economy-provider
 */
public class VaultUnlockedEconomy implements Economy {

  private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
  private final Main plugin;
  private final EconomyManager economyManager;

  public VaultUnlockedEconomy(Main plugin) {
    this.plugin = plugin;
    this.economyManager = EconomyManager.getInstance();
    logger.at(Level.INFO).log("VaultUnlockedEconomy provider initialized");
  }

  @Override
  public boolean isEnabled() {
    return plugin != null && economyManager != null;
  }

  @Override
  public String getName() {
    return "TheEconomy";
  }

  public boolean hasBankSupport() {
    return false;
  }

  @Override
  public boolean hasSharedAccountSupport() {
    return false;
  }

  @Override
  public boolean hasMultiCurrencySupport() {
    return false;
  }

  public int fractionalDigits() {
    return 2;
  }

  public int fractionalDigits(String currency) {
    return fractionalDigits();
  }

  public String format(double amount) {
    return CurrencyFormatter.format(amount);
  }

  public String format(BigDecimal amount) {
    return format(amount.doubleValue());
  }

  public String format(BigDecimal amount, String currency) {
    return format(amount);
  }

  public String format(String playerName, BigDecimal amount) {
    return format(amount);
  }

  public String format(String playerName, BigDecimal amount, String currency) {
    return format(amount);
  }

  public String currencyNamePlural() {
    return "Money";
  }

  public String currencyNameSingular() {
    return "Money";
  }

  public String defaultCurrencyNameSingular(String world) {
    return currencyNameSingular();
  }

  public String defaultCurrencyNamePlural(String world) {
    return currencyNamePlural();
  }

  public String getDefaultCurrency(String world) {
    return null; // Sem suporte a múltiplas moedas
  }

  public boolean hasCurrency(String currency) {
    return false; // Sem suporte a múltiplas moedas
  }

  public List<String> currencies() {
    return Collections.emptyList();
  }

  public boolean hasAccount(String playerName) {
    UUID uuid = getPlayerUUID(playerName);
    return uuid != null && economyManager.hasPlayerBalance(uuid);
  }

  @Override
  public boolean hasAccount(UUID playerUuid) {
    return economyManager.hasPlayerBalance(playerUuid);
  }

  @Override
  public boolean hasAccount(UUID playerUuid, String world) {
    return hasAccount(playerUuid);
  }

  public boolean hasAccount(String playerName, String worldName) {
    return hasAccount(playerName);
  }

  public double getBalance(String playerName) {
    UUID uuid = getPlayerUUID(playerName);
    if (uuid == null) {
      return 0.0;
    }
    return economyManager.getBalance(uuid);
  }

  public BigDecimal getBalance(UUID playerUuid) {
    return BigDecimal.valueOf(economyManager.getBalance(playerUuid));
  }

  public BigDecimal getBalance(UUID playerUuid, String world) {
    return getBalance(playerUuid);
  }

  public BigDecimal getBalance(String playerName, UUID playerUuid, String world) {
    return getBalance(playerUuid, world);
  }

  public BigDecimal getBalance(String playerName, UUID playerUuid, String world, String currency) {
    return getBalance(playerUuid, world);
  }

  // Método adicional para compatibilidade com versões mais recentes do VaultUnlocked
  public BigDecimal getBalance(String playerName, UUID playerUuid) {
    return getBalance(playerUuid);
  }

  public double getBalance(String playerName, String world) {
    return getBalance(playerName);
  }

  public boolean has(String playerName, double amount) {
    UUID uuid = getPlayerUUID(playerName);
    if (uuid == null) {
      return false;
    }
    return economyManager.hasBalance(uuid, amount);
  }

  public boolean has(UUID playerUuid, BigDecimal amount) {
    return economyManager.hasBalance(playerUuid, amount.doubleValue());
  }

  public boolean has(UUID playerUuid, String world, BigDecimal amount) {
    return has(playerUuid, amount);
  }

  public boolean has(String playerName, UUID playerUuid, String world, BigDecimal amount) {
    return has(playerUuid, world, amount);
  }

  public boolean has(String playerName, UUID playerUuid, String world, String currency, BigDecimal amount) {
    return has(playerUuid, world, amount);
  }

  // Método adicional para compatibilidade com versões mais recentes do VaultUnlocked
  public boolean has(String playerName, UUID playerUuid, BigDecimal amount) {
    return has(playerUuid, amount);
  }

  public boolean has(String playerName, String worldName, double amount) {
    return has(playerName, amount);
  }

  public EconomyResponse withdrawPlayer(String playerName, double amount) {
    UUID uuid = getPlayerUUID(playerName);
    if (uuid == null) {
      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
              "Player not found: " + playerName);
    }

    if (amount < 0) {
      return new EconomyResponse(BigDecimal.ZERO, getBalance(uuid), EconomyResponse.ResponseType.FAILURE,
              "Cannot withdraw negative amounts");
    }

    boolean success = economyManager.subtractBalance(uuid, amount);
    if (success) {
      BigDecimal newBalance = BigDecimal.valueOf(economyManager.getBalance(uuid));
      return new EconomyResponse(BigDecimal.valueOf(amount), newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    } else {
      BigDecimal currentBalance = BigDecimal.valueOf(economyManager.getBalance(uuid));
      return new EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE,
              "Insufficient funds");
    }
  }

  public EconomyResponse withdraw(UUID playerUuid, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      return new EconomyResponse(BigDecimal.ZERO, getBalance(playerUuid), EconomyResponse.ResponseType.FAILURE,
              "Cannot withdraw negative amounts");
    }

    boolean success = economyManager.subtractBalance(playerUuid, amount.doubleValue());
    if (success) {
      BigDecimal newBalance = getBalance(playerUuid);
      return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    } else {
      BigDecimal currentBalance = getBalance(playerUuid);
      return new EconomyResponse(BigDecimal.ZERO, currentBalance, EconomyResponse.ResponseType.FAILURE,
              "Insufficient funds");
    }
  }

  public EconomyResponse withdraw(UUID playerUuid, String world, BigDecimal amount) {
    return withdraw(playerUuid, amount);
  }

  public EconomyResponse withdraw(String playerName, UUID playerUuid, BigDecimal amount) {
    return withdraw(playerUuid, amount);
  }

  public EconomyResponse withdraw(String playerName, UUID playerUuid, String world, BigDecimal amount) {
    return withdraw(playerUuid, world, amount);
  }

  public EconomyResponse withdraw(String playerName,
                                  UUID playerUuid,
                                  String world,
                                  String currency,
                                  BigDecimal amount) {
    return withdraw(playerUuid, world, amount);
  }

  public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
    return withdrawPlayer(playerName, amount);
  }

  public EconomyResponse depositPlayer(String playerName, double amount) {
    UUID uuid = getPlayerUUID(playerName);
    if (uuid == null) {
      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
              "Player not found: " + playerName);
    }

    if (amount < 0) {
      return new EconomyResponse(BigDecimal.ZERO, getBalance(uuid), EconomyResponse.ResponseType.FAILURE,
              "Cannot deposit negative amounts");
    }

    economyManager.addBalance(uuid, amount);
    BigDecimal newBalance = BigDecimal.valueOf(economyManager.getBalance(uuid));
    return new EconomyResponse(BigDecimal.valueOf(amount), newBalance, EconomyResponse.ResponseType.SUCCESS, "");
  }

  public EconomyResponse deposit(UUID playerUuid, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      return new EconomyResponse(BigDecimal.ZERO, getBalance(playerUuid), EconomyResponse.ResponseType.FAILURE,
              "Cannot deposit negative amounts");
    }

    economyManager.addBalance(playerUuid, amount.doubleValue());
    BigDecimal newBalance = getBalance(playerUuid);
    return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
  }

  public EconomyResponse deposit(UUID playerUuid, String world, BigDecimal amount) {
    return deposit(playerUuid, amount);
  }

  public EconomyResponse deposit(String playerName, UUID playerUuid, BigDecimal amount) {
    return deposit(playerUuid, amount);
  }

  public EconomyResponse deposit(String playerName, UUID playerUuid, String world, BigDecimal amount) {
    return deposit(playerUuid, world, amount);
  }

  public EconomyResponse deposit(String playerName, UUID playerUuid, String world, String currency, BigDecimal amount) {
    return deposit(playerUuid, world, amount);
  }

  public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
    return depositPlayer(playerName, amount);
  }

  public EconomyResponse createPlayerAccount(String playerName) {
    UUID uuid = getPlayerUUID(playerName);
    if (uuid == null) {
      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE,
              "Player not found: " + playerName);
    }

    if (economyManager.hasPlayerBalance(uuid)) {
      BigDecimal balance = getBalance(uuid);
      return new EconomyResponse(BigDecimal.ZERO, balance, EconomyResponse.ResponseType.SUCCESS,
              "Account already exists");
    }

    double initialBalance = Main.CONFIG.get().getInitialBalance();
    economyManager.setBalance(uuid, initialBalance);
    return new EconomyResponse(BigDecimal.ZERO,
            BigDecimal.valueOf(initialBalance),
            EconomyResponse.ResponseType.SUCCESS,
            "Account created");
  }

  @Override
  public boolean createAccount(UUID playerUuid, String world) {
    if (economyManager.hasPlayerBalance(playerUuid)) {
      return true; // Conta já existe
    }

    double initialBalance = Main.CONFIG.get().getInitialBalance();
    economyManager.setBalance(playerUuid, initialBalance);
    return true;
  }

  @Override
  public boolean createAccount(UUID playerUuid, String world, boolean shared) {
    return createAccount(playerUuid, world);
  }

  @Override
  public boolean createAccount(UUID playerUuid, String world, String currency) {
    return createAccount(playerUuid, world);
  }

  @Override
  public boolean createAccount(UUID playerUuid, String world, String currency, boolean shared) {
    return createAccount(playerUuid, world);
  }

  public EconomyResponse createPlayerAccount(String playerName, String worldName) {
    return createPlayerAccount(playerName);
  }

  public boolean deleteAccount(String accountName, UUID playerUuid) {
    // Não suportamos exclusão de contas, apenas zerar saldo
    economyManager.setBalance(playerUuid, 0.0);
    return true;
  }

  public EconomyResponse createBank(String name, String player) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse deleteBank(String name) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse bankBalance(String name) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse bankHas(String name, double amount) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse bankWithdraw(String name, double amount) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse bankDeposit(String name, double amount) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse isBankOwner(String name, String playerName) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public EconomyResponse isBankMember(String name, String playerName) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
            "Banks are not supported");
  }

  public List<String> getBanks() {
    return new ArrayList<>();
  }

  // ========== Shared Account Support (não suportado) ==========

  @Override
  public boolean createSharedAccount(String accountName, UUID ownerUuid, String world, UUID member) {
    return false; // Shared accounts are not supported
  }

  @Override
  public boolean isAccountOwner(String accountName, UUID ownerUuid, UUID playerUuid) {
    return false;
  }

  @Override
  public boolean isAccountMember(String accountName, UUID ownerUuid, UUID playerUuid) {
    return false;
  }

  @Override
  public boolean addAccountMember(String accountName, UUID ownerUuid, UUID playerUuid) {
    return false; // Shared accounts are not supported
  }

  @Override
  public boolean addAccountMember(String accountName, UUID ownerUuid, UUID playerUuid,
                                  net.milkbowl.vault2.economy.AccountPermission... permissions) {
    return false; // Shared accounts are not supported
  }

  @Override
  public boolean removeAccountMember(String accountName, UUID ownerUuid, UUID playerUuid) {
    return false; // Shared accounts are not supported
  }

  @Override
  public boolean setOwner(String accountName, UUID ownerUuid, UUID newOwnerUuid) {
    return false; // Shared accounts are not supported
  }

  @Override
  public boolean updateAccountPermission(String accountName, UUID ownerUuid, UUID playerUuid,
                                         net.milkbowl.vault2.economy.AccountPermission permission, boolean granted) {
    return false; // Shared accounts are not supported
  }

  @Override
  public boolean hasAccountPermission(String accountName, UUID ownerUuid, UUID playerUuid,
                                      net.milkbowl.vault2.economy.AccountPermission permission) {
    return false;
  }

  public boolean renameAccount(String accountName, UUID ownerUuid, String newAccountName) {
    return false; // Shared accounts are not supported
  }

  public boolean renameAccount(String accountName, UUID ownerUuid, String newAccountName, UUID newOwnerUuid) {
    return false; // Shared accounts are not supported
  }

  // Método adicional para compatibilidade com versões mais recentes do VaultUnlocked
  public boolean renameAccount(UUID playerUuid, String newAccountName) {
    return false; // Account renaming is not supported
  }

  @Override
  public boolean accountSupportsCurrency(String accountName, UUID ownerUuid, String currency) {
    return false;
  }

  @Override
  public boolean accountSupportsCurrency(String accountName, UUID ownerUuid, String world, String currency) {
    return false;
  }

  @Override
  public java.util.Optional<String> getAccountName(UUID playerUuid) {
    String name = economyManager.getPlayerName(playerUuid);
    if (name != null && !name.isEmpty() && !"Desconhecido".equals(name)) {
      return java.util.Optional.of(name);
    }
    return java.util.Optional.empty();
  }

  @Override
  public java.util.Map<UUID, String> getUUIDNameMap() {
    // Retorna um mapa vazio - não implementamos cache completo de UUIDs
    return Collections.emptyMap();
  }

  // ========== Helper Methods ==========

  /**
   * Obtém o UUID de um jogador pelo nome.
   * Tenta primeiro no cache do EconomyManager, depois busca no servidor.
   */
  private UUID getPlayerUUID(String playerName) {
    if (playerName == null || playerName.isEmpty()) {
      return null;
    }

    // Tenta buscar no EconomyManager primeiro (cache de jogadores online)
    UUID uuid = economyManager.getPlayerUuidByName(playerName);
    if (uuid != null) {
      return uuid;
    }

    // Se não encontrar, tenta buscar no servidor Hytale
    try {
      HytaleServer server = HytaleServer.get();

      // Busca por todos os jogadores online usando reflexão para acessar o universo
      try {
        Object universe = server.getClass().getMethod("getUniverse").invoke(server);
        if (universe != null) {
          java.util.Collection<?> players = (java.util.Collection<?>)
                  universe.getClass().getMethod("getPlayers").invoke(universe);

          for (Object playerObj : players) {
            if (playerObj instanceof Player player) {
              try {
                PlayerRef playerRef = player.getPlayerRef();
                if (playerRef != null) {
                  String username = playerRef.getUsername();
                  if (username.equalsIgnoreCase(playerName)) {
                    UUID playerUuid = playerRef.getUuid();
                    // Atualiza o cache do EconomyManager
                    economyManager.setPlayerName(playerUuid, username);
                    return playerUuid;
                  }
                }
              } catch (Exception e) {
                // Ignora erros ao processar um jogador específico
              }
            }
          }
        }
      } catch (Exception e) {
        logger.at(Level.FINE).log("Error accessing universe: " + e.getMessage());
      }
    } catch (Exception e) {
      logger.at(Level.FINE).log("Error getting player UUID from server: " + e.getMessage());
    }

    // Se ainda não encontrou, retorna null
    return null;
  }
}

