package com.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class MonsterRewardEntry {

    public static final BuilderCodec<MonsterRewardEntry> CODEC = BuilderCodec.builder(MonsterRewardEntry.class, MonsterRewardEntry::new)
            .append(new KeyedCodec<>("MonsterId", Codec.STRING),
                    (entry, id, extraInfo) -> entry.monsterId = id,
                    (entry, extraInfo) -> entry.monsterId).add()
            .append(new KeyedCodec<>("Reward", Codec.DOUBLE),
                    (entry, reward, extraInfo) -> entry.reward = reward,
                    (entry, extraInfo) -> entry.reward).add()
            .build();

    public static ArrayCodec<MonsterRewardEntry> CODEC_ARRAY = new ArrayCodec<>(CODEC, MonsterRewardEntry[]::new);

    private String monsterId;
    private double reward;

    public MonsterRewardEntry() {
        this("", 0.0);
    }

    public MonsterRewardEntry(String monsterId, double reward) {
        this.monsterId = monsterId;
        this.reward = reward;
    }

    public String getMonsterId() {
        return monsterId;
    }

    public double getReward() {
        return reward;
    }
}

