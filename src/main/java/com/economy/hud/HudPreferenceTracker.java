package com.economy.hud;

import java.util.HashMap;
import java.util.UUID;

public class HudPreferenceTracker {

    private HashMap<UUID, Boolean> preferences;

    public HudPreferenceTracker() {
        this.preferences = new HashMap<>();
    }

    public boolean isHudEnabled(UUID uuid) {
        // Por padrão, a HUD está habilitada se não houver preferência salva
        return preferences.getOrDefault(uuid, true);
    }

    public void setHudEnabled(UUID uuid, boolean enabled) {
        preferences.put(uuid, enabled);
    }

    public HashMap<UUID, Boolean> getPreferences() {
        return preferences;
    }

    public void setPreferences(HashMap<UUID, Boolean> preferences) {
        this.preferences = preferences != null ? new HashMap<>(preferences) : new HashMap<>();
    }
}

