package com.economy.util;

import com.economy.economy.EconomyManager;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Implementação da interface Economy do Cassaforte usando o EconomyManager do EconomySystem
 * Esta classe registra o EconomySystem como provedor de economia no Cassaforte
 */
public class EconomySystemEconomyProvider {

    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem-Cassaforte");
    private static Object economyImplementation = null;

    /**
     * Cria uma implementação da interface Economy do Cassaforte usando reflexão
     * e registra no Cassaforte
     */
    public static boolean register() {
        try {
            // Obtém a classe Cassaforte
            Class<?> cassaforteClass = Class.forName("it.cassaforte.api.Cassaforte");
            
            // Obtém a interface Economy
            Class<?> economyInterface = Class.forName("it.cassaforte.api.economy.Economy");
            
            // Cria uma implementação dinâmica usando Proxy
            economyImplementation = java.lang.reflect.Proxy.newProxyInstance(
                economyInterface.getClassLoader(),
                new Class<?>[] { economyInterface },
                new EconomyInvocationHandler()
            );

            // Tenta registrar usando registerEconomy
            try {
                java.lang.reflect.Method registerMethod = cassaforteClass.getMethod("registerEconomy", economyInterface);
                registerMethod.invoke(null, economyImplementation);
                
                logger.at(Level.INFO).log("EconomySystem registered as economy provider in Cassaforte");
                return true;
            } catch (NoSuchMethodException e) {
                // Tenta método alternativo: getServiceManager().register()
                try {
                    Object serviceManager = cassaforteClass.getMethod("getServiceManager").invoke(null);
                    java.lang.reflect.Method registerMethod = serviceManager.getClass().getMethod("register", Class.class, Object.class);
                    registerMethod.invoke(serviceManager, economyInterface, economyImplementation);
                    
                    logger.at(Level.INFO).log("EconomySystem registered as economy provider in Cassaforte (via ServiceManager)");
                    return true;
                } catch (Exception e2) {
                    logger.at(Level.WARNING).log("Failed to register EconomySystem in Cassaforte: %s", e2.getMessage());
                    return false;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error registering EconomySystem in Cassaforte: %s", e.getMessage());
            return false;
        }
    }

    /**
     * InvocationHandler que delega as chamadas da interface Economy para o EconomyManager
     */
    private static class EconomyInvocationHandler implements java.lang.reflect.InvocationHandler {
        
        private final EconomyManager economyManager = EconomyManager.getInstance();

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            try {
                // Mapeia métodos da interface Economy para métodos do EconomyManager
                if (methodName.equals("getBalance")) {
                    UUID playerUuid = (UUID) args[0];
                    return economyManager.getBalance(playerUuid);
                } else if (methodName.equals("hasAccount")) {
                    UUID playerUuid = (UUID) args[0];
                    return economyManager.hasPlayerBalance(playerUuid);
                } else if (methodName.equals("has")) {
                    UUID playerUuid = (UUID) args[0];
                    double amount = ((Number) args[1]).doubleValue();
                    return economyManager.hasBalance(playerUuid, amount);
                } else if (methodName.equals("depositPlayer")) {
                    UUID playerUuid = (UUID) args[0];
                    double amount = ((Number) args[1]).doubleValue();
                    double oldBalance = economyManager.getBalance(playerUuid);
                    economyManager.addBalance(playerUuid, amount);
                    double newBalance = economyManager.getBalance(playerUuid);
                    // Retorna EconomyResponse
                    return createEconomyResponse(true, newBalance - oldBalance, newBalance, "Deposit successful");
                } else if (methodName.equals("withdrawPlayer")) {
                    UUID playerUuid = (UUID) args[0];
                    double amount = ((Number) args[1]).doubleValue();
                    double oldBalance = economyManager.getBalance(playerUuid);
                    boolean success = economyManager.subtractBalance(playerUuid, amount);
                    double newBalance = economyManager.getBalance(playerUuid);
                    if (success) {
                        return createEconomyResponse(true, oldBalance - newBalance, newBalance, "Withdrawal successful");
                    } else {
                        return createEconomyResponse(false, 0, oldBalance, "Insufficient funds");
                    }
                } else if (methodName.equals("isEnabled")) {
                    return true;
                } else if (methodName.equals("getName")) {
                    return "EconomySystem";
                } else if (methodName.equals("format")) {
                    double amount = ((Number) args[0]).doubleValue();
                    return String.format("%.2f", amount);
                } else {
                    // Método não implementado, retorna valor padrão
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    } else if (method.getReturnType() == double.class || method.getReturnType() == Double.class) {
                        return 0.0;
                    } else if (method.getReturnType() == String.class) {
                        return "";
                    }
                    return null;
                }
            } catch (Exception e) {
                logger.at(Level.WARNING).log("Error in Economy method %s: %s", methodName, e.getMessage());
                throw e;
            }
        }

        /**
         * Cria um objeto EconomyResponse usando reflexão
         */
        private Object createEconomyResponse(boolean success, double amount, double balance, String errorMessage) {
            try {
                Class<?> responseClass = Class.forName("it.cassaforte.api.economy.EconomyResponse");
                // Tenta diferentes construtores
                try {
                    // Construtor: EconomyResponse(boolean success, double amount, double balance, String errorMessage)
                    java.lang.reflect.Constructor<?> constructor = responseClass.getConstructor(boolean.class, double.class, double.class, String.class);
                    return constructor.newInstance(success, amount, balance, errorMessage);
                } catch (NoSuchMethodException e) {
                    // Tenta construtor alternativo
                    try {
                        java.lang.reflect.Constructor<?> constructor = responseClass.getConstructor(boolean.class, double.class, double.class);
                        return constructor.newInstance(success, amount, balance);
                    } catch (NoSuchMethodException e2) {
                        // Usa builder ou método estático se disponível
                        try {
                            java.lang.reflect.Method builderMethod = responseClass.getMethod("builder");
                            Object builder = builderMethod.invoke(null);
                            builder.getClass().getMethod("success", boolean.class).invoke(builder, success);
                            builder.getClass().getMethod("amount", double.class).invoke(builder, amount);
                            builder.getClass().getMethod("balance", double.class).invoke(builder, balance);
                            builder.getClass().getMethod("errorMessage", String.class).invoke(builder, errorMessage);
                            return builder.getClass().getMethod("build").invoke(builder);
                        } catch (Exception e3) {
                            // Fallback: cria um proxy simples
                            return java.lang.reflect.Proxy.newProxyInstance(
                                responseClass.getClassLoader(),
                                new Class<?>[] { responseClass },
                                (proxy, method, args) -> {
                                    String methodName = method.getName();
                                    if (methodName.equals("transactionSuccess")) return success;
                                    if (methodName.equals("getAmount")) return amount;
                                    if (methodName.equals("getBalance")) return balance;
                                    if (methodName.equals("getErrorMessage")) return errorMessage;
                                    return null;
                                }
                            );
                        }
                    }
                }
            } catch (Exception e) {
                logger.at(Level.WARNING).log("Error creating EconomyResponse: %s", e.getMessage());
                // Retorna um proxy simples como fallback
                try {
                    Class<?> responseClass = Class.forName("it.cassaforte.api.economy.EconomyResponse");
                    return java.lang.reflect.Proxy.newProxyInstance(
                        responseClass.getClassLoader(),
                        new Class<?>[] { responseClass },
                        (proxy, method, args) -> {
                            String methodName = method.getName();
                            if (methodName.equals("transactionSuccess")) return success;
                            if (methodName.equals("getAmount")) return amount;
                            if (methodName.equals("getBalance")) return balance;
                            if (methodName.equals("getErrorMessage")) return errorMessage;
                            return null;
                        }
                    );
                } catch (Exception e2) {
                    return null;
                }
            }
        }
    }
}

