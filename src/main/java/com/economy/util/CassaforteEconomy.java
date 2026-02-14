package com.economy.util;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Utilitário para acessar o serviço de economia do Cassaforte (opcional)
 * Baseado em: https://www.curseforge.com/hytale/mods/cassaforte
 */
public class CassaforteEconomy {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem-Cassaforte");
    private static Object economyService = null;
    private static boolean isAvailable = false;

    /**
     * Verifica se o Cassaforte está disponível e inicializa o serviço de economia
     * Deve ser chamado durante a inicialização do plugin
     * Baseado na API: it.cassaforte.api.Cassaforte.getEconomy()
     * 
     * Agora também registra o EconomySystem como provedor de economia no Cassaforte
     */
    public static void initialize() {
        try {
            // Tenta obter o serviço de economia diretamente via Cassaforte.getEconomy()
            // Esta é a forma recomendada pela documentação do Cassaforte
            // Package correto: it.cassaforte.api.Cassaforte
            Class<?> cassaforteClass = Class.forName("it.cassaforte.api.Cassaforte");
            
            economyService = cassaforteClass.getMethod("getEconomy").invoke(null);
            
            if (economyService != null) {
                // Verifica se o serviço está habilitado
                try {
                    Object isEnabled = economyService.getClass().getMethod("isEnabled").invoke(economyService);
                    
                    if (isEnabled instanceof Boolean && (Boolean) isEnabled) {
                        isAvailable = true;
                        // Obtém o nome do serviço de economia
                        try {
                            Object name = economyService.getClass().getMethod("getName").invoke(economyService);
                            if (name instanceof String) {
                                logger.at(Level.INFO).log("Cassaforte economy service initialized: %s", name);
                            } else {
                                logger.at(Level.INFO).log("Cassaforte economy service initialized successfully");
                            }
                        } catch (Exception e) {
                            logger.at(Level.INFO).log("Cassaforte economy service initialized successfully");
                        }
                    } else {
                        logger.at(Level.INFO).log("Cassaforte is available but economy service is disabled");
                    }
                } catch (NoSuchMethodException e) {
                    // Método isEnabled não existe, assume que está disponível
                    isAvailable = true;
                    logger.at(Level.INFO).log("Cassaforte economy service initialized successfully");
                }
            } else {
                // Nenhum serviço de economia foi registrado no Cassaforte
                // Tenta registrar o EconomySystem como provedor
                logger.at(Level.INFO).log("No economy service registered in Cassaforte, registering EconomySystem as provider...");
                boolean registered = com.economy.util.EconomySystemEconomyProvider.register();
                
                if (registered) {
                    // Tenta obter o serviço novamente após o registro
                    try {
                        economyService = cassaforteClass.getMethod("getEconomy").invoke(null);
                        if (economyService != null) {
                            isAvailable = true;
                            logger.at(Level.INFO).log("EconomySystem successfully registered and available in Cassaforte");
                        } else {
                            logger.at(Level.WARNING).log("EconomySystem registered but getEconomy() still returns null");
                        }
                    } catch (Exception e) {
                        logger.at(Level.WARNING).log("Error getting economy service after registration: %s", e.getMessage());
                    }
                } else {
                    logger.at(Level.WARNING).log("Failed to register EconomySystem as economy provider in Cassaforte");
                }
            }
            
        } catch (ClassNotFoundException e) {
            // Cassaforte não está disponível - isso é normal se o JAR não estiver presente
        } catch (NoSuchMethodException e) {
            // Método getEconomy() não encontrado - pode ser versão diferente da API
            logger.at(Level.WARNING).log("Cassaforte.getEconomy() method not found, trying alternative methods");
            tryAlternativeInitialization();
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error initializing Cassaforte economy service: %s", e.getMessage());
            tryAlternativeInitialization();
        }
    }
    
