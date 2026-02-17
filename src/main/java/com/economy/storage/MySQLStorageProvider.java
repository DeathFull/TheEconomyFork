package com.economy.storage;

import com.economy.Main;
import com.economy.config.EconomyConfig;
import com.economy.economy.BalanceTracker;
import com.economy.economy.PlayerBalance;
import com.hypixel.hytale.logger.HytaleLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * HikariCP-based MariaDB storage provider for economy balance data.
 * <p>
 * Stores player balances in a MariaDB database using HikariCP connection pool.
 * Database: theeconomy
 * Table: configurable (default: bank)
 * Columns: UUID (VARCHAR(36) PRIMARY KEY), Nickname (VARCHAR(64)), Balance (DOUBLE)
 *
 * @author EconomySystem
 */
public class MySQLStorageProvider {

  private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("EconomySystem-MySQL");

  private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "EconomySystem-MySQL-IO");
    t.setDaemon(true);
    return t;
  });

  private HikariDataSource dataSource;
  private BalanceTracker balanceTracker;
  private int playerCount = 0;
  private String tableName = "bank"; // Default table name
  private String host;
  private int port;
  private String database;

  public CompletableFuture<Void> initialize() {
    return CompletableFuture.runAsync(() -> {
      try {
        EconomyConfig config = Main.CONFIG.get();

        this.host = config.getMySQLHost();
        this.port = config.getMySQLPort();
        this.database = config.getMySQLDatabaseName(); // Configurable database name
        String username = config.getMySQLUser();
        String password = config.getMySQLPassword();
        tableName = config.getMySQLTableName(); // Get table name from config

        // Configure HikariCP for MariaDB
        HikariConfig hikariConfig = new HikariConfig();

        // JDBC URL for MariaDB
        String jdbcUrl = String.format("jdbc:mariadb://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, database);
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        // Pool settings
        hikariConfig.setPoolName("EconomyBalancePool");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);

        // Timeouts (in milliseconds)
        hikariConfig.setConnectionTimeout(30000);     // 30 seconds
        hikariConfig.setIdleTimeout(600000);          // 10 minutes
        hikariConfig.setMaxLifetime(1800000);         // 30 minutes
        hikariConfig.setValidationTimeout(5000);      // 5 seconds
        hikariConfig.setKeepaliveTime(120000);        // 2 minutes

        // Connection behavior
        hikariConfig.setAutoCommit(true);
        hikariConfig.setConnectionInitSql("SET NAMES utf8mb4");

        // Leak detection (logs warning if connection not returned within threshold)
        hikariConfig.setLeakDetectionThreshold(60000); // 60 seconds

        // MariaDB-specific optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        // Create HikariCP DataSource
        dataSource = new HikariDataSource(hikariConfig);

        // Create tables
        createTables();

        // Initialize balance tracker
        balanceTracker = new BalanceTracker();

        // Load all players from database
        loadAllPlayers();

        LOGGER.at(Level.INFO).log("MySQL connected: %s:%d/%s (%d players)", host, port, database, playerCount);

      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to connect to MariaDB: %s", e.getMessage());
        throw new RuntimeException("MariaDB connection failed", e);
      }
    }, executor);
  }

  private void createTables() throws SQLException {
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
      // Create table with configurable name, UUID as PRIMARY KEY
      String createTableSql = String.format("""
              CREATE TABLE IF NOT EXISTS `%s` (
                  UUID VARCHAR(36) NOT NULL PRIMARY KEY,
                  Nickname VARCHAR(64),
                  Balance DOUBLE DEFAULT 0.0,
                  Cash INT DEFAULT 0
              )
              """, tableName);

      stmt.execute(createTableSql);

      // Table created/verified silently
    }
  }

  private void loadAllPlayers() {
    playerCount = 0; // Reset counter
    balanceTracker = new BalanceTracker(); // Reset tracker
    try (Connection conn = dataSource.getConnection()) {
      String sql = String.format("SELECT UUID, Nickname, Balance, Cash FROM `%s`", tableName);
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
          UUID uuid = UUID.fromString(rs.getString("UUID"));
          String nickname = rs.getString("Nickname");
          double balance = rs.getDouble("Balance");
          int cash = rs.getInt("Cash");

          balanceTracker.setBalance(uuid, balance);
          balanceTracker.setCash(uuid, cash);
          if (nickname != null && !nickname.isEmpty()) {
            balanceTracker.setPlayerNick(uuid, nickname);
          }
          playerCount++;
        }
      }
    } catch (SQLException e) {
      LOGGER.at(Level.SEVERE).log("Failed to load players from MySQL: %s", e.getMessage());
    }
  }

  /**
   * Recarrega todos os dados do MySQL (útil após migração)
   */
  public void reloadAllPlayers() {
    loadAllPlayers();
  }

  public BalanceTracker getBalanceTracker() {
    return balanceTracker;
  }

  public CompletableFuture<PlayerBalance> loadPlayer(@Nonnull UUID playerUuid) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        String sql = String.format("SELECT Nickname, Balance, Cash FROM `%s` WHERE UUID = ?", tableName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
          ps.setString(1, playerUuid.toString());
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              String nickname = rs.getString("Nickname");
              double balance = rs.getDouble("Balance");
              int cash = rs.getInt("Cash");
              PlayerBalance playerBalance = new PlayerBalance(playerUuid,
                      nickname != null ? nickname : "",
                      balance,
                      cash);
              // Add to tracker if not already present
              if (balanceTracker.getBalance(playerUuid) == 0.0) {
                balanceTracker.setBalance(playerUuid, balance);
                balanceTracker.setCash(playerUuid, cash);
                if (nickname != null && !nickname.isEmpty()) {
                  balanceTracker.setPlayerNick(playerUuid, nickname);
                }
              }
              return playerBalance;
            }
          }
        }

        // Create new player with initial balance
        double initialBalance = Main.CONFIG.get().getInitialBalance();
        PlayerBalance newBalance = new PlayerBalance(playerUuid, "", initialBalance, 0);
        balanceTracker.setBalance(playerUuid, initialBalance);
        balanceTracker.setCash(playerUuid, 0);
        savePlayerSync(playerUuid, newBalance);
        playerCount++;
        return newBalance;

      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to load player %s: %s", playerUuid, e.getMessage());
        return new PlayerBalance(playerUuid, "", Main.CONFIG.get().getInitialBalance(), 0);
      }
    }, executor);
  }

  public CompletableFuture<Void> savePlayer(@Nonnull UUID playerUuid, @Nonnull PlayerBalance balance) {
    return CompletableFuture.runAsync(() -> {
      savePlayerSync(playerUuid, balance);
    }, executor);
  }

  private void savePlayerSync(@Nonnull UUID playerUuid, @Nonnull PlayerBalance balance) {
    try (Connection conn = dataSource.getConnection()) {
      // Verifica se o player já existe no tracker
      boolean playerExists = balanceTracker.getBalances().length > 0 &&
              java.util.Arrays.stream(balanceTracker.getBalances())
                      .anyMatch(b -> b.getUuid().equals(playerUuid));

      String sql = String.format("""
              INSERT INTO `%s` (UUID, Nickname, Balance, Cash)
              VALUES (?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE 
                  Nickname = VALUES(Nickname),
                  Balance = VALUES(Balance),
                  Cash = VALUES(Cash)
              """, tableName);

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, playerUuid.toString());
        ps.setString(2, balance.getNick() != null ? balance.getNick() : "");
        ps.setDouble(3, balance.getBalance());
        ps.setInt(4, balance.getCash());
        ps.executeUpdate();

        // Update tracker
        if (!playerExists) {
          // Novo player adicionado, incrementa contador
          playerCount++;
        }
        balanceTracker.setBalance(playerUuid, balance.getBalance());
        balanceTracker.setCash(playerUuid, balance.getCash());
        if (balance.getNick() != null && !balance.getNick().isEmpty()) {
          balanceTracker.setPlayerNick(playerUuid, balance.getNick());
        }
      }
    } catch (SQLException e) {
      LOGGER.at(Level.SEVERE).log("Failed to save player %s: %s", playerUuid, e.getMessage());
    }
  }

  public CompletableFuture<Void> saveAll(@Nonnull Map<UUID, PlayerBalance> dirtyPlayers) {
    if (dirtyPlayers.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<?>[] futures = dirtyPlayers.entrySet().stream()
            .map(entry -> savePlayer(entry.getKey(), entry.getValue()))
            .toArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(futures).thenRun(() -> {
      LOGGER.at(Level.FINE).log("Saved %d players to MySQL", dirtyPlayers.size());
    });
  }

  public CompletableFuture<Void> shutdown() {
    return CompletableFuture.runAsync(() -> {
      shutdownSync();
    }, executor);
  }

  /**
   * Shutdown síncrono (usado durante reload para evitar RejectedExecutionException)
   */
  public void shutdownSync() {
    try {
      // Save all current balances before shutdown (de forma síncrona)
      if (balanceTracker != null) {
        Map<UUID, PlayerBalance> allBalances = new HashMap<>();
        for (PlayerBalance balance : balanceTracker.getBalances()) {
          allBalances.put(balance.getUuid(), balance);
        }
        // Salva de forma síncrona durante shutdown
        try {
          saveAllSync(allBalances);
        } catch (Exception e) {
          LOGGER.at(Level.WARNING).log("Failed to save balances during shutdown: %s", e.getMessage());
        }
      }

      if (dataSource != null && !dataSource.isClosed()) {
        dataSource.close();
      }
      executor.shutdown();
      LOGGER.at(Level.INFO).log("MySQL HikariCP pool closed");
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).log("Error closing MySQL HikariCP pool: %s", e.getMessage());
    }
  }

  /**
   * Salva todos os balances de forma síncrona (usado durante shutdown)
   */
  private void saveAllSync(Map<UUID, PlayerBalance> balances) throws SQLException {
    if (dataSource == null || dataSource.isClosed()) {
      return;
    }

    try (Connection conn = dataSource.getConnection()) {
      String sql = String.format("""
              INSERT INTO `%s` (UUID, Nickname, Balance, Cash)
              VALUES (?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE
                  Nickname = VALUES(Nickname),
                  Balance = VALUES(Balance),
                  Cash = VALUES(Cash)
              """, tableName);

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        for (Map.Entry<UUID, PlayerBalance> entry : balances.entrySet()) {
          UUID uuid = entry.getKey();
          PlayerBalance balance = entry.getValue();

          ps.setString(1, uuid.toString());
          String nickname = balanceTracker.getPlayerNick(uuid);
          ps.setString(2, nickname != null ? nickname : "");
          ps.setDouble(3, balance.getBalance());
          ps.setInt(4, balance.getCash());
          ps.addBatch();
        }
        ps.executeBatch();
      }
    }
  }

  /**
   * Getter pour dataSource (usado durante shutdown)
   */
  public HikariDataSource getDataSource() {
    return dataSource;
  }

  /**
   * Getter para executor (usado durante shutdown)
   */
  public ExecutorService getExecutor() {
    return executor;
  }

  public String getName() {
    return String.format("MySQL (theeconomy.%s)", tableName);
  }

  public int getPlayerCount() {
    return playerCount;
  }
}

