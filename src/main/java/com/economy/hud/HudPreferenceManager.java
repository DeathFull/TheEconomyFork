package com.economy.hud;

import com.economy.files.HudPreferenceBlockingFile;
import com.economy.util.FileUtils;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.UUID;
import java.util.logging.Level;

public class HudPreferenceManager {

    private static final HudPreferenceManager INSTANCE = new HudPreferenceManager();
    private HudPreferenceBlockingFile hudPreferenceBlockingFile;
    private HudPreferenceTracker tracker;
    private boolean isDirty = false;
    private Thread savingThread;
    private HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("EconomySystem");

    public static HudPreferenceManager getInstance() {
        return INSTANCE;
    }

    private HudPreferenceManager() {
        this.tracker = new HudPreferenceTracker();
        this.hudPreferenceBlockingFile = new HudPreferenceBlockingFile();
        FileUtils.ensureMainDirectory();

        try {
            FileUtils.ensureFile(FileUtils.HUD_PREFERENCES_PATH, "{\"Preferences\": []}");
            logger.at(Level.INFO).log("Loading HUD preferences from JSON file...");
            this.hudPreferenceBlockingFile.syncLoad();
            this.tracker = this.hudPreferenceBlockingFile.getTracker();
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR LOADING HUD PREFERENCES FILE");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
        }

        startSavingThread();
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
                } catch (Exception e) {
                    logger.at(Level.SEVERE).log("Error in HUD preferences saving thread: " + e.getMessage());
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
            this.hudPreferenceBlockingFile.syncSave();
            this.isDirty = false;
            logger.at(Level.FINE).log("HUD preferences data saved");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("ERROR SAVING HUD PREFERENCES FILE");
            logger.at(Level.SEVERE).log(e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isHudEnabled(UUID uuid) {
        return tracker.isHudEnabled(uuid);
    }

    public void setHudEnabled(UUID uuid, boolean enabled) {
        tracker.setHudEnabled(uuid, enabled);
        markDirty();
        save(); // Salva imediatamente
    }
}

