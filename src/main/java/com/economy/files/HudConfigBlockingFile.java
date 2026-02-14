package com.economy.files;

import com.economy.config.HudConfig;
import com.economy.config.HudField;
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

public class HudConfigBlockingFile extends BlockingDiskFile {

    private HudConfig config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public HudConfigBlockingFile() {
        super(Path.of(FileUtils.MAIN_PATH + java.io.File.separator + "HudConfig.json"));
        this.config = new HudConfig();
    }

    @Override
    protected void read(BufferedReader bufferedReader) throws IOException {
        var rootElement = JsonParser.parseReader(bufferedReader);
        if (rootElement == null || !rootElement.isJsonObject()) {
            // Se o arquivo estiver vazio ou inválido, usa configuração padrão
            this.config = new HudConfig();
            this.config.createDefaultFields();
            // Calcula height padrão
            int visibleFieldsCount = this.config.getVisibleFieldsCount();
            int calculatedHeight = Math.max(85, visibleFieldsCount * 20 + 20);
            this.config.setHeight(calculatedHeight);
            this.config.setWidth(150);
            this.config.setTopPosition(450);
            this.config.setRightPosition(20);
            this.config.setLeftPosition(0);
            return;
        }
        
        var root = rootElement.getAsJsonObject();
        this.config = new HudConfig();
        
        // Verifica se o arquivo está completamente vazio (apenas {})
        boolean isEmpty = root.size() == 0;
        boolean hasHeight = root.has("Height");
        boolean hasWidth = root.has("Width");
        boolean hasFieldsKey = root.has("Fields");
        
        // Se o arquivo estiver completamente vazio, cria configuração padrão
        if (isEmpty) {
            this.config.createDefaultFields();
            int visibleFieldsCount = this.config.getVisibleFieldsCount();
            int calculatedHeight = Math.max(85, visibleFieldsCount * 20 + 20);
            this.config.setHeight(calculatedHeight);
            this.config.setWidth(150);
            this.config.setTopPosition(450);
            this.config.setRightPosition(20);
            this.config.setLeftPosition(0);
            return;
        }
        
        // Lê Height (só define padrão se não existir)
        if (hasHeight) {
            this.config.setHeight(root.get("Height").getAsInt());
        } else {
            // Se não tem height, calcula baseado nos campos que serão lidos
            // Mas não cria campos padrão aqui - só se realmente não tiver campos
        }
        
        // Lê Width (só define padrão se não existir)
        if (hasWidth) {
            this.config.setWidth(root.get("Width").getAsInt());
        } else {
            // Define padrão apenas se não existir
            this.config.setWidth(150);
        }
        
        // Lê TopPosition (só define padrão se não existir)
        if (root.has("TopPosition")) {
            this.config.setTopPosition(root.get("TopPosition").getAsInt());
        } else {
            // Define padrão apenas se não existir
            this.config.setTopPosition(450);
        }
        
        // Lê RightPosition (só define padrão se não existir)
        if (root.has("RightPosition")) {
            this.config.setRightPosition(root.get("RightPosition").getAsInt());
        } else {
            // Define padrão apenas se não existir e LeftPosition também não existir
            if (!root.has("LeftPosition") || root.get("LeftPosition").getAsInt() <= 0) {
                this.config.setRightPosition(20);
            } else {
                this.config.setRightPosition(0);
            }
        }
        
        // Lê LeftPosition (só define padrão se não existir)
        if (root.has("LeftPosition")) {
            this.config.setLeftPosition(root.get("LeftPosition").getAsInt());
        } else {
            // Define padrão (0) apenas se não existir
            this.config.setLeftPosition(0);
        }
        
        // Lê Fields - IMPORTANTE: só cria padrão se realmente não tiver campos válidos
        boolean hasValidFields = false;
        if (hasFieldsKey) {
            JsonArray fieldsArray = root.getAsJsonArray("Fields");
            if (fieldsArray != null && fieldsArray.size() > 0) {
                java.util.List<HudField> fields = new java.util.ArrayList<>();
                fieldsArray.forEach(jsonElement -> {
                    if (jsonElement.isJsonObject()) {
                        JsonObject fieldObj = jsonElement.getAsJsonObject();
                        String line = fieldObj.has("Line") ? fieldObj.get("Line").getAsString() : "";
                        boolean visible = fieldObj.has("Visible") ? fieldObj.get("Visible").getAsBoolean() : true;
                        fields.add(new HudField(line, visible));
                    }
                });
                // Só usa os campos lidos se a lista não estiver vazia
                if (!fields.isEmpty()) {
                    this.config.setFields(fields);
                    hasValidFields = true;
                }
            }
        }
        
        // Só cria campos padrão se realmente não tiver campos válidos
        // E só se o arquivo não tiver a chave Fields ou tiver mas estiver vazia
        if (!hasValidFields) {
            // Se não tinha a chave Fields ou tinha mas estava vazia, cria padrão
            this.config.createDefaultFields();
            // Se não tinha height, calcula baseado nos campos padrão
            if (!hasHeight || this.config.getHeight() == 0) {
                int visibleFieldsCount = this.config.getVisibleFieldsCount();
                int calculatedHeight = Math.max(85, visibleFieldsCount * 20 + 20);
                this.config.setHeight(calculatedHeight);
            }
        } else {
            // Se tinha campos válidos, só calcula height se não existir
            if (!hasHeight || this.config.getHeight() == 0) {
                int visibleFieldsCount = this.config.getVisibleFieldsCount();
                int calculatedHeight = Math.max(85, visibleFieldsCount * 20 + 20);
                this.config.setHeight(calculatedHeight);
            }
        }
    }

