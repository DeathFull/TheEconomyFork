package com.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class OreRewardEntry {

    public static final BuilderCodec<OreRewardEntry> CODEC = BuilderCodec.builder(OreRewardEntry.class, OreRewardEntry::new)
            .append(new KeyedCodec<>("OreName", Codec.STRING),
                    (entry, name, extraInfo) -> entry.oreName = name,
                    (entry, extraInfo) -> entry.oreName).add()
            .append(new KeyedCodec<>("Reward", Codec.DOUBLE),
                    (entry, reward, extraInfo) -> entry.reward = reward,
                    (entry, extraInfo) -> entry.reward).add()
            .build();

    public static ArrayCodec<OreRewardEntry> CODEC_ARRAY = new ArrayCodec<>(CODEC, OreRewardEntry[]::new);

    private String oreName;
    private double reward;

    public OreRewardEntry() {
        this("", 0.0);
    }

    public OreRewardEntry(String oreName, double reward) {
        this.oreName = oreName;
        this.reward = reward;
    }

    public String getOreName() {
        return oreName;
    }

    public double getReward() {
        return reward;
    }
}

