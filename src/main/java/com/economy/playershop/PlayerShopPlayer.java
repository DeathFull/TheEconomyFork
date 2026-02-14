package com.economy.playershop;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;

import java.util.UUID;

public class PlayerShopPlayer {

    public static final BuilderCodec<PlayerShopPlayer> CODEC = BuilderCodec.builder(PlayerShopPlayer.class, PlayerShopPlayer::new)
            .append(new KeyedCodec<>("Uuid", Codec.STRING),
                    (player, value, extraInfo) -> {
                        if (value != null && !value.isEmpty()) {
                            try {
                                player.uuid = UUID.fromString(value);
                            } catch (Exception e) {
                                player.uuid = null;
                            }
                        } else {
                            player.uuid = null;
                        }
                    },
                    (player, extraInfo) -> player.uuid != null ? player.uuid.toString() : "").add()
            .append(new KeyedCodec<>("Nick", Codec.STRING),
                    (player, value, extraInfo) -> player.nick = value != null ? value : "",
                    (player, extraInfo) -> player.nick).add()
            .append(new KeyedCodec<>("CustomName", Codec.STRING),
                    (player, value, extraInfo) -> player.customName = value != null ? value : "",
                    (player, extraInfo) -> player.customName != null ? player.customName : "").add()
            .append(new KeyedCodec<>("ShopIcon", Codec.STRING),
                    (player, value, extraInfo) -> player.shopIcon = value != null ? value : "",
                    (player, extraInfo) -> player.shopIcon != null ? player.shopIcon : "").add()
            .build();

    private UUID uuid;
    private String nick;
    private String customName;
    private String shopIcon;

    public PlayerShopPlayer() {
        this.uuid = null;
        this.nick = "";
        this.customName = "";
        this.shopIcon = "";
    }

    public PlayerShopPlayer(UUID uuid, String nick) {
        this.uuid = uuid;
        this.nick = nick;
        this.customName = "";
        this.shopIcon = "";
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getCustomName() {
        return customName != null ? customName : "";
    }

    public void setCustomName(String customName) {
        this.customName = customName != null ? customName : "";
    }
    
    public String getShopIcon() {
        return shopIcon != null ? shopIcon : "";
    }
    
    public void setShopIcon(String shopIcon) {
        this.shopIcon = shopIcon != null ? shopIcon : "";
    }
}

