package com.economy.config;

import com.economy.files.HudConfigBlockingFile;
import com.economy.util.FileUtils;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

public class HudConfigManager {

    private static HudConfigManager INSTANCE;
    private static volatile boolean isShuttingDown = false; // Flag para indicar que está desligando
    private HudConfigBlockingFile hudConfigFile;
    private HudConfig config;
    private boolean isDirty = false;
    private Thread savingThread;
    private HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");
    private long lastSaveTime = 0; // Timestamp da última vez que salvou

    public static HudConfigManager getInstance() {
        if (INSTANCE == null) {
            synchronized (HudConfigManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HudConfigManager();
                }
            }
        }
        return INSTANCE;
    }

    private HudConfigManager() {
        this.hudConfigFile = new HudConfigBlockingFile();
        FileUtils.ensureMainDirectory();

        try {
            // Verifica se o arquivo já existe ANTES de tentar carregar
            java.io.File configFile = new java.io.File(FileUtils.MAIN_PATH + java.io.File.separator + "HudConfig.json");
            boolean fileExists = configFile.exists() && configFile.length() > 2; // Mais que apenas "{}"
            
            if (fileExists) {
                // Arquivo existe - apenas lê
                logger.at(Level.INFO).log("Loading HUD config from JSON file...");
                this.hudConfigFile.syncLoad();
                this.config = this.hudConfigFile.getConfig();
                if (this.config == null) {
                    this.config = new HudConfig();
                }
            } else {
                // Arquivo não existe - cria configuração padrão
                logger.at(Level.INFO).log("HUD config file not found, creating default configuration...");
                this.config = new HudConfig();
                // Cria o arquivo com valores padrão
                this.hudConfigFile.setConfig(this.config);
                this.hudConfigFile.syncSave();
                logger.at(Level.INFO).log("Default HUD config file created");
            }
            
            // Se o arquivo já existe e tem conteúdo válido, verifica se precisa adicionar campos novos
            if (fileExists) {
                // Verifica se tem campos válidos - se tiver, preserva tudo
                if (this.config.getFields() != null && !this.config.getFields().isEmpty()) {
                    // Arquivo existe e tem campos válidos - verifica se precisa adicionar campos novos (TopPosition, RightPosition, LeftPosition)
                    boolean needsUpdate = false;
                    if (this.config.getTopPosition() <= 0) {
                        this.config.setTopPosition(450);
                        needsUpdate = true;
                    }
                    if (this.config.getRightPosition() <= 0 && this.config.getLeftPosition() <= 0) {
                        this.config.setRightPosition(20);
                        needsUpdate = true;
                    }
                    if (this.config.getLeftPosition() < 0) {
                        this.config.setLeftPosition(0);
                        needsUpdate = true;
                    }
                    // Se adicionou campos novos, salva apenas uma vez para migração
                    if (needsUpdate) {
                        this.hudConfigFile.setConfig(this.config);
                        this.hudConfigFile.syncSave();
                        logger.at(Level.INFO).log("HUD config migrated with new fields");
                    }
                    return;
                }
            }
            
            // Só chega aqui se o arquivo não existir ou estiver vazio/inválido
            // Nesse caso, cria configuração padrão
            boolean needsSave = false;
            if (this.config.getFields().isEmpty()) {
                this.config.createDefaultFields();
                needsSave = true;
            }
            
            if (this.config.getHeight() == 0) {
                int visibleFieldsCount = this.config.getVisibleFieldsCount();
                int calculatedHeight = Math.max(85, visibleFieldsCount * 20 + 20);
                this.config.setHeight(calculatedHeight);
                needsSave = true;
            }
            
            if (this.config.getWidth() == 0) {
                this.config.setWidth(150);
                needsSave = true;
            }
            
            // Se o topPosition não foi definido (0 ou negativo), define valor padrão
            if (this.config.getTopPosition() <= 0) {
                this.config.setTopPosition(450);
                needsSave = true;
            }
            
            // Se o rightPosition não foi definido (0 ou negativo), define valor padrão
            if (this.config.getRightPosition() <= 0 && this.config.getLeftPosition() <= 0) {
                this.config.setRightPosition(20);
                needsSave = true;
            }
            
            // Se o leftPosition não foi definido, define valor padrão (0)
            if (this.config.getLeftPosition() < 0) {
                this.config.setLeftPosition(0);
                needsSave = true;
            }
            
            // Só salva se precisar criar valores padrão (arquivo não existe ou está vazio)
            if (needsSave) {
                this.hudConfigFile.setConfig(this.config);
                save();
            }
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR LOADING HUD CONFIG FILE");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
            // Em caso de erro, cria configuração padrão
            this.config = new HudConfig();
        }

        // Thread de salvamento periódico (opcional, apenas para mudanças explícitas)
        startSavingThread();
    }

    private void startSavingThread() {
        // Thread de salvamento periódico apenas para mudanças explícitas via markDirtyAndSave()
        // NÃO salva automaticamente - apenas quando explicitamente solicitado
        this.savingThread = new Thread(() -> {
            while (!isShuttingDown) {
                try {
                    Thread.sleep(30000); // Verifica a cada 30 segundos
                    // NÃO salva automaticamente - apenas quando explicitamente solicitado
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    logger.at(Level.SEVERE).log("Error in HUD config saving thread: " + e.getMessage());
                }
            }
        });
        this.savingThread.setDaemon(true);
        this.savingThread.start();
    }

    public void markDirty() {
        this.isDirty = true;
        // NÃO salva automaticamente - apenas marca como modificado
        // O salvamento só acontece quando explicitamente solicitado
    }

    public void save() {
        try {
            // Garante que o config exista antes de salvar
            if (this.config == null) {
                this.config = new HudConfig();
            }
            // NÃO cria campos padrão aqui - preserva o que já existe
            // Só cria se realmente estiver vazio (arquivo novo)
            if (this.config.getFields() == null || this.config.getFields().isEmpty()) {
                this.config.createDefaultFields();
            }
            
            // IMPORTANTE: Sempre atualiza a referência no hudConfigFile antes de salvar
            // Isso garante que estamos salvando a versão mais recente do config
            this.hudConfigFile.setConfig(this.config);
            this.hudConfigFile.syncSave();
            this.isDirty = false;
            this.lastSaveTime = System.currentTimeMillis(); // Atualiza timestamp
            logger.at(Level.FINE).log("HUD config data saved");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR SAVING HUD CONFIG FILE");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
        }
    }

    public HudConfig getConfig() {
        if (config == null) {
            config = new HudConfig();
            // Só cria campos padrão se realmente não tiver campos
            if (config.getFields().isEmpty()) {
                config.createDefaultFields();
            }
        }
        return config;
    }

    public void setConfig(HudConfig config) {
        this.config = config != null ? config : new HudConfig();
        // NÃO cria campos padrão aqui - preserva o que foi passado
        // Só cria se realmente estiver vazio
        if (this.config.getFields() == null || this.config.getFields().isEmpty()) {
            this.config.createDefaultFields();
        }
        markDirty();
        save(); // Salva imediatamente
    }
    
    /**
     * Marca o config como modificado e salva automaticamente.
     * Use este método quando modificar o config diretamente (via getConfig().setXXX()) 
     * para garantir que as mudanças sejam salvas.
     */
    public void markDirtyAndSave() {
        if (this.config != null) {
            // Atualiza a referência no hudConfigFile para garantir sincronização
            this.hudConfigFile.setConfig(this.config);
            markDirty();
            save(); // Salva imediatamente
        }
    }

    public void shutdown() {
        isShuttingDown = true; // Marca que está desligando
        
        // Interrompe a thread de salvamento
        if (this.savingThread != null) {
            this.savingThread.interrupt();
            try {
                // Espera a thread terminar (máximo 2 segundos)
                this.savingThread.join(2000);
            } catch (InterruptedException e) {
                // Ignora
            }
        }
        
        // NÃO salva durante shutdown - as configurações da HUD são apenas lidas do JSON
        // Se o arquivo não existir, será criado na próxima inicialização
    }
}


