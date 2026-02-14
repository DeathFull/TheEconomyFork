package com.economy.economy;

import com.economy.Main;
import com.economy.config.EconomyConfig;
import com.economy.files.BalanceBlockingFile;
import com.economy.storage.MySQLStorageProvider;
import com.economy.util.FileUtils;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EconomyManager {

    private static final EconomyManager INSTANCE = new EconomyManager();

    private BalanceBlockingFile balanceBlockingFile;
    private MySQLStorageProvider mysqlStorageProvider;
    private boolean useMySQL;
    private boolean isDirty;
    private Thread savingThread;
    private HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
    private final Map<UUID, String> playerNames;

    public static EconomyManager getInstance() {
        return INSTANCE;
    }

    private EconomyManager() {
        this.isDirty = false;
        this.playerNames = new ConcurrentHashMap<>();
        
        EconomyConfig config = Main.CONFIG.get();
        this.useMySQL = config.isEnableMySQL();
        
        if (this.useMySQL) {
            // Initialize MySQL storage
            this.mysqlStorageProvider = new MySQLStorageProvider();
            try {
                this.mysqlStorageProvider.initialize().join();
                
                // Se MySQL está vazio, tenta migrar dados do JSON
                int playerCount = this.mysqlStorageProvider.getPlayerCount();
                logger.at(Level.INFO).log("MySQL initialized with %d players", playerCount);
                if (playerCount == 0) {
                    logger.at(Level.INFO).log("MySQL is empty, attempting to migrate from JSON...");
                    migrateFromJSON();
                    // Recarrega o contador após migração
                    playerCount = this.mysqlStorageProvider.getPlayerCount();
                    logger.at(Level.INFO).log("After migration: %d players in MySQL", playerCount);
                }
                
                // Storage info is in MySQL connection log
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Failed to initialize MySQL storage, falling back to JSON");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
                this.useMySQL = false;
            }
        }
        
        if (!this.useMySQL) {
            // Initialize JSON file storage (default)
            this.balanceBlockingFile = new BalanceBlockingFile();
            FileUtils.ensureMainDirectory();

            try {
                FileUtils.ensureFile(FileUtils.BALANCES_PATH, "{}");
                logger.at(Level.INFO).log("Loading balance data from JSON file...");
                this.balanceBlockingFile.syncLoad();
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("ERROR LOADING BALANCE FILE");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
            }
        }

        startSavingThread();
    }
    
    /**
     * Migra dados do JSON para o MySQL quando MySQL está vazio
     */
    private void migrateFromJSON() {
        try {
            logger.at(Level.INFO).log("Starting migration from JSON to MySQL...");
            BalanceBlockingFile jsonFile = new BalanceBlockingFile();
            FileUtils.ensureMainDirectory();
            FileUtils.ensureFile(FileUtils.BALANCES_PATH, "{}");
            jsonFile.syncLoad();
            
            BalanceTracker jsonTracker = jsonFile.getTracker();
            if (jsonTracker == null) {
                logger.at(Level.INFO).log("JSON tracker is null, no data to migrate");
                return;
            }
            
            com.economy.economy.PlayerBalance[] balances = jsonTracker.getBalances();
            if (balances == null || balances.length == 0) {
                logger.at(Level.INFO).log("No players found in JSON, nothing to migrate");
                return;
            }
            
            logger.at(Level.INFO).log("Found %d players in JSON, starting migration...", balances.length);
            
            int migratedCount = 0;
            int failedCount = 0;
            for (com.economy.economy.PlayerBalance balance : balances) {
                try {
                    if (balance == null || balance.getUuid() == null) {
                        logger.at(Level.WARNING).log("Skipping invalid balance entry");
                        failedCount++;
                        continue;
                    }
                    this.mysqlStorageProvider.savePlayer(balance.getUuid(), balance).join();
                    migratedCount++;
                    logger.at(Level.FINE).log("Migrated player %s (balance: %.2f)", balance.getUuid(), balance.getBalance());
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to migrate player %s: %s", balance != null && balance.getUuid() != null ? balance.getUuid() : "unknown", e.getMessage());
                    failedCount++;
                }
            }
            
            if (migratedCount > 0) {
                logger.at(Level.INFO).log("Migration completed: %d players migrated successfully, %d failed", migratedCount, failedCount);
                // Recarrega os dados do MySQL para garantir sincronização
                try {
                    this.mysqlStorageProvider.reloadAllPlayers();
                    int finalCount = this.mysqlStorageProvider.getPlayerCount();
                    logger.at(Level.INFO).log("Reloaded data from MySQL after migration: %d players", finalCount);
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to reload data after migration: %s", e.getMessage());
                }
            } else if (failedCount > 0) {
                logger.at(Level.WARNING).log("Migration failed: %d players failed to migrate", failedCount);
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Failed to migrate data from JSON to MySQL: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    private void startSavingThread() {
        this.savingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Salva a cada 30 segundos
                    if (isDirty) {
                        save();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        this.savingThread.setDaemon(true);
        this.savingThread.start();
    }

    public void markDirty() {
        this.isDirty = true;
    }

    public void save() {
        try {
            if (this.useMySQL && this.mysqlStorageProvider != null) {
                // Save all dirty balances to MySQL
                Map<UUID, PlayerBalance> dirtyPlayers = new HashMap<>();
                BalanceTracker tracker = this.mysqlStorageProvider.getBalanceTracker();
                if (tracker != null) {
                    for (PlayerBalance balance : tracker.getBalances()) {
                        if (this.isDirty) {
                            dirtyPlayers.put(balance.getUuid(), balance);
                        }
                    }
                    if (!dirtyPlayers.isEmpty()) {
                        this.mysqlStorageProvider.saveAll(dirtyPlayers).join();
                    }
                }
            } else {
                this.balanceBlockingFile.syncSave();
            }
            this.isDirty = false;
            logger.at(Level.FINE).log("Economy data saved");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR SAVING BALANCE DATA");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
        }
    }

    private BalanceTracker getTracker() {
        if (this.useMySQL && this.mysqlStorageProvider != null) {
            return this.mysqlStorageProvider.getBalanceTracker();
        }
        return this.balanceBlockingFile != null ? this.balanceBlockingFile.getTracker() : null;
    }
    
    public double getBalance(UUID uuid) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return 0.0;
        }
        return tracker.getBalance(uuid);
    }
    
    public boolean hasPlayerBalance(UUID uuid) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        // Verifica se o jogador já tem saldo registrado
        for (PlayerBalance balance : tracker.getBalances()) {
            if (balance.getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void setBalance(UUID uuid, double balance) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return;
        }
        tracker.setBalance(uuid, balance);
        // Atualiza o nick se já estiver no playerNames
        if (playerNames.containsKey(uuid)) {
            tracker.setPlayerNick(uuid, playerNames.get(uuid));
        }
        // Save to MySQL immediately if using MySQL
        if (this.useMySQL && this.mysqlStorageProvider != null) {
            int cash = tracker.getCash(uuid);
            PlayerBalance playerBalance = new PlayerBalance(uuid, playerNames.getOrDefault(uuid, ""), balance, cash);
            this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
        }
        markDirty();
    }

    public void addBalance(UUID uuid, double amount) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return;
        }
        tracker.addBalance(uuid, amount);
        // Save to MySQL immediately if using MySQL
        if (this.useMySQL && this.mysqlStorageProvider != null) {
            double newBalance = tracker.getBalance(uuid);
            int cash = tracker.getCash(uuid);
            PlayerBalance playerBalance = new PlayerBalance(uuid, playerNames.getOrDefault(uuid, ""), newBalance, cash);
            this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
        }
        markDirty();
    }

    public boolean subtractBalance(UUID uuid, double amount) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        boolean result = tracker.subtractBalance(uuid, amount);
        if (result) {
            // Save to MySQL immediately if using MySQL
            if (this.useMySQL && this.mysqlStorageProvider != null) {
                double newBalance = tracker.getBalance(uuid);
                int cash = tracker.getCash(uuid);
                PlayerBalance playerBalance = new PlayerBalance(uuid, playerNames.getOrDefault(uuid, ""), newBalance, cash);
                this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
            }
            markDirty();
        }
        return result;
    }

    public boolean hasBalance(UUID uuid, double amount) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        return tracker.hasBalance(uuid, amount);
    }

    public int getCash(UUID uuid) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return 0;
        }
        return tracker.getCash(uuid);
    }

    public void setCash(UUID uuid, int cash) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return;
        }
        tracker.setCash(uuid, cash);
        // Save to MySQL immediately if using MySQL
        if (this.useMySQL && this.mysqlStorageProvider != null) {
            double balance = tracker.getBalance(uuid);
            PlayerBalance playerBalance = new PlayerBalance(uuid, playerNames.getOrDefault(uuid, ""), balance, cash);
            this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
        }
        markDirty();
    }

    public void addCash(UUID uuid, int amount) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return;
        }
        tracker.addCash(uuid, amount);
        // Save to MySQL immediately if using MySQL
        if (this.useMySQL && this.mysqlStorageProvider != null) {
            double balance = tracker.getBalance(uuid);
            int newCash = tracker.getCash(uuid);
            PlayerBalance playerBalance = new PlayerBalance(uuid, playerNames.getOrDefault(uuid, ""), balance, newCash);
            this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
        }
        markDirty();
    }

    public boolean subtractCash(UUID uuid, int amount) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        boolean result = tracker.subtractCash(uuid, amount);
        if (result) {
            // Save to MySQL immediately if using MySQL
            if (this.useMySQL && this.mysqlStorageProvider != null) {
                double balance = tracker.getBalance(uuid);
                int newCash = tracker.getCash(uuid);
                PlayerBalance playerBalance = new PlayerBalance(uuid, playerNames.getOrDefault(uuid, ""), balance, newCash);
                this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
            }
            markDirty();
        }
        return result;
    }

    public boolean hasCash(UUID uuid, int amount) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return false;
        }
        return tracker.hasCash(uuid, amount);
    }

    public void setPlayerName(UUID uuid, String name) {
        playerNames.put(uuid, name);
        BalanceTracker tracker = getTracker();
        if (tracker != null) {
            // Atualiza o nick no PlayerBalance também
            tracker.setPlayerNick(uuid, name);
            // Save to MySQL immediately if using MySQL
            if (this.useMySQL && this.mysqlStorageProvider != null) {
                double balance = tracker.getBalance(uuid);
                int cash = tracker.getCash(uuid);
                PlayerBalance playerBalance = new PlayerBalance(uuid, name, balance, cash);
                this.mysqlStorageProvider.savePlayer(uuid, playerBalance);
            }
        }
        markDirty();
    }

    public String getPlayerName(UUID uuid) {
        // Primeiro tenta buscar no cache de nomes (jogadores online)
        String name = playerNames.get(uuid);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        
        // Se não encontrar, busca no BalanceTracker (JSON ou Database)
        BalanceTracker tracker = getTracker();
        if (tracker != null) {
            String nick = tracker.getPlayerNick(uuid);
            if (nick != null && !nick.isEmpty()) {
                // Atualiza o cache para próximas consultas
                playerNames.put(uuid, nick);
                return nick;
            }
        }
        
        // Se não encontrar em nenhum lugar, retorna "Desconhecido"
        return "Desconhecido";
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return new ArrayList<>();
        }
        Map<UUID, Double> balanceMap = new HashMap<>();
        for (PlayerBalance balance : tracker.getBalances()) {
            balanceMap.put(balance.getUuid(), balance.getBalance());
        }
        
        return balanceMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Obtém o rank de um jogador no ranking de saldos (1-500)
     * @param uuid UUID do jogador
     * @return Rank do jogador (1-500) ou -1 se não estiver no top 500
     */
    public int getPlayerRank(UUID uuid) {
        BalanceTracker tracker = getTracker();
        if (tracker == null) {
            return -1;
        }
        
        // Obtém todos os saldos ordenados
        Map<UUID, Double> balanceMap = new HashMap<>();
        for (PlayerBalance balance : tracker.getBalances()) {
            balanceMap.put(balance.getUuid(), balance.getBalance());
        }
        
        // Ordena por saldo (maior para menor)
        List<Map.Entry<UUID, Double>> sortedBalances = balanceMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        // Procura o jogador na lista
        for (int i = 0; i < sortedBalances.size() && i < 500; i++) {
            if (sortedBalances.get(i).getKey().equals(uuid)) {
                return i + 1; // Rank começa em 1
            }
        }
        
        return -1; // Não está no top 500
    }

    public UUID getPlayerUuidByName(String name) {
        for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public void shutdown() {
        if (this.savingThread != null) {
            this.savingThread.interrupt();
        }
        
        // Save all data before shutdown
        save();
        
        // Shutdown MySQL connection if using MySQL (usando método síncrono para evitar problemas durante reload)
        if (this.useMySQL && this.mysqlStorageProvider != null) {
            try {
                this.mysqlStorageProvider.shutdownSync();
            } catch (Exception e) {
                logger.at(Level.WARNING).log("Error shutting down MySQL: %s", e.getMessage());
            }
        }
    }
}

