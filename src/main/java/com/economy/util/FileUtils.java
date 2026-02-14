package com.economy.util;

import com.hypixel.hytale.server.core.Constants;

import java.io.File;

public class FileUtils {

    public static String MAIN_PATH = Constants.UNIVERSE_PATH.resolve("EconomySystem").toAbsolutePath().toString();
    public static String BALANCES_PATH = MAIN_PATH + File.separator + "Balances.json";
    public static String SHOP_PATH = MAIN_PATH + File.separator + "Shop.json";
    public static String PLAYER_SHOP_PATH = MAIN_PATH + File.separator + "PlayerShop.json";
    public static String HUD_PREFERENCES_PATH = MAIN_PATH + File.separator + "HudPreferences.json";

    public static void ensureDirectory(String path){
        var file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void ensureMainDirectory(){
        ensureDirectory(MAIN_PATH);
    }

    public static File ensureFile(String path, String defaultContent){
        var file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
                var writer = new java.io.FileWriter(file);
                writer.write(defaultContent);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}