    /**
     * Tenta inicializar usando métodos alternativos (fallback)
     */
    private static void tryAlternativeInitialization() {
        try {
            // Tenta obter via plugin manager usando o identificador correto: it.cassaforte:Cassaforte
            Class<?> hytaleServerClass = Class.forName("com.hypixel.hytale.server.core.plugin.HytaleServer");
            Object hytaleServer = hytaleServerClass.getMethod("get").invoke(null);
            Object pluginManager = hytaleServerClass.getMethod("getPluginManager").invoke(hytaleServer);
            
            Class<?> pluginIdentifierClass = Class.forName("com.hypixel.hytale.common.plugin.PluginIdentifier");
            Class<?> semverRangeClass = Class.forName("com.hypixel.hytale.common.plugin.SemverRange");
            Object wildcardRange = semverRangeClass.getField("WILDCARD").get(null);
            
            // Identificador correto baseado na documentação: it.cassaforte:Cassaforte
            String[] possibleIdentifiers = {
                "it.cassaforte:Cassaforte",
                "Cassaforte:Cassaforte",
                "Filocava99:Cassaforte"
            };
            
            for (String identifier : possibleIdentifiers) {
                try {
                    Object pluginIdentifier = pluginIdentifierClass.getMethod("fromString", String.class).invoke(null, identifier);
                    Object pluginInstance = pluginManager.getClass().getMethod("getPlugin", pluginIdentifierClass, semverRangeClass)
                            .invoke(pluginManager, pluginIdentifier, wildcardRange);
                    
                    if (pluginInstance != null) {
                        // Tenta obter via Cassaforte.getEconomy() novamente
                        try {
                            Class<?> cassaforteClass = Class.forName("it.cassaforte.api.Cassaforte");
                            economyService = cassaforteClass.getMethod("getEconomy").invoke(null);
                            if (economyService != null) {
                                isAvailable = true;
                                logger.at(Level.INFO).log("Cassaforte economy service initialized via plugin manager");
                                return;
                            }
                        } catch (Exception e) {
                            // Continua tentando
                        }
                    }
                } catch (Exception e) {
                    // Tenta próximo identificador
                }
            }
        } catch (Exception e) {
            // Fallback falhou, mas não é crítico
        }
    }

    /**
     * Verifica se o serviço de economia do Cassaforte está disponível
     * @return true se o Cassaforte está instalado e o serviço está disponível
     */
    public static boolean isAvailable() {
        return isAvailable && economyService != null;
    }

    /**
     * Verifica se um jogador tem conta no sistema de economia do Cassaforte
     * @param playerUuid UUID do jogador
     * @return true se o jogador tem conta
     */
    public static boolean hasAccount(UUID playerUuid) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            Object result = economyService.getClass().getMethod("hasAccount", UUID.class).invoke(economyService, playerUuid);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (NoSuchMethodException e) {
            // Método não encontrado, assume que tem conta se conseguir obter saldo
            return getBalance(playerUuid) >= 0;
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error checking account in Cassaforte: %s", e.getMessage());
        }
        
