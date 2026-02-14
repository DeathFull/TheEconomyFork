package com.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração da HUD personalizável via JSON
 * Suporta adicionar/remover linhas dinamicamente
 */
public class HudConfig {

    public static final BuilderCodec<HudConfig> CODEC = BuilderCodec.builder(HudConfig.class, HudConfig::new)
            .append(new KeyedCodec<Integer>("Height", Codec.INTEGER),
                    (config, value, extraInfo) -> config.height = value != null && value > 0 ? value : 0,
                    (config, extraInfo) -> config.height).add()
            .append(new KeyedCodec<Integer>("Width", Codec.INTEGER),
                    (config, value, extraInfo) -> config.width = value != null && value > 0 ? value : 0,
                    (config, extraInfo) -> config.width).add()
            .append(new KeyedCodec<Integer>("TopPosition", Codec.INTEGER),
                    (config, value, extraInfo) -> config.topPosition = value != null && value >= 0 ? value : 450,
                    (config, extraInfo) -> config.topPosition).add()
            .append(new KeyedCodec<Integer>("RightPosition", Codec.INTEGER),
                    (config, value, extraInfo) -> config.rightPosition = value != null && value >= 0 ? value : 20,
                    (config, extraInfo) -> config.rightPosition).add()
            .append(new KeyedCodec<Integer>("LeftPosition", Codec.INTEGER),
                    (config, value, extraInfo) -> config.leftPosition = value != null && value >= 0 ? value : 0,
                    (config, extraInfo) -> config.leftPosition).add()
            .append(new KeyedCodec<HudField[]>("Fields", HudField.CODEC_ARRAY),
                    (config, value, extraInfo) -> {
                        // Garante que a lista exista
                        if (config.fields == null) {
                            config.fields = new ArrayList<>();
                        }
                        
                        if (value != null && value.length > 0) {
                            // Limpa a lista antes de adicionar novos campos
                            config.fields.clear();
                            for (HudField field : value) {
                                if (field != null) {
                                    config.fields.add(field);
                                }
                            }
                            // Se após adicionar os campos a lista estiver vazia, cria os padrões
                            if (config.fields.isEmpty()) {
                                config.createDefaultFields();
                            }
                        } else {
                            // Se não houver campos definidos (null ou array vazio), cria os padrões
                            config.createDefaultFields();
                        }
                    },
                    (config, extraInfo) -> {
                        // Garante que os campos padrão existam antes de serializar
                        if (config.fields == null) {
                            config.fields = new ArrayList<>();
                        }
                        if (config.fields.isEmpty()) {
                            config.createDefaultFields();
                        }
                        return config.fields.toArray(new HudField[0]);
                    }).add()
            .build();

    private int height = 0; // 0 = calcular automaticamente
    private int width = 0; // 0 = usar valor padrão (150)
    private int topPosition = 450; // Posição vertical (Top) da HUD, padrão 450
    private int rightPosition = 20; // Posição horizontal (Right) da HUD, padrão 20. Se 0, usa LeftPosition
    private int leftPosition = 0; // Posição horizontal (Left) da HUD, padrão 0. Usado quando RightPosition = 0
    private List<HudField> fields = new ArrayList<>();

    public HudConfig() {
        // Cria campos padrão se não houver nenhum
        createDefaultFields();
    }

    /**
     * Cria os campos padrão da HUD
     * Se a lista estiver vazia, adiciona os campos padrão
     */
    public void createDefaultFields() {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        // Só cria os campos padrão se a lista estiver vazia
        if (fields.isEmpty()) {
            fields.add(new HudField("&f=-= &2The Economy &f=-=", true));
            fields.add(new HudField("*hud_nick* %player_name%", true));
            fields.add(new HudField("", true));
            fields.add(new HudField("*hud_money* %balance%", true));
            fields.add(new HudField("*hud_cash* %cash%", true));
            fields.add(new HudField("*hud_top_rank* %player_rank%", true));
            fields.add(new HudField("", true));
            fields.add(new HudField("*hud_shop_status*: %player_shop%", true));
        }
    }
    
    /**
     * Força a recriação dos campos padrão (limpa a lista e adiciona os padrões)
     */
    public void resetToDefaultFields() {
        if (fields == null) {
            fields = new ArrayList<>();
        } else {
            fields.clear();
        }
        fields.add(new HudField("*hud_nick*: %player_name%", true));
        fields.add(new HudField("", true));
        fields.add(new HudField("*hud_money*: %balance%", true));
        fields.add(new HudField("*hud_cash*: %cash%", true));
        fields.add(new HudField("*hud_top_rank*: %player_rank%", true));
        fields.add(new HudField("", true));
        fields.add(new HudField("*hud_shop_status*: %player_shop%", true));
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height > 0 ? height : 0;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width > 0 ? width : 0;
    }

    public int getTopPosition() {
        return topPosition;
    }

    public void setTopPosition(int topPosition) {
        this.topPosition = topPosition >= 0 ? topPosition : 450;
    }

    public int getRightPosition() {
        return rightPosition;
    }

    public void setRightPosition(int rightPosition) {
        this.rightPosition = rightPosition >= 0 ? rightPosition : 20;
    }

    public int getLeftPosition() {
        return leftPosition;
    }

    public void setLeftPosition(int leftPosition) {
        this.leftPosition = leftPosition >= 0 ? leftPosition : 0;
    }

    public List<HudField> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        if (fields.isEmpty()) {
            createDefaultFields();
        }
        // Garantia final: nunca retorna null
        if (fields == null) {
            fields = new ArrayList<>();
            createDefaultFields();
        }
        return fields;
    }

    public void setFields(List<HudField> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
        if (this.fields.isEmpty()) {
            createDefaultFields();
        }
    }

    /**
     * Adiciona um novo campo à HUD
     */
    public void addField(HudField field) {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        if (field != null) {
            fields.add(field);
        }
    }

    /**
     * Remove um campo da HUD pelo índice
     */
    public void removeField(int index) {
        if (fields != null && index >= 0 && index < fields.size()) {
            fields.remove(index);
        }
    }

    /**
     * Obtém o número de campos visíveis
     */
    public int getVisibleFieldsCount() {
        if (fields == null || fields.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (HudField field : fields) {
            if (field.isVisible()) {
                count++;
            }
        }
        return count;
    }
}

