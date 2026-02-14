package com.economy.files;

import com.economy.hud.HudPreferenceTracker;
import com.economy.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class HudPreferenceBlockingFile extends BlockingDiskFile {

    private HudPreferenceTracker tracker;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public HudPreferenceBlockingFile() {
        super(Path.of(FileUtils.HUD_PREFERENCES_PATH));
        this.tracker = new HudPreferenceTracker();
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        var rootElement = JsonParser.parseReader(bufferedReader);
        if (rootElement == null || !rootElement.isJsonObject()) return;
        var root = rootElement.getAsJsonObject();
        JsonArray valuesArray = root.getAsJsonArray("Preferences");
        if (valuesArray == null) return;
        this.tracker = new HudPreferenceTracker();
        valuesArray.forEach(jsonElement -> {
            JsonObject prefObj = jsonElement.getAsJsonObject();
            UUID uuid = UUID.fromString(prefObj.get("UUID").getAsString());
            boolean enabled = prefObj.has("Enabled") ? prefObj.get("Enabled").getAsBoolean() : true;
            this.tracker.setHudEnabled(uuid, enabled);
        });
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray valuesArray = new JsonArray();
        for (java.util.Map.Entry<UUID, Boolean> entry : this.tracker.getPreferences().entrySet()) {
            JsonObject prefObj = new JsonObject();
            prefObj.addProperty("UUID", entry.getKey().toString());
            prefObj.addProperty("Enabled", entry.getValue());
            valuesArray.add(prefObj);
        }
        root.add("Preferences", valuesArray);
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        this.tracker = new HudPreferenceTracker();
        write(bufferedWriter);
    }

    public HudPreferenceTracker getTracker() {
        return tracker;
    }
}

