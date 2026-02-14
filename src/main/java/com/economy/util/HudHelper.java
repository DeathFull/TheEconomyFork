package com.economy.util;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.logger.HytaleLogger;

import java.lang.reflect.Method;

/**
 * Helper class para gerenciar HUDs com suporte ao MultipleHUD
 * Permite compatibilidade com PartyPlugin e SimpleParty
 */
public class HudHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final PluginIdentifier MULTIPLE_HUD_ID = new PluginIdentifier("Buuz135", "MultipleHUD");
    private static final String HUD_IDENTIFIER = "economy_hud";
    
    private static Boolean multipleHudAvailable = null;
    private static Object multipleHudInstance = null;
    private static Method setCustomHudMethod = null;
    private static Method hideCustomHudMethod = null;
    
    /**
     * Verifica se o MultipleHUD está disponível
     */
    private static boolean checkMultipleHudAvailable() {
        if (multipleHudAvailable == null) {
            try {
                // Tenta verificar via PluginManager primeiro (método do PartyPlugin)
                PluginBase multipleHudPlugin = PluginManager.get().getPlugin(MULTIPLE_HUD_ID);
                if (multipleHudPlugin != null) {
                    // Tenta primeiro o pacote que SimpleParty usa (com.buuz135.multiplehud)
                    // para garantir compatibilidade
                    try {
                        Class<?> multipleHudClass = Class.forName("com.buuz135.multiplehud.MultipleHUD");
                        Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
                        multipleHudInstance = getInstanceMethod.invoke(null);
                        setCustomHudMethod = multipleHudClass.getMethod("setCustomHud", 
                            Player.class, PlayerRef.class, String.class, CustomUIHud.class);
                        hideCustomHudMethod = multipleHudClass.getMethod("hideCustomHud", 
                            Player.class, PlayerRef.class, String.class);
                        multipleHudAvailable = true;
                        LOGGER.atInfo().log("MultipleHUD detected (com.buuz135.multiplehud) - using multiple HUD support for compatibility with SimpleParty/PartyPlugin");
                    } catch (Exception e) {
                        // Se falhar, tenta o pacote alternativo (com.buuz135.mhud) usado pelo PartyPlugin
                        try {
                            Class<?> multipleHudClass = Class.forName("com.buuz135.mhud.MultipleHUD");
                            Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
                            multipleHudInstance = getInstanceMethod.invoke(null);
                            setCustomHudMethod = multipleHudClass.getMethod("setCustomHud", 
                                Player.class, PlayerRef.class, String.class, CustomUIHud.class);
                            hideCustomHudMethod = multipleHudClass.getMethod("hideCustomHud", 
                                Player.class, PlayerRef.class, String.class);
                            multipleHudAvailable = true;
                            LOGGER.atInfo().log("MultipleHUD detected (com.buuz135.mhud) - using multiple HUD support for compatibility with PartyPlugin");
                        } catch (Exception e2) {
                            multipleHudAvailable = false;
                            LOGGER.atWarning().log("MultipleHUD plugin found but API access failed for both packages: %s", e2.getMessage());
                        }
                    }
                } else {
                    // Se não encontrou via PluginManager, tenta diretamente via reflection
                    // (para casos onde o plugin está presente mas não registrado)
                    try {
                        Class<?> multipleHudClass = Class.forName("com.buuz135.multiplehud.MultipleHUD");
                        Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
                        multipleHudInstance = getInstanceMethod.invoke(null);
                        setCustomHudMethod = multipleHudClass.getMethod("setCustomHud", 
                            Player.class, PlayerRef.class, String.class, CustomUIHud.class);
                        hideCustomHudMethod = multipleHudClass.getMethod("hideCustomHud", 
                            Player.class, PlayerRef.class, String.class);
                        multipleHudAvailable = true;
                        LOGGER.atInfo().log("MultipleHUD detected via reflection (com.buuz135.multiplehud) - using multiple HUD support");
                    } catch (Exception e) {
                        try {
                            Class<?> multipleHudClass = Class.forName("com.buuz135.mhud.MultipleHUD");
                            Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
                            multipleHudInstance = getInstanceMethod.invoke(null);
                            setCustomHudMethod = multipleHudClass.getMethod("setCustomHud", 
                                Player.class, PlayerRef.class, String.class, CustomUIHud.class);
                            hideCustomHudMethod = multipleHudClass.getMethod("hideCustomHud", 
                                Player.class, PlayerRef.class, String.class);
                            multipleHudAvailable = true;
                            LOGGER.atInfo().log("MultipleHUD detected via reflection (com.buuz135.mhud) - using multiple HUD support");
                        } catch (Exception e2) {
                            multipleHudAvailable = false;
                            LOGGER.atFine().log("MultipleHUD not found - using standard HUD mode");
                        }
                    }
                }
            } catch (Exception e) {
                multipleHudAvailable = false;
                LOGGER.atFine().log("Error checking for MultipleHUD: %s - using standard HUD mode", e.getMessage());
            }
        }
        return multipleHudAvailable != null && multipleHudAvailable;
    }
    
    /**
     * Verifica se o MultipleHUD está disponível
     */
    public static boolean isMultipleHudAvailable() {
        return checkMultipleHudAvailable();
    }
    
    /**
     * Define a HUD customizada para o jogador
     * Usa MultipleHUD se disponível, senão usa o método padrão
     * @return true se usou MultipleHUD, false se usou método padrão
     */
    public static boolean setCustomHud(Player player, PlayerRef playerRef, CustomUIHud customHud) {
        if (checkMultipleHudAvailable() && multipleHudInstance != null && setCustomHudMethod != null) {
            try {
                setCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_IDENTIFIER, customHud);
                return true; // Usou MultipleHUD com sucesso
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error setting HUD via MultipleHUD - falling back to standard mode");
                // Fallback para método padrão
                try {
                    player.getHudManager().setCustomHud(playerRef, customHud);
                } catch (Exception e2) {
                    LOGGER.atSevere().withCause(e2).log("Failed to set HUD via standard method as fallback");
                }
                return false;
            }
        } else {
            // Usa método padrão se MultipleHUD não estiver disponível
            try {
                player.getHudManager().setCustomHud(playerRef, customHud);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to set HUD via standard method");
            }
            return false;
        }
    }
    
    /**
     * Remove a HUD customizada do jogador
     */
    public static void hideCustomHud(Player player, PlayerRef playerRef) {
        if (checkMultipleHudAvailable() && multipleHudInstance != null && hideCustomHudMethod != null) {
            try {
                hideCustomHudMethod.invoke(multipleHudInstance, player, playerRef, HUD_IDENTIFIER);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error hiding HUD via MultipleHUD: %s - falling back to standard mode", e.getMessage());
                // Fallback para método padrão
                player.getHudManager().setCustomHud(playerRef, null);
            }
        } else {
            // Usa método padrão se MultipleHUD não estiver disponível
            player.getHudManager().setCustomHud(playerRef, null);
        }
    }
}