    @Override
    protected void write(BufferedWriter bufferedWriter) throws IOException {
        // Garante que o config exista
        if (this.config == null) {
            this.config = new HudConfig();
        }
        
        // NÃO cria campos padrão aqui - preserva o que já existe
        // Só cria se realmente estiver vazio (arquivo novo)
        if (this.config.getFields() == null || this.config.getFields().isEmpty()) {
            this.config.createDefaultFields();
        }
        
        JsonObject root = new JsonObject();
        root.addProperty("Height", this.config.getHeight() > 0 ? this.config.getHeight() : 150);
        root.addProperty("Width", this.config.getWidth() > 0 ? this.config.getWidth() : 150);
        root.addProperty("TopPosition", this.config.getTopPosition() > 0 ? this.config.getTopPosition() : 450);
        root.addProperty("RightPosition", this.config.getRightPosition() >= 0 ? this.config.getRightPosition() : 20);
        root.addProperty("LeftPosition", this.config.getLeftPosition() >= 0 ? this.config.getLeftPosition() : 0);
        
        JsonArray fieldsArray = new JsonArray();
        java.util.List<HudField> fields = this.config.getFields();
        if (fields != null && !fields.isEmpty()) {
            for (HudField field : fields) {
                if (field != null) {
                    JsonObject fieldObj = new JsonObject();
                    fieldObj.addProperty("Line", field.getLine() != null ? field.getLine() : "");
                    fieldObj.addProperty("Visible", field.isVisible());
                    fieldsArray.add(fieldObj);
                }
            }
        }
        root.add("Fields", fieldsArray);
        
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    @Override
    protected void create(BufferedWriter bufferedWriter) throws IOException {
        // Cria configuração padrão
        this.config = new HudConfig();
        this.config.createDefaultFields();
        
        // Calcula height padrão baseado nos campos visíveis
        if (this.config.getHeight() == 0) {
            int visibleFieldsCount = this.config.getVisibleFieldsCount();
            // Altura base + altura por campo (aproximadamente 20px por campo) + padding
            int calculatedHeight = Math.max(85, visibleFieldsCount * 20 + 20);
            this.config.setHeight(calculatedHeight);
        }
        
        // Define width padrão se não estiver definido
        if (this.config.getWidth() == 0) {
            this.config.setWidth(150);
        }
        
        // Define topPosition padrão se não estiver definido
        if (this.config.getTopPosition() <= 0) {
            this.config.setTopPosition(450);
        }
        
        // Define rightPosition padrão se não estiver definido e leftPosition também não
        if (this.config.getRightPosition() <= 0 && this.config.getLeftPosition() <= 0) {
            this.config.setRightPosition(20);
        }
        
        // Define leftPosition padrão se não estiver definido
        if (this.config.getLeftPosition() < 0) {
            this.config.setLeftPosition(0);
        }
        
        JsonObject root = new JsonObject();
        root.addProperty("Height", this.config.getHeight());
        root.addProperty("Width", this.config.getWidth());
        root.addProperty("TopPosition", this.config.getTopPosition());
        root.addProperty("RightPosition", this.config.getRightPosition());
        root.addProperty("LeftPosition", this.config.getLeftPosition());
        
        JsonArray fieldsArray = new JsonArray();
        for (HudField field : this.config.getFields()) {
            JsonObject fieldObj = new JsonObject();
            fieldObj.addProperty("Line", field.getLine());
            fieldObj.addProperty("Visible", field.isVisible());
            fieldsArray.add(fieldObj);
        }
        root.add("Fields", fieldsArray);
        
        String jsonString = GSON.toJson(root);
        bufferedWriter.write(jsonString);
    }

    public HudConfig getConfig() {
        return config;
    }

    public void setConfig(HudConfig config) {
        // IMPORTANTE: Cria uma cópia dos campos para evitar referências compartilhadas
        // que podem causar problemas quando o config é modificado
        if (config != null) {
            this.config = new HudConfig();
            this.config.setHeight(config.getHeight());
            this.config.setWidth(config.getWidth());
            this.config.setTopPosition(config.getTopPosition());
            this.config.setRightPosition(config.getRightPosition());
            this.config.setLeftPosition(config.getLeftPosition());
            
            // Copia os campos (cria nova lista para evitar referência compartilhada)
            java.util.List<HudField> fieldsCopy = new java.util.ArrayList<>();
            if (config.getFields() != null) {
                for (HudField field : config.getFields()) {
                    if (field != null) {
                        fieldsCopy.add(new HudField(field.getLine(), field.isVisible()));
                    }
                }
            }
            if (fieldsCopy.isEmpty()) {
                this.config.createDefaultFields();
            } else {
                this.config.setFields(fieldsCopy);
            }
        } else {
            this.config = new HudConfig();
            if (this.config.getFields().isEmpty()) {
                this.config.createDefaultFields();
            }
        }
    }
}


