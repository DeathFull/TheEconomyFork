package com.economy.storage;

import com.economy.Main;
import com.economy.config.EconomyConfig;
import com.economy.playershop.PlayerShopItem;
import com.economy.playershop.PlayerShopPlayer;
import com.economy.playershop.PlayerShopTracker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * HikariCP-based MariaDB storage provider for PlayerShop data.
 *
 * Stores player shop info and items in MariaDB database using HikariCP connection pool.
 * Database: theeconomy
 * Tables: {tablePrefix}_info and {tablePrefix}_items
 * 
 * Info table columns:
 * - UUID (VARCHAR(36) PRIMARY KEY)
 * - NickName (VARCHAR(64))
 * - CustomName (VARCHAR(255))
 * - ShopIcon (VARCHAR(255))
 * - isOpen (BOOLEAN)
 * - Tabs (JSON/TEXT) - Array of tab names as JSON
 * 
 * Items table columns:
 * - UniqueId (INT PRIMARY KEY AUTO_INCREMENT)
 * - ItemId (VARCHAR(255))
 * - PriceBuy (DOUBLE)
 * - PriceSell (DOUBLE)
 * - Durability (DOUBLE)
 * - Stock (INT)
 * - Tab (VARCHAR(255))
 * - OwnerUuid (VARCHAR(36)) - Foreign key to info table
 * 
 * @author EconomySystem
 */
