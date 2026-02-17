package com.economy.storage;

import com.economy.Main;
import com.economy.config.EconomyConfig;
import com.economy.shop.ShopItem;
import com.economy.shop.ShopTracker;
import com.hypixel.hytale.logger.HytaleLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * HikariCP-based MariaDB storage provider for AdminShop data.
 * <p>
 * Stores shop items and tabs in MariaDB database using HikariCP connection pool.
 * Database: theeconomy
 * Tables: {tablePrefix}_items and {tablePrefix}_tabs
 * <p>
 * Items table columns:
 * - UniqueId (INT PRIMARY KEY)
 * - ItemId (VARCHAR)
 * - Quantity (INT)
 * - PriceSell (DOUBLE)
 * - PriceBuy (DOUBLE)
 * - Tab (VARCHAR)
 * <p>
 * Tabs table columns:
 * - Id (INT PRIMARY KEY AUTO_INCREMENT)
 * - TabName (VARCHAR)
 *
 * @author EconomySystem
 */
public class MySQLShopStorageProvider {

  private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("EconomySystem-MySQL-Shop");

  private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "EconomySystem-MySQL-Shop-IO");
    t.setDaemon(true);
    return t;
  });

  private HikariDataSource dataSource;
  private String itemsTableName;
  private String tabsTableName;
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
        String tablePrefix = config.getMySQLAdminShopTableName();

        // Set table names
        itemsTableName = tablePrefix + "_items";
        tabsTableName = tablePrefix + "_tabs";

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
        hikariConfig.setPoolName("EconomyAdminShopPool");
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

        // Connection established - will log after data is loaded

      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to connect to MariaDB for AdminShop: %s", e.getMessage());
        throw new RuntimeException("MariaDB connection failed", e);
      }
    }, executor);
  }

  private void createTables() throws SQLException {
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
      // Items table with UniqueId as AUTO_INCREMENT
      String createItemsTableSql = String.format("""
              CREATE TABLE IF NOT EXISTS `%s` (
                  UniqueId INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                  ShopId INT NOT NULL DEFAULT 0,
                  ItemId VARCHAR(255) NOT NULL,
                  Quantity INT NOT NULL DEFAULT 1,
                  PriceSell DOUBLE NOT NULL DEFAULT 0.0,
                  PriceBuy DOUBLE NOT NULL DEFAULT 0.0,
                  Tab VARCHAR(255) NOT NULL DEFAULT '',
                  IsConsoleCommand BOOLEAN NOT NULL DEFAULT FALSE,
                  ConsoleCommand TEXT,
                  DisplayName VARCHAR(255) NOT NULL DEFAULT '',
                  INDEX idx_shopid (ShopId)
              )
              """, itemsTableName);

      stmt.execute(createItemsTableSql);

      // Adiciona colunas se não existirem (migração)
      try {
        stmt.execute(String.format("ALTER TABLE `%s` ADD COLUMN IF NOT EXISTS ShopId INT NOT NULL DEFAULT 0",
                itemsTableName));
        stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_shopid ON `%s` (ShopId)", itemsTableName));
      } catch (SQLException e) {
        // Coluna já existe ou índice já existe, ignora
      }
      try {
        stmt.execute(String.format(
                "ALTER TABLE `%s` ADD COLUMN IF NOT EXISTS IsConsoleCommand BOOLEAN NOT NULL DEFAULT FALSE",
                itemsTableName));
      } catch (SQLException e) {
        // Coluna já existe, ignora
      }
      try {
        stmt.execute(String.format("ALTER TABLE `%s` ADD COLUMN IF NOT EXISTS ConsoleCommand TEXT", itemsTableName));
      } catch (SQLException e) {
        // Coluna já existe, ignora
      }
      try {
        stmt.execute(String.format(
                "ALTER TABLE `%s` ADD COLUMN IF NOT EXISTS DisplayName VARCHAR(255) NOT NULL DEFAULT ''",
                itemsTableName));
      } catch (SQLException e) {
        // Coluna já existe, ignora
      }
      try {
        stmt.execute(String.format("ALTER TABLE `%s` ADD COLUMN IF NOT EXISTS UseCash BOOLEAN NOT NULL DEFAULT FALSE",
                itemsTableName));
      } catch (SQLException e) {
        // Coluna já existe, ignora
      }

      // Tabs table
      String createTabsTableSql = String.format("""
              CREATE TABLE IF NOT EXISTS `%s` (
                  Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                  ShopId INT NOT NULL DEFAULT 0,
                  TabName VARCHAR(255) NOT NULL,
                  UNIQUE KEY unique_shop_tab (ShopId, TabName),
                  INDEX idx_shopid (ShopId)
              )
              """, tabsTableName);

      stmt.execute(createTabsTableSql);

      // Adiciona coluna ShopId se não existir (migração)
      try {
        stmt.execute(String.format("ALTER TABLE `%s` ADD COLUMN IF NOT EXISTS ShopId INT NOT NULL DEFAULT 0",
                tabsTableName));
        // Remove constraint UNIQUE antiga se existir e cria nova com ShopId
        try {
          stmt.execute(String.format("ALTER TABLE `%s` DROP INDEX IF EXISTS TabName", tabsTableName));
        } catch (SQLException e) {
          // Ignora se não existir
        }
        stmt.execute(String.format("CREATE UNIQUE INDEX IF NOT EXISTS unique_shop_tab ON `%s` (ShopId, TabName)",
                tabsTableName));
        stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_shopid ON `%s` (ShopId)", tabsTableName));
      } catch (SQLException e) {
        // Coluna já existe ou índice já existe, ignora
      }

      // Tables created/verified silently
    }
  }


  public CompletableFuture<Void> loadShopData(@Nonnull ShopTracker tracker) {
    return loadShopData(tracker, 0);
  }

  public CompletableFuture<Void> loadShopData(@Nonnull ShopTracker tracker, int shopId) {
    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        // Load tabs first
        String tabsSql = String.format("SELECT TabName FROM `%s` WHERE ShopId = ? ORDER BY Id", tabsTableName);
        try (PreparedStatement ps = conn.prepareStatement(tabsSql)) {
          ps.setInt(1, shopId);
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String tabName = rs.getString("TabName");
              if (tabName != null && !tabName.isEmpty()) {
                tracker.addTab(tabName);
              }
            }
          }
        }

        // Load items
        String itemsSql = String.format(
                "SELECT UniqueId, ItemId, Quantity, PriceSell, PriceBuy, Tab, IsConsoleCommand, ConsoleCommand, " +
                        "DisplayName, UseCash FROM `%s` WHERE ShopId = ?",
                itemsTableName);
        try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
          ps.setInt(1, shopId);
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              int uniqueId = rs.getInt("UniqueId");
              String itemId = rs.getString("ItemId");
              int quantity = rs.getInt("Quantity");
              double priceSell = rs.getDouble("PriceSell");
              double priceBuy = rs.getDouble("PriceBuy");
              String tab = rs.getString("Tab");

              ShopItem item = new ShopItem(uniqueId, itemId, quantity, priceSell, priceBuy, tab != null ? tab : "");

              // Carrega campos de comando console e useCash (se existirem)
              try {
                boolean isConsoleCommand = rs.getBoolean("IsConsoleCommand");
                String consoleCommand = rs.getString("ConsoleCommand");
                String displayName = rs.getString("DisplayName");
                boolean useCash = rs.getBoolean("UseCash");

                item.setConsoleCommand(isConsoleCommand);
                item.setConsoleCommand(consoleCommand != null ? consoleCommand : "");
                item.setDisplayName(displayName != null ? displayName : "");
                item.setUseCash(useCash);
              } catch (SQLException e) {
                // Campos não existem ainda (migração), usa valores padrão
              }

              tracker.addItem(item);
            }
          }
        }

        LOGGER.at(Level.INFO)
                .log("MySQL AdminShop (shopId %d): %s:%d/%s (%d items, %d tabs)",
                        shopId,
                        host,
                        port,
                        database,
                        tracker.getAllItems().size(),
                        tracker.getTabs().size());

      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to load shop data from MySQL (shopId %d): %s", shopId, e.getMessage());
      }
    }, executor);
  }

  public CompletableFuture<ShopItem> addItem(@Nonnull ShopItem item) {
    return addItem(item, 0);
  }

  public CompletableFuture<ShopItem> addItem(@Nonnull ShopItem item, int shopId) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        // Don't include UniqueId in INSERT - let MySQL AUTO_INCREMENT handle it
        String sql = String.format("""
                INSERT INTO `%s` (ShopId, ItemId, Quantity, PriceSell, PriceBuy, Tab, IsConsoleCommand, ConsoleCommand, DisplayName, UseCash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, itemsTableName);

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
          ps.setInt(1, shopId);
          ps.setString(2, item.getItemId());
          ps.setInt(3, item.getQuantity());
          ps.setDouble(4, item.getPriceSell());
          ps.setDouble(5, item.getPriceBuy());
          ps.setString(6, item.getTab() != null ? item.getTab() : "");
          ps.setBoolean(7, item.isConsoleCommand());
          ps.setString(8, item.getConsoleCommand() != null ? item.getConsoleCommand() : "");
          ps.setString(9, item.getDisplayName() != null ? item.getDisplayName() : "");
          ps.setBoolean(10, item.isUseCash());
          ps.executeUpdate();

          // Get the generated UniqueId
          try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
            if (generatedKeys.next()) {
              int generatedId = generatedKeys.getInt(1);
              item.setUniqueId(generatedId);
            }
          }
        }

        return item;
      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to add item to MySQL (shopId %d): %s", shopId, e.getMessage());
        throw new RuntimeException("Failed to add item", e);
      }
    }, executor);
  }

  public CompletableFuture<Boolean> removeItem(int uniqueId) {
    return removeItem(uniqueId, 0);
  }

  public CompletableFuture<Boolean> removeItem(int uniqueId, int shopId) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        String sql = String.format("DELETE FROM `%s` WHERE UniqueId = ? AND ShopId = ?", itemsTableName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
          ps.setInt(1, uniqueId);
          ps.setInt(2, shopId);
          int rowsAffected = ps.executeUpdate();
          return rowsAffected > 0;
        }
      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to remove item from MySQL (shopId %d): %s", shopId, e.getMessage());
        return false;
      }
    }, executor);
  }

  public CompletableFuture<Boolean> updateItem(@Nonnull ShopItem item) {
    return updateItem(item, 0);
  }

  public CompletableFuture<Boolean> updateItem(@Nonnull ShopItem item, int shopId) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        String sql = String.format("""
                UPDATE `%s` 
                SET ItemId = ?, Quantity = ?, PriceSell = ?, PriceBuy = ?, Tab = ?, IsConsoleCommand = ?, ConsoleCommand = ?, DisplayName = ?, UseCash = ?
                WHERE UniqueId = ? AND ShopId = ?
                """, itemsTableName);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
          ps.setString(1, item.getItemId());
          ps.setInt(2, item.getQuantity());
          ps.setDouble(3, item.getPriceSell());
          ps.setDouble(4, item.getPriceBuy());
          ps.setString(5, item.getTab() != null ? item.getTab() : "");
          ps.setBoolean(6, item.isConsoleCommand());
          ps.setString(7, item.getConsoleCommand() != null ? item.getConsoleCommand() : "");
          ps.setString(8, item.getDisplayName() != null ? item.getDisplayName() : "");
          ps.setBoolean(9, item.isUseCash());
          ps.setInt(10, item.getUniqueId());
          ps.setInt(11, shopId);
          int rowsAffected = ps.executeUpdate();
          return rowsAffected > 0;
        }
      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to update item in MySQL (shopId %d): %s", shopId, e.getMessage());
        return false;
      }
    }, executor);
  }

  public CompletableFuture<Void> createTab(@Nonnull String tabName) {
    return createTab(tabName, 0);
  }

  public CompletableFuture<Void> createTab(@Nonnull String tabName, int shopId) {
    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        String sql = String.format("INSERT INTO `%s` (ShopId, TabName) VALUES (?, ?)", tabsTableName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
          ps.setInt(1, shopId);
          ps.setString(2, tabName);
          ps.executeUpdate();
        }
      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to create tab in MySQL (shopId %d): %s", shopId, e.getMessage());
        throw new RuntimeException("Failed to create tab", e);
      }
    }, executor);
  }

  public CompletableFuture<Boolean> removeTab(@Nonnull String tabName) {
    return removeTab(tabName, 0);
  }

  public CompletableFuture<Boolean> removeTab(@Nonnull String tabName, int shopId) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection()) {
        String sql = String.format("DELETE FROM `%s` WHERE TabName = ? AND ShopId = ?", tabsTableName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
          ps.setString(1, tabName);
          ps.setInt(2, shopId);
          int rowsAffected = ps.executeUpdate();
          return rowsAffected > 0;
        }
      } catch (SQLException e) {
        LOGGER.at(Level.SEVERE).log("Failed to remove tab from MySQL (shopId %d): %s", shopId, e.getMessage());
        return false;
      }
    }, executor);
  }

  public CompletableFuture<Void> saveAll(@Nonnull ShopTracker tracker) {
    // For now, we don't need to save all since each operation is saved immediately
    // This method can be used for bulk operations if needed in the future
    return CompletableFuture.completedFuture(null);
  }

  public CompletableFuture<Void> shutdown() {
    return CompletableFuture.runAsync(() -> {
      try {
        if (dataSource != null && !dataSource.isClosed()) {
          dataSource.close();
        }
        executor.shutdown();
        LOGGER.at(Level.INFO).log("MySQL AdminShop HikariCP pool closed");
      } catch (Exception e) {
        LOGGER.at(Level.WARNING).log("Error closing MySQL AdminShop HikariCP pool: %s", e.getMessage());
      }
    });
  }

  public String getName() {
    return String.format("MySQL (%s, %s)", itemsTableName, tabsTableName);
  }
}

