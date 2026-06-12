package dev.wafflycatt.wafflehomes.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Null-safe wrapper around the Floodgate API.
 * All methods are safe to call even when Floodgate is not installed.
 */
public final class FloodgateUtil {

    private FloodgateUtil() {}

    /** Returns true if the Floodgate plugin is present and enabled. */
    public static boolean isFloodgateAvailable() {
        return Bukkit.getPluginManager().getPlugin("Floodgate") != null;
    }

    /** Returns true if the given player is a Bedrock player connected via Floodgate/Geyser. */
    public static boolean isBedrockPlayer(Player player) {
        if (!isFloodgateAvailable()) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Returns true if the given UUID belongs to a Bedrock player. */
    public static boolean isBedrockUuid(UUID uuid) {
        if (!isFloodgateAvailable()) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Sends a Floodgate form to a Bedrock player.
     * Does nothing if Floodgate is unavailable or the player is not Bedrock.
     */
    public static void sendForm(Player player, Form form) {
        if (!isFloodgateAvailable()) return;
        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (Exception ignored) {}
    }
}