public class MySQLPlayerShopStorageProvider {
    
    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("EconomySystem-MySQL-PlayerShop");
    private static final Gson GSON = new Gson();
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {}.getType();
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EconomySystem-MySQL-PlayerShop-IO");
        t.setDaemon(true);
        return t;
    });
    
    private HikariDataSource dataSource;
    private String infoTableName;
    private String itemsTableName;
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
                String tablePrefix = config.getMySQLPlayerShopTableName();
                
                // Set table names
                infoTableName = tablePrefix + "_info";
                itemsTableName = tablePrefix + "_items";
                
                // First, create database if it doesn't exist using temporary connection
                String urlNoDb = String.format("jdbc:mariadb://%s:%d?useSSL=false&allowPublicKeyRetrieval=true",
                    host, port);
                
                try (Connection tempConnection = DriverManager.getConnection(urlNoDb, username, password);
                     Statement stmt = tempConnection.createStatement()) {
                    
                    // Create database if it doesn't exist
                    String createDbSql = "CREATE DATABASE IF NOT EXISTS `" + database + "`";
                    stmt.executeUpdate(createDbSql);
                }
                
                // Configure HikariCP for MariaDB
                HikariConfig hikariConfig = new HikariConfig();

                // JDBC URL for MariaDB
                String jdbcUrl = String.format("jdbc:mariadb://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                    host, port, database);
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);

                // Pool settings
                hikariConfig.setPoolName("EconomyPlayerShopPool");
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
                LOGGER.at(Level.SEVERE).log("Failed to connect to MariaDB for PlayerShop: %s", e.getMessage());
                throw new RuntimeException("MariaDB connection failed", e);
            }
        }, executor);
    }
    
    private void createTables() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            LOGGER.at(Level.SEVERE).log("Cannot create tables: dataSource is null or closed");
            throw new SQLException("DataSource is null or closed");
        }
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Info table
            String createInfoTableSql = String.format("""
                CREATE TABLE IF NOT EXISTS `%s` (
                    UUID VARCHAR(36) NOT NULL PRIMARY KEY,
                    NickName VARCHAR(64) NOT NULL DEFAULT '',
                    CustomName VARCHAR(255) NOT NULL DEFAULT '',
                    ShopIcon VARCHAR(255) NOT NULL DEFAULT '',
                    isOpen BOOLEAN NOT NULL DEFAULT FALSE,
                    Tabs TEXT NOT NULL
                )
                """, infoTableName);
            
            stmt.execute(createInfoTableSql);
            
            // Items table with UniqueId as AUTO_INCREMENT
            String createItemsTableSql = String.format("""
                CREATE TABLE IF NOT EXISTS `%s` (
                    UniqueId INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    ItemId VARCHAR(255) NOT NULL,
                    PriceBuy DOUBLE NOT NULL DEFAULT 0.0,
                    PriceSell DOUBLE NOT NULL DEFAULT 0.0,
                    Durability DOUBLE NOT NULL DEFAULT 0.0,
                    MaxDurability DOUBLE NOT NULL DEFAULT 0.0,
                    Stock INT NOT NULL DEFAULT 0,
                    Tab VARCHAR(255) NOT NULL DEFAULT '',
                    OwnerUuid VARCHAR(36) NOT NULL,
                    INDEX idx_owner (OwnerUuid)
                )
                """, itemsTableName);
            
            stmt.execute(createItemsTableSql);
            
            // Adiciona a coluna MaxDurability se não existir (migração)
            try {
                String alterTableSql = String.format("ALTER TABLE `%s` ADD COLUMN MaxDurability DOUBLE NOT NULL DEFAULT 0.0", itemsTableName);
                stmt.execute(alterTableSql);
            } catch (SQLException e) {
                // Coluna já existe, ignora o erro
            }
            
            // Tables created/verified silently
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Error creating PlayerShop tables: %s", e.getMessage());
            LOGGER.at(Level.SEVERE).log("SQL State: %s, Error Code: %d", e.getSQLState(), e.getErrorCode());
            e.printStackTrace();
            throw e;
        }
    }
    
    public CompletableFuture<Void> loadShopData(@Nonnull PlayerShopTracker tracker) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Load all player shop info
                String infoSql = String.format("SELECT UUID, NickName, CustomName, ShopIcon, isOpen, Tabs FROM `%s`", infoTableName);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(infoSql)) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("UUID"));
                        String nickname = rs.getString("NickName");
                        String customName = rs.getString("CustomName");
                        String shopIcon = rs.getString("ShopIcon");
                        boolean isOpen = rs.getBoolean("isOpen");
                        String tabsJson = rs.getString("Tabs");
                        
                        // Create or update player
                        PlayerShopPlayer player = new PlayerShopPlayer(uuid, nickname != null ? nickname : "");
                        player.setCustomName(customName != null ? customName : "");
                        player.setShopIcon(shopIcon != null ? shopIcon : "");
                        tracker.getPlayers().put(uuid, player);
                        
                        // Set shop open status
                        tracker.setShopOpen(uuid, isOpen);
                        
                        // Load tabs from JSON
                        if (tabsJson != null && !tabsJson.isEmpty() && !tabsJson.equals("[]")) {
                            try {
                                List<String> tabs = GSON.fromJson(tabsJson, LIST_STRING_TYPE);
                                if (tabs != null) {
                                    for (String tab : tabs) {
                                        if (tab != null && !tab.isEmpty()) {
                                            tracker.addTab(uuid, tab);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.at(Level.WARNING).log("Failed to parse tabs JSON for player %s: %s", uuid, e.getMessage());
                            }
                        }
                    }
                }
                
                // Load all items
                String itemsSql = String.format("SELECT UniqueId, ItemId, PriceBuy, PriceSell, Durability, MaxDurability, Stock, Tab, OwnerUuid FROM `%s` ORDER BY UniqueId", itemsTableName);
                int maxUniqueId = 0;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(itemsSql)) {
                    while (rs.next()) {
                        int uniqueId = rs.getInt("UniqueId");
                        String itemId = rs.getString("ItemId");
                        double priceBuy = rs.getDouble("PriceBuy");
                        double priceSell = rs.getDouble("PriceSell");
                        double durability = rs.getDouble("Durability");
                        double maxDurability = 0.0;
                        try {
                            maxDurability = rs.getDouble("MaxDurability");
                        } catch (SQLException e) {
                            // Coluna não existe ainda, mantém 0.0
                        }
                        int stock = rs.getInt("Stock");
                        String tab = rs.getString("Tab");
                        UUID ownerUuid = UUID.fromString(rs.getString("OwnerUuid"));
                        
                        // Track the maximum UniqueId to sync nextUniqueId
                        if (uniqueId > maxUniqueId) {
                            maxUniqueId = uniqueId;
                        }
                        
                        // Note: Quantity is stored as Stock in items table, but we'll use default quantity of 1
                        // IMPORTANTE: Não usa tracker.addItem() porque isso incrementa nextUniqueId
                        // Em vez disso, adiciona diretamente à lista e define o UniqueId manualmente
                        PlayerShopItem item = new PlayerShopItem(uniqueId, itemId, 1, priceBuy, priceSell, ownerUuid, durability, maxDurability, stock, tab != null ? tab : "");
                        tracker.getItems().add(item);
                    }
                }
                
                // Sincroniza o nextUniqueId com o maior ID encontrado no banco + 1
                if (maxUniqueId > 0) {
                    tracker.setNextUniqueId(maxUniqueId + 1);
                }
                
                LOGGER.at(Level.INFO).log("MySQL PlayerShop: %s:%d/%s (%d players, %d items)", host, port, database, tracker.getPlayers().size(), tracker.getAllItems().size());
                
            } catch (SQLException e) {
                LOGGER.at(Level.SEVERE).log("Failed to load PlayerShop data from MySQL: %s", e.getMessage());
            }
        }, executor);
    }
    
    public CompletableFuture<PlayerShopItem> addItem(@Nonnull PlayerShopItem item) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Don't include UniqueId in INSERT - let MySQL AUTO_INCREMENT handle it
                String sql = String.format("""
                    INSERT INTO `%s` (ItemId, PriceBuy, PriceSell, Durability, MaxDurability, Stock, Tab, OwnerUuid)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, itemsTableName);
                
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, item.getItemId());
                    ps.setDouble(2, item.getPriceBuy());
                    ps.setDouble(3, item.getPriceSell());
                    ps.setDouble(4, item.getDurability());
                    ps.setDouble(5, item.getMaxDurability());
                    ps.setInt(6, item.getStock());
                    ps.setString(7, item.getTab() != null ? item.getTab() : "");
                    ps.setString(8, item.getOwnerUuid().toString());
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
                LOGGER.at(Level.SEVERE).log("Failed to add item to MySQL: %s", e.getMessage());
                throw new RuntimeException("Failed to add item", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Boolean> removeItem(int uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Primeiro verifica se o item existe
                String checkSql = String.format("SELECT UniqueId FROM `%s` WHERE UniqueId = ?", itemsTableName);
                boolean itemExists = false;
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, uniqueId);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        itemExists = rs.next();
                    }
                }
                
                if (!itemExists) {
                    return true; // Item não existe, objetivo alcançado
                }
                
                // Remove o item
                String sql = String.format("DELETE FROM `%s` WHERE UniqueId = ?", itemsTableName);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, uniqueId);
                    int rowsAffected = ps.executeUpdate();
                    return rowsAffected > 0;
                }
            } catch (SQLException e) {
                LOGGER.at(Level.SEVERE).log("Failed to remove item from MySQL: %s", e.getMessage());
                return false;
            }
        }, executor);
    }
    
    public CompletableFuture<Boolean> updateItem(@Nonnull PlayerShopItem item) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = String.format("""
                    UPDATE `%s` 
                    SET ItemId = ?, PriceBuy = ?, PriceSell = ?, Durability = ?, MaxDurability = ?, Stock = ?, Tab = ?
                    WHERE UniqueId = ?
                    """, itemsTableName);
                
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, item.getItemId());
                    ps.setDouble(2, item.getPriceBuy());
                    ps.setDouble(3, item.getPriceSell());
                    ps.setDouble(4, item.getDurability());
                    ps.setDouble(5, item.getMaxDurability());
                    ps.setInt(6, item.getStock());
                    ps.setString(7, item.getTab() != null ? item.getTab() : "");
                    ps.setInt(8, item.getUniqueId());
                    int rowsAffected = ps.executeUpdate();
                    return rowsAffected > 0;
                }
            } catch (SQLException e) {
                LOGGER.at(Level.SEVERE).log("Failed to update item in MySQL: %s", e.getMessage());
                return false;
            }
        }, executor);
    }
    
    public CompletableFuture<Void> savePlayerInfo(@Nonnull UUID uuid, @Nonnull PlayerShopPlayer player, boolean isOpen, @Nonnull List<String> tabs) {
        return CompletableFuture.runAsync(() -> {
            savePlayerInfoSync(uuid, player, isOpen, tabs);
        }, executor);
    }
    
    /**
     * Salva informações do jogador de forma síncrona (usado durante shutdown)
     */
    public void savePlayerInfoSync(@Nonnull UUID uuid, @Nonnull PlayerShopPlayer player, boolean isOpen, @Nonnull List<String> tabs) {
        // Verifica se a conexão está disponível antes de tentar salvar
        if (dataSource == null || dataSource.isClosed()) {
            LOGGER.at(Level.WARNING).log("Cannot save player info for %s: HikariCP DataSource is closed", uuid);
            return;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            String tabsJson = GSON.toJson(tabs);
            
            String sql = String.format("""
                INSERT INTO `%s` (UUID, NickName, CustomName, ShopIcon, isOpen, Tabs)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    NickName = VALUES(NickName),
                    CustomName = VALUES(CustomName),
                    ShopIcon = VALUES(ShopIcon),
                    isOpen = VALUES(isOpen),
                    Tabs = VALUES(Tabs)
                """, infoTableName);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, player.getNick() != null ? player.getNick() : "");
                ps.setString(3, player.getCustomName() != null ? player.getCustomName() : "");
                ps.setString(4, player.getShopIcon() != null ? player.getShopIcon() : "");
                ps.setBoolean(5, isOpen);
                ps.setString(6, tabsJson);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save player info to MySQL: %s", e.getMessage());
        }
    }
    
    public CompletableFuture<Void> createTab(@Nonnull UUID ownerUuid, @Nonnull String tabName) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Load current tabs
                List<String> tabs = loadTabs(ownerUuid);
                if (!tabs.contains(tabName)) {
                    tabs.add(tabName);
                    saveTabs(ownerUuid, tabs);
                }
            } catch (SQLException e) {
                LOGGER.at(Level.SEVERE).log("Failed to create tab in MySQL: %s", e.getMessage());
                throw new RuntimeException("Failed to create tab", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Boolean> removeTab(@Nonnull UUID ownerUuid, @Nonnull String tabName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load current tabs
                List<String> tabs = loadTabs(ownerUuid);
                if (tabs.remove(tabName)) {
                    saveTabs(ownerUuid, tabs);
                    return true;
                }
                return false;
            } catch (SQLException e) {
                LOGGER.at(Level.SEVERE).log("Failed to remove tab from MySQL: %s", e.getMessage());
                return false;
            }
        }, executor);
    }
    
    private List<String> loadTabs(UUID ownerUuid) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = String.format("SELECT Tabs FROM `%s` WHERE UUID = ?", infoTableName);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String tabsJson = rs.getString("Tabs");
                        if (tabsJson != null && !tabsJson.isEmpty() && !tabsJson.equals("[]")) {
                            try {
                                List<String> tabs = GSON.fromJson(tabsJson, LIST_STRING_TYPE);
                                if (tabs != null) {
                                    return tabs;
                                }
                            } catch (Exception e) {
                                LOGGER.at(Level.WARNING).log("Failed to parse tabs JSON for player %s: %s", ownerUuid, e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }
    
    private void saveTabs(UUID ownerUuid, List<String> tabs) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String tabsJson = GSON.toJson(tabs);
            String sql = String.format("UPDATE `%s` SET Tabs = ? WHERE UUID = ?", infoTableName);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tabsJson);
                ps.setString(2, ownerUuid.toString());
                ps.executeUpdate();
            }
        }
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
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            executor.shutdown();
            LOGGER.at(Level.INFO).log("MySQL PlayerShop HikariCP pool closed");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error closing MySQL PlayerShop HikariCP pool: %s", e.getMessage());
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
        return String.format("MySQL (%s, %s)", infoTableName, itemsTableName);
    }
}


