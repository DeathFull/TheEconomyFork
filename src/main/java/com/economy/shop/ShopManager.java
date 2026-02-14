package com.economy.shop;

import com.economy.Main;
import com.economy.config.EconomyConfig;
import com.economy.files.ShopBlockingFile;
import com.economy.files.ShopNpcBlockingFile;
import com.economy.storage.MySQLShopStorageProvider;
import com.economy.util.FileUtils;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ShopManager {

    private static final ShopManager INSTANCE = new ShopManager();

    private ShopBlockingFile shopBlockingFile;
    private Map<Integer, ShopNpcBlockingFile> npcShopFiles; // Mapa de shopId -> ShopNpcBlockingFile para lojas de NPCs
    private MySQLShopStorageProvider mysqlShopStorageProvider;
    private ShopTracker tracker; // Mantido para compatibilidade (shopId 0)
    private Map<Integer, ShopTracker> shopTrackers; // Mapa de shopId -> ShopTracker
    private boolean useMySQL;
    private boolean isDirty;
    private Thread savingThread;
    private HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");

    public static ShopManager getInstance() {
        return INSTANCE;
    }

    private ShopManager() {
        this.isDirty = false;
        this.tracker = new ShopTracker();
        this.shopTrackers = new ConcurrentHashMap<>();
        this.shopTrackers.put(0, this.tracker); // shopId 0 é a loja padrão (/shop)
        this.npcShopFiles = new ConcurrentHashMap<>();
        
        EconomyConfig config = Main.CONFIG.get();
        this.useMySQL = config.isEnableMySQL();
        
        if (this.useMySQL) {
            // Initialize MySQL storage
            this.mysqlShopStorageProvider = new MySQLShopStorageProvider();
            try {
                this.mysqlShopStorageProvider.initialize().join();
                // Load data from MySQL into tracker (shopId 0)
                this.mysqlShopStorageProvider.loadShopData(this.tracker, 0).join();
                
                // Se MySQL está vazio, tenta migrar dados do JSON
                if (this.tracker.getAllItems().isEmpty() && this.tracker.getTabs().isEmpty()) {
                    migrateFromJSON();
                }
                
                // Storage info is in MySQL connection log
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Failed to initialize MySQL storage for AdminShop, falling back to JSON");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
                this.useMySQL = false;
            }
        }
        
        if (!this.useMySQL) {
            // Initialize JSON file storage (default)
            this.shopBlockingFile = new ShopBlockingFile();
            FileUtils.ensureMainDirectory();

            try {
                FileUtils.ensureFile(FileUtils.SHOP_PATH, "{\"NextUniqueId\": 1, \"Items\": [], \"Tabs\": []}");
                logger.at(Level.INFO).log("Loading shop data from JSON file...");
                this.shopBlockingFile.syncLoad();
                this.tracker = this.shopBlockingFile.getTracker();
                this.shopTrackers.put(0, this.tracker); // Atualiza referência
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("ERROR LOADING SHOP FILE");
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
            }
            
            // Carrega todas as lojas de NPCs salvos
            loadAllNpcShops();
        }

        startSavingThread();
    }
    
    /**
     * Obtém o ShopTracker para um shopId específico
     * Se não existir, cria um novo
     * @param shopId ID da loja (0 para /shop, 1+ para NPCs)
     * @return ShopTracker da loja
     */
    private ShopTracker getTracker(int shopId) {
        return shopTrackers.computeIfAbsent(shopId, k -> {
            ShopTracker newTracker = new ShopTracker();
            // Se for uma loja de NPC (shopId > 0)
            if (shopId > 0) {
                if (useMySQL) {
                    // Carrega do MySQL se estiver configurado
                    try {
                        mysqlShopStorageProvider.loadShopData(newTracker, shopId).join();
                    } catch (Exception e) {
                        logger.at(Level.WARNING).log("Failed to load shop data for shopId %d: %s", shopId, e.getMessage());
                    }
                } else {
                    // Carrega do JSON se MySQL não estiver configurado
                    try {
                        ShopNpcBlockingFile npcFile = npcShopFiles.computeIfAbsent(shopId, sid -> {
                            ShopNpcBlockingFile file = new ShopNpcBlockingFile(sid);
                            try {
                                String filePath = FileUtils.MAIN_PATH + java.io.File.separator + "shop_npc_" + sid + ".json";
                                FileUtils.ensureFile(filePath, "{\"NextUniqueId\": 1, \"Items\": [], \"Tabs\": []}");
                                file.syncLoad();
                            } catch (Exception e) {
                                logger.at(Level.WARNING).log("Failed to load NPC shop file for shopId %d: %s", sid, e.getMessage());
                            }
                            return file;
                        });
                        newTracker = npcFile.getTracker();
                        // Garante que o tracker tenha um NextUniqueId válido
                        if (newTracker.getNextUniqueId() <= 0) {
                            newTracker.setNextUniqueId(1);
                        }
                    } catch (Exception e) {
                        logger.at(Level.WARNING).log("Failed to load NPC shop data from JSON for shopId %d: %s", shopId, e.getMessage());
                    }
                }
            }
            return newTracker;
        });
    }
    
    /**
     * Migra dados do JSON para o MySQL quando MySQL está vazio
     */
    private void migrateFromJSON() {
        try {
            ShopBlockingFile jsonFile = new ShopBlockingFile();
            FileUtils.ensureMainDirectory();
            FileUtils.ensureFile(FileUtils.SHOP_PATH, "{\"NextUniqueId\": 1, \"Items\": [], \"Tabs\": []}");
            jsonFile.syncLoad();
            
            ShopTracker jsonTracker = jsonFile.getTracker();
            if (jsonTracker == null) {
                return; // Não há dados para migrar
            }
            
            int migratedItems = 0;
            int migratedTabs = 0;
            
            // Migra tabs
            for (String tab : jsonTracker.getTabs()) {
                try {
                    this.mysqlShopStorageProvider.createTab(tab).join();
                    migratedTabs++;
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to migrate tab %s: %s", tab, e.getMessage());
                }
            }
            
            // Migra items
            for (ShopItem item : jsonTracker.getAllItems()) {
                try {
                    this.mysqlShopStorageProvider.addItem(item).join();
                    migratedItems++;
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to migrate item %d: %s", item.getUniqueId(), e.getMessage());
                }
            }
            
            if (migratedItems > 0 || migratedTabs > 0) {
                logger.at(Level.INFO).log("Migrated AdminShop from JSON to MySQL: %d items, %d tabs", migratedItems, migratedTabs);
                // Recarrega os dados do MySQL após migração
                this.mysqlShopStorageProvider.loadShopData(this.tracker).join();
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to migrate AdminShop data from JSON to MySQL: %s", e.getMessage());
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
            this.shopBlockingFile.syncSave();
            this.isDirty = false;
            logger.at(Level.FINE).log("Shop data saved");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR SAVING SHOP FILE");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
        }
    }

    public ShopItem addItem(String itemId, int quantity, double priceSell, double priceBuy) {
        return addItem(itemId, quantity, priceSell, priceBuy, "", 0);
    }

    public ShopItem addItem(String itemId, int quantity, double priceSell, double priceBuy, String tab) {
        return addItem(itemId, quantity, priceSell, priceBuy, tab, 0);
    }
    
    public ShopItem addItem(String itemId, int quantity, double priceSell, double priceBuy, String tab, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        ShopItem item = new ShopItem(0, itemId, quantity, priceSell, priceBuy, tab != null ? tab : "");
        
        if (useMySQL) {
            // Save to MySQL immediately - MySQL will generate UniqueId via AUTO_INCREMENT
            item = mysqlShopStorageProvider.addItem(item, shopId).join();
            // Add to tracker in memory after getting the generated ID
            targetTracker.addItem(item);
        } else {
            // Add to tracker in memory first (tracker will assign unique ID)
            targetTracker.addItem(item);
            if (shopId == 0) {
                markDirty(); // Apenas marca dirty para shopId 0 (compatibilidade)
            } else {
                // Salva imediatamente para lojas de NPCs (shopId > 0)
                saveNpcShop(shopId);
            }
        }
        
        logger.at(Level.FINE).log("Item added to shop %d: %s (Unique ID: %d, Tab: %s)", shopId, item.getItemId(), item.getUniqueId(), tab);
        return item;
    }

    public boolean removeItem(int uniqueId) {
        return removeItem(uniqueId, 0);
    }
    
    public boolean removeItem(int uniqueId, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        boolean result = targetTracker.removeItem(uniqueId);
        if (result) {
            if (useMySQL) {
                mysqlShopStorageProvider.removeItem(uniqueId, shopId).join();
            } else {
                if (shopId == 0) {
                    markDirty(); // Apenas marca dirty para shopId 0 (compatibilidade)
                } else {
                    // Salva imediatamente para lojas de NPCs (shopId > 0)
                    saveNpcShop(shopId);
                }
            }
        }
        return result;
    }

    public ShopItem getItem(int uniqueId) {
        return getItem(uniqueId, 0);
    }
    
    public ShopItem getItem(int uniqueId, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        return targetTracker.getItem(uniqueId);
    }
    
    public boolean updateItem(ShopItem item, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        ShopItem existingItem = targetTracker.getItem(item.getUniqueId());
        if (existingItem == null) {
            return false;
        }
        
        // Atualiza os campos do item existente
        existingItem.setItemId(item.getItemId());
        existingItem.setQuantity(item.getQuantity());
        existingItem.setPriceSell(item.getPriceSell());
        existingItem.setPriceBuy(item.getPriceBuy());
        existingItem.setTab(item.getTab());
        existingItem.setConsoleCommand(item.isConsoleCommand());
        existingItem.setConsoleCommand(item.getConsoleCommand());
        existingItem.setDisplayName(item.getDisplayName());
        existingItem.setUseCash(item.isUseCash());
        
        if (useMySQL) {
            mysqlShopStorageProvider.updateItem(existingItem, shopId).join();
        } else {
            if (shopId == 0) {
                markDirty();
            } else {
                // Salva imediatamente para lojas de NPCs (shopId > 0)
                saveNpcShop(shopId);
            }
        }
        
        logger.at(Level.FINE).log("Item updated in shop %d: %s (Unique ID: %d)", shopId, existingItem.getItemId(), existingItem.getUniqueId());
        return true;
    }

    public List<ShopItem> getAllItems() {
        return getAllItems(0);
    }
    
    public List<ShopItem> getAllItems(int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        return targetTracker.getAllItems();
    }

    public boolean hasItem(int uniqueId) {
        return hasItem(uniqueId, 0);
    }
    
    public boolean hasItem(int uniqueId, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        return targetTracker.hasItem(uniqueId);
    }
    
    /**
     * Recarrega os dados do arquivo/MySQL (útil se houver mudanças externas)
     */
    public void reload() {
        reload(0);
    }
    
    /**
     * Recarrega os dados de uma loja específica
     */
    public void reload(int shopId) {
        if (useMySQL) {
            try {
                ShopTracker targetTracker = new ShopTracker();
                mysqlShopStorageProvider.loadShopData(targetTracker, shopId).join();
                shopTrackers.put(shopId, targetTracker);
                if (shopId == 0) {
                    this.tracker = targetTracker; // Mantém compatibilidade
                }
                logger.at(Level.FINE).log("Shop data reloaded from MySQL (shopId: %d)", shopId);
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("ERROR RELOADING SHOP DATA FROM MYSQL (shopId: %d)", shopId);
                logger.at(Level.SEVERE).log(e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (shopId == 0) {
                try {
                    this.shopBlockingFile.syncLoad();
                    this.tracker = this.shopBlockingFile.getTracker();
                    this.shopTrackers.put(0, this.tracker);
                    logger.at(Level.FINE).log("Shop data reloaded from file");
                } catch (Exception e) {
                    logger.at(Level.SEVERE).log("ERROR RELOADING SHOP FILE");
                    logger.at(Level.SEVERE).log(e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Carrega loja de NPC do JSON
                try {
                    ShopNpcBlockingFile npcFile = npcShopFiles.computeIfAbsent(shopId, sid -> {
                        ShopNpcBlockingFile file = new ShopNpcBlockingFile(sid);
                        try {
                            String filePath = FileUtils.MAIN_PATH + java.io.File.separator + "shop_npc_" + sid + ".json";
                            FileUtils.ensureFile(filePath, "{\"NextUniqueId\": 1, \"Items\": [], \"Tabs\": []}");
                            file.syncLoad();
                        } catch (Exception e) {
                            logger.at(Level.WARNING).log("Failed to load NPC shop file for shopId %d: %s", sid, e.getMessage());
                        }
                        return file;
                    });
                    ShopTracker targetTracker = npcFile.getTracker();
                    shopTrackers.put(shopId, targetTracker);
                    logger.at(Level.FINE).log("Shop data reloaded from JSON (shopId: %d)", shopId);
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to reload NPC shop data from JSON for shopId %d: %s", shopId, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Carrega todas as lojas de NPCs salvos em JSON
     */
    private void loadAllNpcShops() {
        if (useMySQL) {
            return; // Não precisa carregar se estiver usando MySQL
        }
        
        try {
            // Busca todos os arquivos shop_npc_*.json
            java.nio.file.Path mainPath = java.nio.file.Paths.get(FileUtils.MAIN_PATH);
            if (!java.nio.file.Files.exists(mainPath)) {
                return;
            }
            
            java.nio.file.DirectoryStream<java.nio.file.Path> stream = 
                java.nio.file.Files.newDirectoryStream(mainPath, "shop_npc_*.json");
            
            for (java.nio.file.Path path : stream) {
                try {
                    String fileName = path.getFileName().toString();
                    // Extrai o shopId do nome do arquivo (shop_npc_1.json -> 1)
                    String shopIdStr = fileName.replace("shop_npc_", "").replace(".json", "");
                    int shopId = Integer.parseInt(shopIdStr);
                    
                    if (shopId > 0) {
                        // Carrega a loja usando getTracker (que já faz o carregamento)
                        getTracker(shopId);
                        logger.at(Level.FINE).log("Loaded NPC shop from JSON (shopId: %d)", shopId);
                    }
                } catch (NumberFormatException e) {
                    // Ignora arquivos com nome inválido
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to load NPC shop from file %s: %s", path, e.getMessage());
                }
            }
            stream.close();
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to load NPC shops from JSON: %s", e.getMessage());
        }
    }
    
    /**
     * Salva uma loja de NPC (shopId > 0) em JSON
     */
    private void saveNpcShop(int shopId) {
        if (shopId <= 0 || useMySQL) {
            return; // Apenas salva lojas de NPCs em JSON quando MySQL não está configurado
        }
        
        try {
            ShopTracker targetTracker = shopTrackers.get(shopId);
            if (targetTracker == null) {
                return;
            }
            
            ShopNpcBlockingFile npcFile = npcShopFiles.computeIfAbsent(shopId, sid -> {
                ShopNpcBlockingFile file = new ShopNpcBlockingFile(sid);
                try {
                    String filePath = FileUtils.MAIN_PATH + java.io.File.separator + "shop_npc_" + sid + ".json";
                    FileUtils.ensureFile(filePath, "{\"NextUniqueId\": 1, \"Items\": [], \"Tabs\": []}");
                } catch (Exception e) {
                    logger.at(Level.WARNING).log("Failed to create NPC shop file for shopId %d: %s", sid, e.getMessage());
                }
                return file;
            });
            
            // Atualiza o tracker do arquivo com os dados atuais
            // Como BlockingDiskFile não permite atualizar o tracker diretamente, precisamos recriar o arquivo
            // Vamos usar reflexão ou criar um método para atualizar o tracker
            // Por enquanto, vamos usar uma abordagem mais simples: atualizar o arquivo diretamente
            java.lang.reflect.Field trackerField = ShopNpcBlockingFile.class.getDeclaredField("tracker");
            trackerField.setAccessible(true);
            trackerField.set(npcFile, targetTracker);
            
            npcFile.syncSave();
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to save NPC shop data for shopId %d: %s", shopId, e.getMessage());
        }
    }

    public void createTab(String tabName) {
        createTab(tabName, 0);
    }
    
    public void createTab(String tabName, int shopId) {
        if (tabName != null && !tabName.isEmpty()) {
            ShopTracker targetTracker = getTracker(shopId);
            // Limita a 7 tabs
            if (targetTracker.getTabs().size() >= 7) {
                throw new IllegalStateException("Maximum of 7 tabs allowed");
            }
            
            targetTracker.addTab(tabName);
            
            if (useMySQL) {
                mysqlShopStorageProvider.createTab(tabName, shopId).join();
            } else {
                if (shopId == 0) {
                    markDirty(); // Apenas marca dirty para shopId 0 (compatibilidade)
                } else {
                    // Salva imediatamente para lojas de NPCs (shopId > 0)
                    saveNpcShop(shopId);
                }
            }
            
            logger.at(Level.FINE).log("Tab created: %s (shopId: %d)", tabName, shopId);
        }
    }

    public boolean removeTab(String tabName) {
        return removeTab(tabName, 0);
    }
    
    public boolean removeTab(String tabName, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        boolean result = targetTracker.removeTab(tabName);
        if (result) {
            if (useMySQL) {
                mysqlShopStorageProvider.removeTab(tabName, shopId).join();
            } else {
                if (shopId == 0) {
                    markDirty(); // Apenas marca dirty para shopId 0 (compatibilidade)
                } else {
                    // Salva imediatamente para lojas de NPCs (shopId > 0)
                    saveNpcShop(shopId);
                }
            }
            logger.at(Level.FINE).log("Tab removed: %s (shopId: %d)", tabName, shopId);
        }
        return result;
    }

    public boolean hasTab(String tabName) {
        return hasTab(tabName, 0);
    }
    
    public boolean hasTab(String tabName, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        return targetTracker.hasTab(tabName);
    }

    public java.util.List<String> getAllTabs() {
        return getAllTabs(0);
    }
    
    public java.util.List<String> getAllTabs(int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        return new java.util.ArrayList<>(targetTracker.getTabs());
    }

    public List<ShopItem> getItemsByTab(String tabName) {
        return getItemsByTab(tabName, 0);
    }
    
    public List<ShopItem> getItemsByTab(String tabName, int shopId) {
        ShopTracker targetTracker = getTracker(shopId);
        return targetTracker.getItemsByTab(tabName);
    }
    
    /**
     * Remove todas as tabs e itens de uma loja específica (usado quando um NPC é removido)
     */
    public void clearShop(int shopId) {
        if (shopId <= 0) {
            logger.at(Level.WARNING).log("Cannot clear shop with shopId <= 0 (shopId: %d)", shopId);
            return;
        }
        
        ShopTracker targetTracker = getTracker(shopId);
        
        // Remove todos os itens
        List<ShopItem> allItems = new java.util.ArrayList<>(targetTracker.getAllItems());
        for (ShopItem item : allItems) {
            if (useMySQL) {
                mysqlShopStorageProvider.removeItem(item.getUniqueId(), shopId).join();
            }
            targetTracker.removeItem(item.getUniqueId());
        }
        
        // Remove todas as tabs
        List<String> allTabs = new java.util.ArrayList<>(targetTracker.getTabs());
        for (String tabName : allTabs) {
            if (useMySQL) {
                mysqlShopStorageProvider.removeTab(tabName, shopId).join();
            }
            targetTracker.removeTab(tabName);
        }
        
        // Remove o tracker do mapa (opcional, mas ajuda a limpar memória)
        shopTrackers.remove(shopId);
        
        // Remove o arquivo JSON se não estiver usando MySQL
        if (!useMySQL) {
            try {
                String filePath = FileUtils.MAIN_PATH + java.io.File.separator + "shop_npc_" + shopId + ".json";
                java.io.File npcShopFile = new java.io.File(filePath);
                if (npcShopFile.exists()) {
                    npcShopFile.delete();
                }
                npcShopFiles.remove(shopId);
            } catch (Exception e) {
                logger.at(Level.WARNING).log("Failed to delete NPC shop file for shopId %d: %s", shopId, e.getMessage());
            }
        }
        
        logger.at(Level.INFO).log("Cleared shop %d: removed %d items and %d tabs", shopId, allItems.size(), allTabs.size());
    }
    
    /**
     * Shutdown and save all data
     */
    public void shutdown() {
        if (useMySQL && mysqlShopStorageProvider != null) {
            mysqlShopStorageProvider.shutdown().join();
        } else if (!useMySQL) {
            save(); // Salva loja admin (shopId 0)
            // Salva todas as lojas de NPCs
            for (Integer shopId : shopTrackers.keySet()) {
                if (shopId > 0) {
                    saveNpcShop(shopId);
                }
            }
        }
    }
}

