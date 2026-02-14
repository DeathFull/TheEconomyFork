package com.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.HashMap;
import java.util.Map;

public class EconomyConfig {

    public static final BuilderCodec<EconomyConfig> CODEC = BuilderCodec.builder(EconomyConfig.class, EconomyConfig::new)
    
            .append(new KeyedCodec<Boolean>("EnableMySQL", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableMySQL = value != null ? value : false,
                    (config, extraInfo) -> config.enableMySQL).add()
            .append(new KeyedCodec<String>("MySQLHost", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlHost = value != null && !value.isEmpty() ? value : "localhost",
                    (config, extraInfo) -> config.mysqlHost).add()
            .append(new KeyedCodec<Integer>("MySQLPort", Codec.INTEGER),
                    (config, value, extraInfo) -> config.mysqlPort = value != null ? value : 3306,
                    (config, extraInfo) -> config.mysqlPort).add()
            .append(new KeyedCodec<String>("MySQLUser", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlUser = value != null && !value.isEmpty() ? value : "root",
                    (config, extraInfo) -> config.mysqlUser).add()
            .append(new KeyedCodec<String>("MySQLPassword", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlPassword = value != null ? value : "",
                    (config, extraInfo) -> config.mysqlPassword).add()
            .append(new KeyedCodec<String>("MySQLDatabaseName", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlDatabaseName = value != null && !value.isEmpty() ? value : "theeconomy",
                    (config, extraInfo) -> config.mysqlDatabaseName).add()
            .append(new KeyedCodec<String>("MySQLTableName", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlTableName = value != null && !value.isEmpty() ? value : "bank",
                    (config, extraInfo) -> config.mysqlTableName).add()
            .append(new KeyedCodec<String>("MySQLAdminShopTableName", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlAdminShopTableName = value != null && !value.isEmpty() ? value : "adminshop",
                    (config, extraInfo) -> config.mysqlAdminShopTableName).add()
            .append(new KeyedCodec<String>("MySQLPlayerShopTableName", Codec.STRING),
                    (config, value, extraInfo) -> config.mysqlPlayerShopTableName = value != null && !value.isEmpty() ? value : "playershop",
                    (config, extraInfo) -> config.mysqlPlayerShopTableName).add()
            .append(new KeyedCodec<String>("Language", Codec.STRING),
                    (config, value, extraInfo) -> config.language = value != null ? value : "EN",
                    (config, extraInfo) -> config.language).add()
            .append(new KeyedCodec<Double>("InitialBalance", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.initialBalance = value != null ? value : 1000.0,
                    (config, extraInfo) -> config.initialBalance).add()
            .append(new KeyedCodec<String>("CurrencySymbol", Codec.STRING),
                    (config, value, extraInfo) -> config.currencySymbol = value != null && !value.isEmpty() ? value : "$",
                    (config, extraInfo) -> config.currencySymbol).add()
            .append(new KeyedCodec<Boolean>("EnableOreRewards", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableOreRewards = value,
                    (config, extraInfo) -> config.enableOreRewards).add()
            .append(new KeyedCodec<Boolean>("EnableWoodRewards", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableWoodRewards = value,
                    (config, extraInfo) -> config.enableWoodRewards).add()
            .append(new KeyedCodec<Boolean>("EnableDebugLogs", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableDebugLogs = value,
                    (config, extraInfo) -> config.enableDebugLogs).add()
            .append(new KeyedCodec<Boolean>("EnableShop", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableShop = value != null ? value : true,
                    (config, extraInfo) -> config.enableShop).add()
            .append(new KeyedCodec<Boolean>("EnablePlayerShop", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enablePlayerShop = value != null ? value : true,
                    (config, extraInfo) -> config.enablePlayerShop).add()
            .append(new KeyedCodec<Boolean>("EnableHud", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableHud = value != null ? value : true,
                    (config, extraInfo) -> config.enableHud).add()
            .append(new KeyedCodec<Boolean>("EnableMoneyTop", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableMoneyTop = value != null ? value : true,
                    (config, extraInfo) -> config.enableMoneyTop).add()
            .append(new KeyedCodec<Boolean>("InvertBuyButtonAction", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.invertBuyButtonAction = value != null ? value : false,
                    (config, extraInfo) -> config.invertBuyButtonAction).add()
            .append(new KeyedCodec<String>("ShortNumberFormat", Codec.STRING),
                    (config, value, extraInfo) -> config.shortNumberFormat = value != null && !value.isEmpty() ? value : "kk",
                    (config, extraInfo) -> config.shortNumberFormat).add()
            .append(new KeyedCodec<Double>("PlayerTax", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.playerTax = value != null ? value : 0.0,
                    (config, extraInfo) -> config.playerTax).add()
            .append(new KeyedCodec<Boolean>("EnableMonsterRewards", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.enableMonsterRewards = value,
                    (config, extraInfo) -> config.enableMonsterRewards).add()
            .append(new KeyedCodec<MonsterRewardEntry[]>("MonsterRewards", MonsterRewardEntry.CODEC_ARRAY),
                    (config, value, extraInfo) -> {
                        config.monsterRewards = new HashMap<>();
                        if (value != null && value.length > 0) {
                            for (MonsterRewardEntry entry : value) {
                                config.monsterRewards.put(entry.getMonsterId().toLowerCase(), entry.getReward());
                            }
                        }
                        // Não inicializa padrões se o array estiver vazio - preserva valores personalizados
                        // Os padrões só são inicializados no construtor se necessário
                    },
                    (config, extraInfo) -> {
                        MonsterRewardEntry[] entries = new MonsterRewardEntry[config.monsterRewards.size()];
                        int i = 0;
                        for (Map.Entry<String, Double> entry : config.monsterRewards.entrySet()) {
                            entries[i++] = new MonsterRewardEntry(entry.getKey(), entry.getValue());
                        }
                        return entries;
                    }).add()
            .append(new KeyedCodec<OreRewardEntry[]>("OreRewards", OreRewardEntry.CODEC_ARRAY),
                    (config, value, extraInfo) -> {
                        config.oreRewards = new HashMap<>();
                        if (value != null && value.length > 0) {
                            for (OreRewardEntry entry : value) {
                                config.oreRewards.put(entry.getOreName().toLowerCase(), entry.getReward());
                            }
                        }
                        // Não inicializa padrões se o array estiver vazio - preserva valores personalizados
                        // Os padrões só são inicializados no construtor se necessário
                    },
                    (config, extraInfo) -> {
                        OreRewardEntry[] entries = new OreRewardEntry[config.oreRewards.size()];
                        int i = 0;
                        for (Map.Entry<String, Double> entry : config.oreRewards.entrySet()) {
                            entries[i++] = new OreRewardEntry(entry.getKey(), entry.getValue());
                        }
                        return entries;
                    }).add()
            .append(new KeyedCodec<WoodRewardEntry[]>("WoodRewards", WoodRewardEntry.CODEC_ARRAY),
                    (config, value, extraInfo) -> {
                        config.woodRewards = new HashMap<>();
                        if (value != null && value.length > 0) {
                            for (WoodRewardEntry entry : value) {
                                config.woodRewards.put(entry.getWoodName().toLowerCase(), entry.getReward());
                            }
                        }
                        // Não inicializa padrões se o array estiver vazio - preserva valores personalizados
                        // Os padrões só são inicializados no construtor se necessário
                    },
                    (config, extraInfo) -> {
                        WoodRewardEntry[] entries = new WoodRewardEntry[config.woodRewards.size()];
                        int i = 0;
                        for (Map.Entry<String, Double> entry : config.woodRewards.entrySet()) {
                            entries[i++] = new WoodRewardEntry(entry.getKey(), entry.getValue());
                        }
                        return entries;
                    }).add()
            .build();

    private String language = "EN";
    private double initialBalance = 1000.0;
    private String currencySymbol = "$";
    private boolean enableOreRewards = true;
    private boolean enableWoodRewards = true;
    private boolean enableMonsterRewards = true;
    private boolean enableDebugLogs = false;
    private boolean enableShop = true;
    private boolean enablePlayerShop = true;
    private boolean enableHud = true;
    private boolean enableMoneyTop = true;
    private boolean invertBuyButtonAction = false;
    private String shortNumberFormat = "kk"; // "kk" para 1k, 1kk, 1kkk, 1kkkk ou "international" para 1k, 1m, 1b, 1t
    private double playerTax = 0.0;
    private Map<String, Double> monsterRewards = new HashMap<>();
    private Map<String, Double> oreRewards = new HashMap<>();
    private Map<String, Double> woodRewards = new HashMap<>();
    
    // MySQL settings (only used if enableMySQL = true)
    private boolean enableMySQL = false;
    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlUser = "root";
    private String mysqlPassword = "";
    private String mysqlDatabaseName = "theeconomy";
    private String mysqlTableName = "bank";
    private String mysqlAdminShopTableName = "adminshop";
    private String mysqlPlayerShopTableName = "playershop";

    public EconomyConfig() {
        // Inicializa os valores padrão para todos os minérios, madeiras e monstros
        initializeDefaultOreRewards();
        initializeDefaultWoodRewards();
        initializeDefaultMonsterRewards();
    }

    void initializeDefaultOreRewards() {
        // Iron Ores
        oreRewards.put("ore_iron_stone", 1.0);
        oreRewards.put("ore_iron_basalt", 1.0);
        oreRewards.put("ore_iron_shale", 1.0);
        oreRewards.put("ore_iron_slate", 1.0);
        oreRewards.put("ore_iron_sandstone", 1.0);
        oreRewards.put("ore_iron_volcanic", 1.0);

        // Copper Ores
        oreRewards.put("ore_copper_stone", 1.5);
        oreRewards.put("ore_copper_basalt", 1.5);
        oreRewards.put("ore_copper_shale", 1.5);
        oreRewards.put("ore_copper_sandstone", 1.5);
        oreRewards.put("ore_copper_volcanic", 1.5);

        // Silver Ores
        oreRewards.put("ore_silver_stone", 2.0);
        oreRewards.put("ore_silver_basalt", 2.0);
        oreRewards.put("ore_silver_shale", 2.0);
        oreRewards.put("ore_silver_slate", 2.0);
        oreRewards.put("ore_silver_sandstone", 2.0);
        oreRewards.put("ore_silver_volcanic", 2.0);

        // Gold Ores
        oreRewards.put("ore_gold_stone", 2.5);
        oreRewards.put("ore_gold_sandstone", 2.5);
        oreRewards.put("ore_gold_basalt", 2.5);
        oreRewards.put("ore_gold_shale", 2.5);
        oreRewards.put("ore_gold_volcanic", 2.5);

        // Cobalt Ores
        oreRewards.put("ore_cobalt_stone", 3.0);
        oreRewards.put("ore_cobalt_basalt", 3.0);
        oreRewards.put("ore_cobalt_shale", 3.0);
        oreRewards.put("ore_cobalt_slate", 3.0);
        oreRewards.put("ore_cobalt_sandstone", 3.0);
        oreRewards.put("ore_cobalt_volcanic", 3.0);

        // Mithril Ores
        oreRewards.put("ore_mithril_stone", 4.0);
        oreRewards.put("ore_mithril_basalt", 4.0);
        oreRewards.put("ore_mithril_slate", 4.0);
        oreRewards.put("ore_mithril_volcanic", 4.0);
        oreRewards.put("ore_mithril_magma", 4.0);

        // Adamantite Ores
        oreRewards.put("ore_adamantite_stone", 5.0);
        oreRewards.put("ore_adamantite_basalt", 5.0);
        oreRewards.put("ore_adamantite_slate", 5.0);
        oreRewards.put("ore_adamantite_shale", 5.0);
        oreRewards.put("ore_adamantite_volcanic", 5.0);

        // Thorium Ores
        oreRewards.put("ore_thorium_stone", 6.0);
        oreRewards.put("ore_thorium_basalt", 6.0);
        oreRewards.put("ore_thorium_shale", 6.0);
        oreRewards.put("ore_thorium_sandstone", 6.0);
        oreRewards.put("ore_thorium_volcanic", 6.0);

        // Onyxium Ores
        oreRewards.put("ore_onyxium_stone", 7.0);
        oreRewards.put("ore_onyxium_basalt", 7.0);
        oreRewards.put("ore_onyxium_shale", 7.0);
        oreRewards.put("ore_onyxium_sandstone", 7.0);
        oreRewards.put("ore_onyxium_volcanic", 7.0);
    }

    void initializeDefaultWoodRewards() {
        // Todos os tipos de madeira com valor padrão de 0.5
        woodRewards.put("wood_fir_trunk", 0.5);
        woodRewards.put("wood_windwillow_trunk", 0.5);
        woodRewards.put("wood_oak_trunk", 0.5);
        woodRewards.put("wood_fire_trunk", 0.5);
        woodRewards.put("wood_crystal_trunk", 0.5);
        woodRewards.put("wood_petrified_trunk", 0.5);
        woodRewards.put("wood_jungle_trunk", 0.5);
        woodRewards.put("wood_fig_blue_trunk", 0.5);
        woodRewards.put("wood_gumboab_trunk", 0.5);
        woodRewards.put("wood_cedar_trunk", 0.5);
        woodRewards.put("wood_bamboo_trunk_deco", 0.5);
        woodRewards.put("wood_maple_trunk", 0.5);
        woodRewards.put("wood_poisoned_trunk", 0.5);
        woodRewards.put("wood_beech_trunk", 0.5);
        woodRewards.put("wood_wisteria_wild_trunk", 0.5);
        woodRewards.put("wood_azure_trunk", 0.5);
    }

    void initializeDefaultMonsterRewards() {
        // Monstros com recompensas padrão (todos em lowercase para consistência)
        monsterRewards.put("skeleton_fighter", 1.0);
        monsterRewards.put("crawler_void", 2.0);
        monsterRewards.put("rabbit", 1.0);
        monsterRewards.put("cow", 1.0);
        monsterRewards.put("sheep", 1.0);
        monsterRewards.put("lamb", 1.0);
        monsterRewards.put("calf", 1.0);
        monsterRewards.put("tetrabird", 1.0);
    }


    public boolean isEnableOreRewards() {
        return enableOreRewards;
    }

    public void setEnableOreRewards(boolean enableOreRewards) {
        this.enableOreRewards = enableOreRewards;
    }

    public boolean isEnableWoodRewards() {
        return enableWoodRewards;
    }

    public void setEnableWoodRewards(boolean enableWoodRewards) {
        this.enableWoodRewards = enableWoodRewards;
    }

    public boolean isEnableMonsterRewards() {
        return enableMonsterRewards;
    }

    public void setEnableMonsterRewards(boolean enableMonsterRewards) {
        this.enableMonsterRewards = enableMonsterRewards;
    }

    public boolean isEnableDebugLogs() {
        return enableDebugLogs;
    }

    public void setEnableDebugLogs(boolean enableDebugLogs) {
        this.enableDebugLogs = enableDebugLogs;
    }

    public Map<String, Double> getOreRewards() {
        return oreRewards;
    }

    public void setOreRewards(Map<String, Double> oreRewards) {
        this.oreRewards = oreRewards != null ? new HashMap<>(oreRewards) : new HashMap<>();
        // Não inicializa padrões automaticamente - preserva valores personalizados vazios
    }

    /**
     * Obtém o valor de recompensa para um minério específico
     * @param oreName Nome do minério (case-insensitive)
     * @return Valor da recompensa ou 0.0 se não encontrado
     */
    public double getOreReward(String oreName) {
        // Não inicializa padrões automaticamente - retorna 0.0 se não encontrado
        // Isso preserva valores personalizados vazios
        if (oreRewards == null) {
            return 0.0;
        }
        String key = oreName.toLowerCase();
        return oreRewards.getOrDefault(key, 0.0);
    }

    public Map<String, Double> getWoodRewards() {
        return woodRewards;
    }

    public void setWoodRewards(Map<String, Double> woodRewards) {
        this.woodRewards = woodRewards != null ? new HashMap<>(woodRewards) : new HashMap<>();
        // Não inicializa padrões automaticamente - preserva valores personalizados vazios
    }

    /**
     * Obtém o valor de recompensa para uma madeira específica
     * @param woodName Nome da madeira (case-insensitive)
     * @return Valor da recompensa ou 0.0 se não encontrado
     */
    public double getWoodReward(String woodName) {
        // Não inicializa padrões automaticamente - retorna 0.0 se não encontrado
        // Isso preserva valores personalizados vazios
        if (woodRewards == null) {
            return 0.0;
        }
        String key = woodName.toLowerCase();
        return woodRewards.getOrDefault(key, 0.0);
    }

    /**
     * Obtém o valor de recompensa para monstros (valor único para todos)
     * @return Valor da recompensa padrão
     */
    public Map<String, Double> getMonsterRewards() {
        // Não inicializa padrões automaticamente - preserva valores personalizados vazios
        if (monsterRewards == null) {
            return new HashMap<>();
        }
        return monsterRewards;
    }

    public void setMonsterRewards(Map<String, Double> monsterRewards) {
        this.monsterRewards = monsterRewards != null ? new HashMap<>(monsterRewards) : new HashMap<>();
    }

    public Double getMonsterReward(String monsterId) {
        // Não inicializa padrões automaticamente - retorna null se não encontrado
        // Isso preserva valores personalizados vazios
        if (monsterRewards == null || monsterId == null) {
            return null;
        }
        return monsterRewards.get(monsterId.toLowerCase());
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language != null ? language : "EN";
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol != null && !currencySymbol.isEmpty() ? currencySymbol : "$";
    }

    public boolean isEnableShop() {
        return enableShop;
    }

    public void setEnableShop(boolean enableShop) {
        this.enableShop = enableShop;
    }

    public boolean isEnablePlayerShop() {
        return enablePlayerShop;
    }

    public void setEnablePlayerShop(boolean enablePlayerShop) {
        this.enablePlayerShop = enablePlayerShop;
    }

    public boolean isEnableHud() {
        return enableHud;
    }

    public void setEnableHud(boolean enableHud) {
        this.enableHud = enableHud;
    }

    public boolean isEnableMoneyTop() {
        return enableMoneyTop;
    }

    public void setEnableMoneyTop(boolean enableMoneyTop) {
        this.enableMoneyTop = enableMoneyTop;
    }

    public boolean isInvertBuyButtonAction() {
        return invertBuyButtonAction;
    }

    public void setInvertBuyButtonAction(boolean invertBuyButtonAction) {
        this.invertBuyButtonAction = invertBuyButtonAction;
    }

    public String getShortNumberFormat() {
        return shortNumberFormat != null ? shortNumberFormat : "kk";
    }

    public void setShortNumberFormat(String shortNumberFormat) {
        this.shortNumberFormat = shortNumberFormat != null && !shortNumberFormat.isEmpty() ? shortNumberFormat : "kk";
    }

    public double getPlayerTax() {
        return playerTax;
    }

    public void setPlayerTax(double playerTax) {
        this.playerTax = playerTax >= 0 && playerTax <= 100 ? playerTax : 0.0;
    }

    public boolean isEnableMySQL() {
        return enableMySQL;
    }

    public void setEnableMySQL(boolean enableMySQL) {
        this.enableMySQL = enableMySQL;
    }

    public String getMySQLHost() {
        return mysqlHost;
    }

    public void setMySQLHost(String mysqlHost) {
        this.mysqlHost = mysqlHost != null && !mysqlHost.isEmpty() ? mysqlHost : "localhost";
    }

    public int getMySQLPort() {
        return mysqlPort;
    }

    public void setMySQLPort(int mysqlPort) {
        this.mysqlPort = mysqlPort > 0 ? mysqlPort : 3306;
    }

    public String getMySQLUser() {
        return mysqlUser;
    }

    public void setMySQLUser(String mysqlUser) {
        this.mysqlUser = mysqlUser != null && !mysqlUser.isEmpty() ? mysqlUser : "root";
    }

    public String getMySQLPassword() {
        return mysqlPassword;
    }

    public void setMySQLPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword != null ? mysqlPassword : "";
    }

    public String getMySQLDatabaseName() {
        return mysqlDatabaseName;
    }

    public void setMySQLDatabaseName(String mysqlDatabaseName) {
        this.mysqlDatabaseName = mysqlDatabaseName != null && !mysqlDatabaseName.isEmpty() ? mysqlDatabaseName : "theeconomy";
    }

    public String getMySQLTableName() {
        return mysqlTableName;
    }

    public void setMySQLTableName(String mysqlTableName) {
        this.mysqlTableName = mysqlTableName != null && !mysqlTableName.isEmpty() ? mysqlTableName : "bank";
    }

    public String getMySQLAdminShopTableName() {
        return mysqlAdminShopTableName;
    }

    public void setMySQLAdminShopTableName(String mysqlAdminShopTableName) {
        this.mysqlAdminShopTableName = mysqlAdminShopTableName != null && !mysqlAdminShopTableName.isEmpty() ? mysqlAdminShopTableName : "adminshop";
    }

    public String getMySQLPlayerShopTableName() {
        return mysqlPlayerShopTableName;
    }

    public void setMySQLPlayerShopTableName(String mysqlPlayerShopTableName) {
        this.mysqlPlayerShopTableName = mysqlPlayerShopTableName != null && !mysqlPlayerShopTableName.isEmpty() ? mysqlPlayerShopTableName : "playershop";
    }
}
