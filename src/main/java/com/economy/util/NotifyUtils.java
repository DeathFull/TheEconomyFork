package com.economy.util;

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;


/**
 * Utility class for sending notifications to players.
 */
public class NotifyUtils {

    /**
     * Sends a notification to a player with the specified message and style.
     *
     * @param playerRef The player reference
     * @param message   The message to send
     * @param style     The notification style
     */
    public static void sendNotification(@Nonnull PlayerRef playerRef, @Nonnull String message, @Nonnull NotificationStyle style) {
        NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                MessageFormatter.format(message),
                style
        );
    }

    /**
     * Sends a default notification to a player.
     *
     * @param playerRef The player reference
     * @param message   The message to send
     */
    public static void sendNotification(@Nonnull PlayerRef playerRef, @Nonnull String message) {
        sendNotification(playerRef, message, NotificationStyle.Default);
    }

    /**
     * Sends a success notification to a player.
     *
     * @param playerRef The player reference
     * @param message   The message to send
     */
    public static void sendSuccessNotification(@Nonnull PlayerRef playerRef, @Nonnull String message) {
        sendNotification(playerRef, message, NotificationStyle.Success);
    }

    /**
     * Sends a warning notification to a player.
     *
     * @param playerRef The player reference
     * @param message   The message to send
     */
    public static void sendWarningNotification(@Nonnull PlayerRef playerRef, @Nonnull String message) {
        sendNotification(playerRef, message, NotificationStyle.Warning);
    }

    /**
     * Sends a danger notification to a player.
     *
     * @param playerRef The player reference
     * @param message   The message to send
     */
    public static void sendDangerNotification(@Nonnull PlayerRef playerRef, @Nonnull String message) {
        sendNotification(playerRef, message, NotificationStyle.Danger);
    }

    /**
     * Shows an event title to a player.
     *
     * @param playerRef      The player reference
     * @param primaryTitle   The primary title
     * @param secondaryTitle The secondary title
     * @param isMajor        Whether this is a major event
     */
    public static void showEventTitle(@Nonnull PlayerRef playerRef, @Nonnull String primaryTitle, @Nonnull String secondaryTitle, boolean isMajor) {
        java.util.UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid != null) {
            World world = Universe.get().getWorld(worldUuid);
            if (world != null && world.isAlive()) {
                world.execute(() -> EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        Message.raw(primaryTitle),
                        Message.raw(secondaryTitle),
                        isMajor
                ));
                return;
            }
        }
        
        try {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw(primaryTitle),
                    Message.raw(secondaryTitle),
                    isMajor
            );
        } catch (Exception e) {
            sendNotification(playerRef, primaryTitle + " - " + secondaryTitle, NotificationStyle.Success);
        }
    }

    /**
     * Shows a level up title to a player.
     *
     * @param playerRef The player reference
     * @param newLevel  The new level
     */
    public static void showLevelUpTitle(@Nonnull PlayerRef playerRef, int newLevel) {
        showEventTitle(playerRef, "LEVEL UP!", "You are now level " + newLevel + "!", true);
    }

    /**
     * Shows a max level title to a player.
     *
     * @param playerRef The player reference
     * @param maxLevel  The maximum level
     */
    public static void showMaxLevelTitle(@Nonnull PlayerRef playerRef, int maxLevel) {
        showEventTitle(playerRef, "CONGRATULATIONS!", "You have reached the maximum level " + maxLevel + "!", true);
    }
}

