package dev.wafflycatt.wafflehomes.util;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Checks whether a player has WaffleHomes admin access.
 * Access is granted if the player's name appears in config.yml under "admins"
 * (case-insensitive) OR if the player has the wafflehomes.admin permission node.
 */
public final class AdminUtil {

    private AdminUtil() {}

    /**
     * Returns true if the player is in the config admin list or has the admin permission.
     * Compatible with both Java Edition and Bedrock Edition (Floodgate) players.
     */
    public static boolean isAdmin(Player player) {
        if (player.hasPermission("wafflehomes.admin")) return true;
        List<String> admins = WaffleHomes.getInstance().getConfigManager().getAdmins();
        String name = player.getName();
        for (String admin : admins) {
            if (admin.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
