package com.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

/**
 * Representa um campo/linha individual da HUD
 * Suporta traduções (*chave*) e placeholders (%placeholder%)
 */
public class HudField {

    public static final BuilderCodec<HudField> CODEC = BuilderCodec.builder(HudField.class, HudField::new)
            .append(new KeyedCodec<String>("Line", Codec.STRING),
                    (field, value, extraInfo) -> field.line = value != null ? value : "",
                    (field, extraInfo) -> field.line).add()
            .append(new KeyedCodec<Boolean>("Visible", Codec.BOOLEAN),
                    (field, value, extraInfo) -> field.visible = value != null ? value : true,
                    (field, extraInfo) -> field.visible).add()
            .build();

    public static ArrayCodec<HudField> CODEC_ARRAY = new ArrayCodec<>(CODEC, HudField[]::new);

    private String line = "";
    private boolean visible = true;

    public HudField() {
        // Construtor padrão
    }

    public HudField(String line, boolean visible) {
        this.line = line != null ? line : "";
        this.visible = visible;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line != null ? line : "";
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}