        return false;
    }

    /**
     * Obtém o saldo de um jogador via Cassaforte
     * @param playerUuid UUID do jogador
     * @return Saldo do jogador, ou 0.0 se o Cassaforte não estiver disponível ou jogador não tiver conta
     */
    public static double getBalance(UUID playerUuid) {
        if (!isAvailable()) {
            return 0.0;
        }
        
        try {
            // Verifica se o jogador tem conta primeiro
            if (!hasAccount(playerUuid)) {
                return 0.0;
            }
            
            // Obtém o saldo
            Object result = economyService.getClass().getMethod("getBalance", UUID.class).invoke(economyService, playerUuid);
            if (result instanceof Double) {
                return (Double) result;
            } else if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (NoSuchMethodException e) {
            // Tenta métodos alternativos
            String[] possibleMethods = {"getPlayerBalance", "balance"};
            for (String methodName : possibleMethods) {
                try {
                    Object result = economyService.getClass().getMethod(methodName, UUID.class).invoke(economyService, playerUuid);
                    if (result instanceof Double) {
                        return (Double) result;
                    } else if (result instanceof Number) {
                        return ((Number) result).doubleValue();
                    }
                } catch (Exception ex) {
                    // Continua tentando
                }
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error getting balance from Cassaforte: %s", e.getMessage());
        }
        
        return 0.0;
    }
    
    /**
     * Formata um valor monetário usando o formato do Cassaforte
     * @param amount Valor a formatar
     * @return String formatada (ex: "$1,234.56")
     */
    public static String format(double amount) {
        if (!isAvailable()) {
            return String.valueOf(amount);
        }
        
        try {
            Object result = economyService.getClass().getMethod("format", double.class).invoke(economyService, amount);
            if (result instanceof String) {
                return (String) result;
            }
        } catch (NoSuchMethodException e) {
            // Método format não disponível, retorna valor sem formatação
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error formatting amount via Cassaforte: %s", e.getMessage());
        }
        
        return String.valueOf(amount);
    }

    /**
     * Deposita dinheiro na conta de um jogador via Cassaforte
     * @param playerUuid UUID do jogador
     * @param amount Quantia a depositar
     * @return true se o depósito foi bem-sucedido
     */
    public static boolean depositPlayer(UUID playerUuid, double amount) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            // Usa depositPlayer que retorna EconomyResponse conforme a API
            Object result = economyService.getClass().getMethod("depositPlayer", UUID.class, double.class).invoke(economyService, playerUuid, amount);
            
            // Verifica se retornou EconomyResponse
            if (result != null) {
                try {
                    // EconomyResponse tem transactionSuccess() conforme a documentação
                    Object success = result.getClass().getMethod("transactionSuccess").invoke(result);
                    if (success instanceof Boolean) {
                        return (Boolean) success;
                    }
                } catch (NoSuchMethodException e) {
                    // Tenta métodos alternativos
                    String[] successMethods = {"isSuccess", "success", "wasSuccessful"};
                    for (String methodName : successMethods) {
                        try {
                            Object success = result.getClass().getMethod(methodName).invoke(result);
                            if (success instanceof Boolean) {
                                return (Boolean) success;
                            }
                        } catch (Exception ex) {
                            // Continua
                        }
                    }
                    // Se não encontrou método, assume sucesso se não for null
                    return true;
                } catch (Exception e) {
                    // Erro ao verificar, assume falha
                }
            }
        } catch (NoSuchMethodException e) {
            logger.at(Level.WARNING).log("Cassaforte depositPlayer method not found");
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error depositing to player via Cassaforte: %s", e.getMessage());
        }
        
        return false;
    }

    /**
     * Retira dinheiro da conta de um jogador via Cassaforte
     * @param playerUuid UUID do jogador
     * @param amount Quantia a retirar
     * @return true se a retirada foi bem-sucedida
     */
    public static boolean withdrawPlayer(UUID playerUuid, double amount) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            // Usa withdrawPlayer que retorna EconomyResponse conforme a API
            Object result = economyService.getClass().getMethod("withdrawPlayer", UUID.class, double.class).invoke(economyService, playerUuid, amount);
            
            // Verifica se retornou EconomyResponse
            if (result != null) {
                try {
                    // EconomyResponse tem transactionSuccess() conforme a documentação
                    Object success = result.getClass().getMethod("transactionSuccess").invoke(result);
                    if (success instanceof Boolean) {
                        return (Boolean) success;
                    }
                } catch (NoSuchMethodException e) {
                    // Tenta métodos alternativos
                    String[] successMethods = {"isSuccess", "success", "wasSuccessful"};
                    for (String methodName : successMethods) {
                        try {
                            Object success = result.getClass().getMethod(methodName).invoke(result);
                            if (success instanceof Boolean) {
                                return (Boolean) success;
                            }
                        } catch (Exception ex) {
                            // Continua
                        }
                    }
                    return true;
                } catch (Exception e) {
                    // Erro ao verificar, assume falha
                }
            }
        } catch (NoSuchMethodException e) {
            logger.at(Level.WARNING).log("Cassaforte withdrawPlayer method not found");
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error withdrawing from player via Cassaforte: %s", e.getMessage());
        }
        
        return false;
    }

    /**
     * Define o saldo de um jogador via Cassaforte
     * @param playerUuid UUID do jogador
     * @param amount Novo saldo
     * @return true se a definição foi bem-sucedida
     */
    public static boolean setBalance(UUID playerUuid, double amount) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            String[] possibleMethods = {"setBalance", "setPlayerBalance", "setMoney"};
            for (String methodName : possibleMethods) {
                try {
                    Object result = economyService.getClass().getMethod(methodName, UUID.class, double.class).invoke(economyService, playerUuid, amount);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    } else if (result != null) {
                        // Se retornou algo (não null), assume sucesso
                        return true;
                    }
                } catch (NoSuchMethodException e) {
                    // Tenta próximo método
                } catch (Exception e) {
                    // Outro erro, tenta próximo método
                }
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error setting balance via Cassaforte: %s", e.getMessage());
        }
        
        return false;
    }

    /**
     * Verifica se um jogador tem saldo suficiente via Cassaforte
     * Usa o método has() da API conforme a documentação
     * @param playerUuid UUID do jogador
     * @param amount Quantia a verificar
     * @return true se o jogador tem saldo suficiente
     */
    public static boolean hasBalance(UUID playerUuid, double amount) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            // Usa o método has() conforme a documentação: economy.has(playerId, amount)
            Object result = economyService.getClass().getMethod("has", UUID.class, double.class).invoke(economyService, playerUuid, amount);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (NoSuchMethodException e) {
            // Fallback: calcula manualmente
            double balance = getBalance(playerUuid);
            return balance >= amount;
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error checking balance via Cassaforte: %s", e.getMessage());
            // Fallback: calcula manualmente
            double balance = getBalance(playerUuid);
            return balance >= amount;
        }
        
        return false;
    }
}

