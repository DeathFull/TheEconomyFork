package com.economy.playershop;

import com.economy.Main;
import com.economy.config.EconomyConfig;
import com.economy.files.PlayerShopBlockingFile;
import com.economy.storage.MySQLPlayerShopStorageProvider;
import com.economy.util.FileUtils;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerShopManager {

    private static final PlayerShopManager INSTANCE = new PlayerShopManager();

    private PlayerShopBlockingFile playerShopBlockingFile;
    private MySQLPlayerShopStorageProvider mysqlPlayerShopStorageProvider;
    private PlayerShopTracker tracker;
    private boolean useMySQL;
    private boolean isDirty;
    private Thread savingThread;
    private HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");

    // Mapa para rastrear quais jogadores têm suas lojas abertas (em memória, sincronizado com arquivo)
    private java.util.Map<UUID, Boolean> shopOpenStatus = new java.util.concurrent.ConcurrentHashMap<>();

    public static PlayerShopManager getInstance() {
        return INSTANCE;
    }

    private PlayerShopManager() {
        this.isDirty = false;
        this.tracker = new PlayerShopTracker();
        
        EconomyConfig config = Main.CONFIG.get();
        this.useMySQL = config.isEnableMySQL();
        
        if (this.useMySQL) {
            // Initialize MySQL storage
            this.mysqlPlayerShopStorageProvider = new MySQLPlayerShopStorageProvider();
            try {
                this.mysqlPlayerShopStorageProvider.initialize().join();
                // Load data from MySQL into tracker
                this.mysqlPlayerShopStorageProvider.loadShopData(this.tracker).join();
                
                // Load shop open status from tracker
                for (PlayerShopPlayer player : this.tracker.getAllPlayers()) {
                    if (player.getUuid() != null) {
                        boolean isOpen = this.tracker.isShopOpen(player.getUuid());
                        this.shopOpenStatus.put(player.getUuid(), isOpen);
                    }
                }
                
                // Se MySQL está vazio, tenta migrar dados do JSON
                if (this.tracker.getAllPlayers().isEmpty()) {
                    migrateFromJSON();
                }
                
                // Storage info is in MySQL connection log
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Failed to initialize MySQL storage for PlayerShop, falling back to JSON");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
                this.useMySQL = false;
            }
        }
        
        if (!this.useMySQL) {
            // Initialize JSON file storage (default)
            this.playerShopBlockingFile = new PlayerShopBlockingFile();
            FileUtils.ensureMainDirectory();

            try {
                FileUtils.ensureFile(FileUtils.PLAYER_SHOP_PATH, "{\"NextUniqueId\": 1, \"Shops\": []}");
                logger.at(Level.INFO).log("Loading player shop data from JSON file...");
                this.playerShopBlockingFile.syncLoad();
                this.tracker = this.playerShopBlockingFile.getTracker();
                
                // Carrega o status de abertura das lojas do arquivo para a memória
                for (PlayerShopPlayer player : this.tracker.getAllPlayers()) {
                    if (player.getUuid() != null) {
                        boolean isOpen = this.tracker.isShopOpen(player.getUuid());
                        this.shopOpenStatus.put(player.getUuid(), isOpen);
                    }
                }
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("ERROR LOADING PLAYER SHOP FILE");
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
            PlayerShopBlockingFile jsonFile = new PlayerShopBlockingFile();
            FileUtils.ensureMainDirectory();
            FileUtils.ensureFile(FileUtils.PLAYER_SHOP_PATH, "{\"NextUniqueId\": 1, \"Shops\": []}");
            jsonFile.syncLoad();
            
            PlayerShopTracker jsonTracker = jsonFile.getTracker();
            if (jsonTracker == null || jsonTracker.getAllPlayers().isEmpty()) {
                return; // Não há dados para migrar
            }
            
            int migratedPlayers = 0;
            int migratedItems = 0;
            
            // Migra players e seus items
            for (PlayerShopPlayer player : jsonTracker.getAllPlayers()) {
                try {
                    // Obtém informações do tracker
                    boolean isOpen = jsonTracker.isShopOpen(player.getUuid());
                    List<String> tabs = jsonTracker.getTabs(player.getUuid());
                    
                    // Cria o player no MySQL
                    this.mysqlPlayerShopStorageProvider.savePlayerInfo(
                        player.getUuid(),
                        player,
                        isOpen,
                        tabs
                    ).join();
                    migratedPlayers++;
                    
                    // Migra items do player
                    for (PlayerShopItem item : jsonTracker.getItemsByOwner(player.getUuid())) {
                        try {
                            this.mysqlPlayerShopStorageProvider.addItem(item).join();
                            migratedItems++;
                        } catch (Exception e) {
                            logger.at(Level.WARNING).log("Failed to migrate item %d for player %s: %s", 
                                item.getUniqueId(), player.getUuid(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to migrate player %s: %s", player.getUuid(), e.getMessage());
                }
            }
            
            if (migratedPlayers > 0 || migratedItems > 0) {
                logger.at(Level.INFO).log("Migrated PlayerShop from JSON to MySQL: %d players, %d items", 
                    migratedPlayers, migratedItems);
                // Recarrega os dados do MySQL após migração
                this.mysqlPlayerShopStorageProvider.loadShopData(this.tracker).join();
                // Atualiza o status das lojas
                for (PlayerShopPlayer player : this.tracker.getAllPlayers()) {
                    if (player.getUuid() != null) {
                        boolean isOpen = this.tracker.isShopOpen(player.getUuid());
                        this.shopOpenStatus.put(player.getUuid(), isOpen);
                    }
                }
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to migrate PlayerShop data from JSON to MySQL: %s", e.getMessage());
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
        if (useMySQL) {
            // MySQL operations are saved immediately, no need for periodic save
            this.isDirty = false;
            return;
        }
        
        try {
            this.playerShopBlockingFile.syncSave();
            this.isDirty = false;
            logger.at(Level.FINE).log("Player shop data saved");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR SAVING PLAYER SHOP FILE");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerShopItem addItem(String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, int stock) {
        return addItem(itemId, quantity, priceBuy, priceSell, ownerUuid, durability, stock, "");
    }
    
    public PlayerShopItem addItem(String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, int stock, String tab) {
        return addItem(itemId, quantity, priceBuy, priceSell, ownerUuid, durability, 0.0, stock, tab);
    }
    
    public PlayerShopItem addItem(String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, double maxDurability, int stock, String tab) {
        PlayerShopItem item = new PlayerShopItem(0, itemId, quantity, priceBuy, priceSell, ownerUuid, durability, maxDurability, stock, tab);
        
        if (useMySQL) {
            // Save to MySQL immediately - MySQL will generate UniqueId via AUTO_INCREMENT
            item = mysqlPlayerShopStorageProvider.addItem(item).join();
            // Add to tracker in memory after getting the generated ID
            // IMPORTANTE: Não usa tracker.addItem() porque isso incrementa nextUniqueId
            // Em vez disso, adiciona diretamente à lista e sincroniza o nextUniqueId
            tracker.getItems().add(item);
            // Sincroniza o nextUniqueId com o ID gerado pelo MySQL + 1
            if (item.getUniqueId() >= tracker.getNextUniqueId()) {
                tracker.setNextUniqueId(item.getUniqueId() + 1);
            }
        } else {
            // Add to tracker in memory first (tracker will assign unique ID)
            tracker.addItem(item);
            markDirty();
        }
        
        logger.at(Level.FINE).log("Item added to player shop: %s (Unique ID: %d, Owner: %s, Tab: %s)", item.getItemId(), item.getUniqueId(), ownerUuid, tab != null ? tab : "");
        return item;
    }

    public boolean removeItem(int uniqueId) {
        // Para MySQL, verifica se o item existe antes de remover da memória
        if (useMySQL) {
            PlayerShopItem itemToRemove = tracker.getItem(uniqueId);
            if (itemToRemove == null) {
                logger.at(Level.WARNING).log("Attempted to remove item %d that does not exist in memory", uniqueId);
                // Tenta remover do banco mesmo assim (pode estar desincronizado)
                try {
                    mysqlPlayerShopStorageProvider.removeItem(uniqueId).join();
                } catch (Exception e) {
                    // Ignora erros se o item não existe
                }
                return false;
            }
        }
        
        boolean result = tracker.removeItem(uniqueId);
        if (result) {
            if (useMySQL) {
                try {
                    mysqlPlayerShopStorageProvider.removeItem(uniqueId).join();
                } catch (Exception e) {
                    logger.at(Level.SEVERE).log("Exception while removing item %d from MySQL: %s", uniqueId, e.getMessage());
                    e.printStackTrace();
                }
            } else {
                markDirty();
                // Para JSON, salva imediatamente após remover item crítico
                save();
            }
        }
        return result;
    }

    public PlayerShopItem getItem(int uniqueId) {
        return tracker.getItem(uniqueId);
    }

    public List<PlayerShopItem> getAllItems() {
        return tracker.getAllItems();
    }

    public List<PlayerShopItem> getItemsByOwner(UUID ownerUuid) {
        return tracker.getItemsByOwner(ownerUuid);
    }

    public List<PlayerShopItem> getOpenShopItems() {
        // Retorna apenas itens de lojas abertas e com estoque > 0
        List<PlayerShopItem> allItems = getAllItems();
        List<PlayerShopItem> openItems = new java.util.ArrayList<>();
        for (PlayerShopItem item : allItems) {
            if (item.getOwnerUuid() != null && isShopOpen(item.getOwnerUuid()) && item.getStock() > 0) {
                openItems.add(item);
            }
        }
        return openItems;
    }

    public List<UUID> getOpenShopOwners() {
        // Retorna lista de UUIDs de jogadores com lojas abertas e com pelo menos um item (mesmo com estoque 0)
        List<UUID> openOwners = new java.util.ArrayList<>();
        for (PlayerShopPlayer player : tracker.getAllPlayers()) {
            UUID ownerUuid = player.getUuid();
            if (ownerUuid != null && isShopOpen(ownerUuid)) {
                // Verifica se o jogador tem pelo menos um item (não importa o estoque)
                List<PlayerShopItem> items = getItemsByOwner(ownerUuid);
                if (items != null && !items.isEmpty()) {
                    openOwners.add(ownerUuid);
                }
            }
        }
        return openOwners;
    }

    public boolean isShopOpen(UUID ownerUuid) {
        // Primeiro verifica em memória, depois no tracker
        if (shopOpenStatus.containsKey(ownerUuid)) {
            return shopOpenStatus.get(ownerUuid);
        }
        // Se não estiver em memória, verifica no tracker
        boolean isOpen = tracker.isShopOpen(ownerUuid);
        shopOpenStatus.put(ownerUuid, isOpen);
        return isOpen;
    }

    public void setShopOpen(UUID ownerUuid, boolean open) {
        shopOpenStatus.put(ownerUuid, open);
        tracker.setShopOpen(ownerUuid, open);
        
        if (useMySQL) {
            // Save player info with updated isOpen status
            PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
            if (player != null) {
                List<String> tabs = tracker.getTabs(ownerUuid);
                mysqlPlayerShopStorageProvider.savePlayerInfo(ownerUuid, player, open, tabs).join();
            }
        } else {
            markDirty();
        }
    }

    public boolean hasItem(int uniqueId) {
        return tracker.hasItem(uniqueId);
    }

    public void decreaseStock(int uniqueId, int amount) {
        PlayerShopItem item = getItem(uniqueId);
        if (item != null) {
            int newStock = Math.max(0, item.getStock() - amount);
            item.setStock(newStock);
            
            if (useMySQL) {
                mysqlPlayerShopStorageProvider.updateItem(item).join();
            } else {
                markDirty();
            }
            // Não remove o item quando estoque chega a 0 - pode ser reabastecido depois
        }
    }
    
    /**
     * Atualiza os preços de um item da loja do jogador
     */
    public boolean updateItemPrice(int uniqueId, double priceBuy, double priceSell) {
        PlayerShopItem item = getItem(uniqueId);
        if (item == null) {
            return false;
        }
        
        item.setPriceBuy(priceBuy);
        item.setPriceSell(priceSell);
        
        if (useMySQL) {
            mysqlPlayerShopStorageProvider.updateItem(item).join();
        } else {
            markDirty();
        }
        
        return true;
    }
    
    /**
     * Verifica se já existe um item com o mesmo itemId e durabilidade do mesmo dono
     * Se existir, aumenta o estoque e atualiza o preço se diferente
     */
    public PlayerShopItem addOrUpdateItem(String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, int stock) {
        return addOrUpdateItem(itemId, quantity, priceBuy, priceSell, ownerUuid, durability, stock, "");
    }
    
    /**
     * Verifica se já existe um item com o mesmo itemId e durabilidade do mesmo dono
     * Se existir, aumenta o estoque e atualiza o preço se diferente
     */
    public PlayerShopItem addOrUpdateItem(String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, int stock, String tab) {
        return addOrUpdateItem(itemId, quantity, priceBuy, priceSell, ownerUuid, durability, 0.0, stock, tab);
    }
    
    /**
     * Verifica se já existe um item com o mesmo itemId e durabilidade do mesmo dono
     * Se existir, aumenta o estoque e atualiza o preço se diferente
     */
    public PlayerShopItem addOrUpdateItem(String itemId, int quantity, double priceBuy, double priceSell, UUID ownerUuid, double durability, double maxDurability, int stock, String tab) {
        // Procura item existente do mesmo dono com mesmo itemId, durabilidade e tab
        List<PlayerShopItem> ownerItems = getItemsByOwner(ownerUuid);
        String tabToMatch = tab != null ? tab : "";
        for (PlayerShopItem existingItem : ownerItems) {
            String existingTab = existingItem.getTab() != null ? existingItem.getTab() : "";
            if (existingItem.getItemId().equals(itemId) && 
                Math.abs(existingItem.getDurability() - durability) < 0.01 && // Compara durabilidade com tolerância
                existingTab.equals(tabToMatch)) { // Compara tab
                // Item já existe - aumenta o estoque
                existingItem.setStock(existingItem.getStock() + stock);
                // Atualiza os preços se forem diferentes
                if (Math.abs(existingItem.getPriceBuy() - priceBuy) > 0.01) {
                    existingItem.setPriceBuy(priceBuy);
                }
                if (Math.abs(existingItem.getPriceSell() - priceSell) > 0.01) {
                    existingItem.setPriceSell(priceSell);
                }
                
                if (useMySQL) {
                    mysqlPlayerShopStorageProvider.updateItem(existingItem).join();
                } else {
                    markDirty();
                }
                return existingItem;
            }
        }
        
        // Item não existe - cria novo
        PlayerShopItem newItem = addItem(itemId, quantity, priceBuy, priceSell, ownerUuid, durability, maxDurability, stock, tab);
        return newItem;
    }
    
    /**
     * Atualiza ou adiciona informações do jogador (UUID e nick)
     * Preserva customName e shopIcon se já existirem
     */
    public void updatePlayerInfo(UUID uuid, String nick) {
        if (uuid != null) {
            PlayerShopPlayer player = tracker.getPlayer(uuid);
            if (player == null) {
                // Se não existe, cria um novo
                tracker.addOrUpdatePlayer(uuid, nick);
                player = tracker.getPlayer(uuid);
            } else {
                // Se existe, atualiza apenas o nick (preserva customName e shopIcon)
                player.setNick(nick != null ? nick : "");
            }
            
            if (useMySQL && player != null) {
                // Save player info to MySQL
                boolean isOpen = tracker.isShopOpen(uuid);
                List<String> tabs = tracker.getTabs(uuid);
                mysqlPlayerShopStorageProvider.savePlayerInfo(uuid, player, isOpen, tabs).join();
            } else {
                markDirty();
            }
        }
    }
    
    /**
     * Obtém o nick do jogador do arquivo
     */
    public String getPlayerNick(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        PlayerShopPlayer player = tracker.getPlayer(uuid);
        if (player != null) {
            return player.getNick();
        }
        return null;
    }
    
    /**
     * Renomeia a loja do jogador
     */
    public boolean renameShop(UUID ownerUuid, String customName) {
        if (ownerUuid == null) {
            return false;
        }
        
        PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
        if (player == null) {
            // Se o jogador não existe, cria um novo e adiciona ao tracker
            player = new PlayerShopPlayer(ownerUuid, "");
            // Usa put diretamente para evitar sobrescrever com addOrUpdatePlayer
            tracker.getPlayers().put(ownerUuid, player);
        }
        
        // Atualiza o nome personalizado (preserva outros dados como shopIcon)
        player.setCustomName(customName != null ? customName : "");
        
        if (useMySQL) {
            // Save player info to MySQL
            boolean isOpen = tracker.isShopOpen(ownerUuid);
            List<String> tabs = tracker.getTabs(ownerUuid);
            mysqlPlayerShopStorageProvider.savePlayerInfo(ownerUuid, player, isOpen, tabs).join();
        } else {
            markDirty();
        }
        return true;
    }
    
    /**
     * Obtém o nome personalizado da loja, ou null se não tiver
     */
    public String getShopCustomName(UUID ownerUuid) {
        if (ownerUuid == null) {
            return null;
        }
        
        PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
        if (player != null) {
            String customName = player.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                return customName;
            }
        }
        return null;
    }
    
    /**
     * Define o ícone da loja do jogador
     */
    public boolean setShopIcon(UUID ownerUuid, String itemId) {
        if (ownerUuid == null) {
            return false;
        }
        
        PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
        if (player == null) {
            // Cria o jogador se não existir
            player = new PlayerShopPlayer(ownerUuid, "");
            tracker.addOrUpdatePlayer(ownerUuid, "");
        }
        
        player.setShopIcon(itemId != null ? itemId : "");
        
        if (useMySQL) {
            // Save player info to MySQL
            boolean isOpen = tracker.isShopOpen(ownerUuid);
            List<String> tabs = tracker.getTabs(ownerUuid);
            mysqlPlayerShopStorageProvider.savePlayerInfo(ownerUuid, player, isOpen, tabs).join();
        } else {
            markDirty();
        }
        return true;
    }
    
    /**
     * Obtém o ícone da loja do jogador
     */
    public String getShopIcon(UUID ownerUuid) {
        if (ownerUuid == null) {
            return null;
        }
        
        PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
        if (player != null) {
            String icon = player.getShopIcon();
            return (icon != null && !icon.isEmpty()) ? icon : null;
        }
        return null;
    }
    
    /**
     * Recarrega os dados do arquivo (útil se houver mudanças externas)
     */
    public void reload() {
        if (useMySQL) {
            try {
                tracker = new PlayerShopTracker();
                mysqlPlayerShopStorageProvider.loadShopData(tracker).join();
                
                // Reload shop open status
                shopOpenStatus.clear();
                for (PlayerShopPlayer player : tracker.getAllPlayers()) {
                    if (player.getUuid() != null) {
                        boolean isOpen = tracker.isShopOpen(player.getUuid());
                        shopOpenStatus.put(player.getUuid(), isOpen);
                    }
                }
                
                logger.at(Level.FINE).log("Player shop data reloaded from MySQL");
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("ERROR RELOADING PLAYER SHOP DATA FROM MYSQL");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
            }
        } else {
            try {
                this.playerShopBlockingFile.syncLoad();
                this.tracker = this.playerShopBlockingFile.getTracker();
                
                // Reload shop open status
                shopOpenStatus.clear();
                for (PlayerShopPlayer player : tracker.getAllPlayers()) {
                    if (player.getUuid() != null) {
                        boolean isOpen = tracker.isShopOpen(player.getUuid());
                        shopOpenStatus.put(player.getUuid(), isOpen);
                    }
                }
                
                logger.at(Level.FINE).log("Player shop data reloaded from file");
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("ERROR RELOADING PLAYER SHOP FILE");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Cria uma nova tab para a loja do jogador
     */
    public void createTab(UUID ownerUuid, String tabName) {
        if (tabName != null && !tabName.isEmpty() && ownerUuid != null) {
            // Limita a 7 tabs
            List<String> tabs = tracker.getTabs(ownerUuid);
            if (tabs.size() >= 7) {
                throw new IllegalStateException("Maximum of 7 tabs allowed");
            }
            
            tracker.addTab(ownerUuid, tabName);
            
            if (useMySQL) {
                mysqlPlayerShopStorageProvider.createTab(ownerUuid, tabName).join();
                // Also update player info to save tabs
                PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
                if (player != null) {
                    boolean isOpen = tracker.isShopOpen(ownerUuid);
                    mysqlPlayerShopStorageProvider.savePlayerInfo(ownerUuid, player, isOpen, tracker.getTabs(ownerUuid)).join();
                }
            } else {
                markDirty();
            }
            
            logger.at(Level.FINE).log("Tab created for player %s: %s", ownerUuid, tabName);
        }
    }
    
    /**
     * Remove uma tab da loja do jogador
     */
    public boolean removeTab(UUID ownerUuid, String tabName) {
        boolean result = tracker.removeTab(ownerUuid, tabName);
        if (result) {
            if (useMySQL) {
                mysqlPlayerShopStorageProvider.removeTab(ownerUuid, tabName).join();
                // Also update player info to save tabs
                PlayerShopPlayer player = tracker.getPlayer(ownerUuid);
                if (player != null) {
                    boolean isOpen = tracker.isShopOpen(ownerUuid);
                    mysqlPlayerShopStorageProvider.savePlayerInfo(ownerUuid, player, isOpen, tracker.getTabs(ownerUuid)).join();
                }
            } else {
                markDirty();
            }
            logger.at(Level.FINE).log("Tab removed for player %s: %s", ownerUuid, tabName);
        }
        return result;
    }
    
    /**
     * Verifica se a loja do jogador tem uma tab específica
     */
    public boolean hasTab(UUID ownerUuid, String tabName) {
        return tracker.hasTab(ownerUuid, tabName);
    }
    
    /**
     * Obtém todas as tabs da loja do jogador
     */
    public List<String> getAllTabs(UUID ownerUuid) {
        return new java.util.ArrayList<>(tracker.getTabs(ownerUuid));
    }
    
    /**
     * Obtém itens da loja do jogador filtrados por tab
     */
    public List<PlayerShopItem> getItemsByTab(UUID ownerUuid, String tabName) {
        return tracker.getItemsByTab(ownerUuid, tabName);
    }
    
    /**
     * Shutdown and save all data
     */
    public void shutdown() {
        if (useMySQL && mysqlPlayerShopStorageProvider != null) {
            // Save all player info before shutdown (usando método síncrono para evitar problemas durante reload)
            for (PlayerShopPlayer player : tracker.getAllPlayers()) {
                if (player.getUuid() != null) {
                    try {
                        boolean isOpen = tracker.isShopOpen(player.getUuid());
                        List<String> tabs = tracker.getTabs(player.getUuid());
                        // Usa método síncrono durante shutdown para evitar RejectedExecutionException
                        mysqlPlayerShopStorageProvider.savePlayerInfoSync(player.getUuid(), player, isOpen, tabs);
                    } catch (Exception e) {
                        logger.at(Level.WARNING).log("Failed to save player shop info for %s during shutdown: %s", 
                            player.getUuid(), e.getMessage());
                    }
                }
            }
            // Shutdown de forma síncrona (após salvar todos os dados)
            try {
                mysqlPlayerShopStorageProvider.shutdownSync();
            } catch (Exception e) {
                logger.at(Level.WARNING).log("Error shutting down MySQL PlayerShop storage: %s", e.getMessage());
            }
        } else if (!useMySQL) {
            save();
        }
    }
}

