package com.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class WoodRewardEntry {

    public static final BuilderCodec<WoodRewardEntry> CODEC = BuilderCodec.builder(WoodRewardEntry.class, WoodRewardEntry::new)
            .append(new KeyedCodec<>("WoodName", Codec.STRING),
                    (entry, name, extraInfo) -> entry.woodName = name,
                    (entry, extraInfo) -> entry.woodName).add()
            .append(new KeyedCodec<>("Reward", Codec.DOUBLE),
                    (entry, reward, extraInfo) -> entry.reward = reward,
                    (entry, extraInfo) -> entry.reward).add()
            .build();

    public static ArrayCodec<WoodRewardEntry> CODEC_ARRAY = new ArrayCodec<>(CODEC, WoodRewardEntry[]::new);

    private String woodName;
    private double reward;

    public WoodRewardEntry() {
        this("", 0.0);
    }

    public WoodRewardEntry(String woodName, double reward) {
        this.woodName = woodName;
        this.reward = reward;
    }

    public String getWoodName() {
        return woodName;
    }

    public double getReward() {
        return reward;
    }
}

