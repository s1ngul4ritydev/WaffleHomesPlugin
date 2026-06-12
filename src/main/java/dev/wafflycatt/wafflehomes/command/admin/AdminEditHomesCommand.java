package dev.wafflycatt.wafflehomes.command.admin;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.manager.HomeManager;
import dev.wafflycatt.wafflehomes.util.AdminUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * /adminedithomes <player> <home> <newname> — rename any player's home.
 * Requires wafflehomes.admin permission OR being listed in config admins.
 */
public final class AdminEditHomesCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public AdminEditHomesCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        if (!AdminUtil.isAdmin(admin)) {
            plugin.getMessageManager().send(admin, "no-permission");
            return true;
        }

        if (args.length < 3) return false;

        String targetName = args[0];
        String oldName    = args[1];
        String newName    = args[2];

        if (!HomeManager.isValidName(newName)) {
            plugin.getMessageManager().send(admin, "home-invalid-name");
            return true;
        }

        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) {
            plugin.getMessageManager().send(admin, "player-not-found",
                    Map.of("{player}", targetName));
            return true;
        }

        plugin.getHomeManager().renameHome(targetUuid, oldName, newName)
                .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        plugin.getMessageManager().send(admin, "admin-home-renamed",
                                Map.of("{old}", oldName, "{new}", newName, "{player}", targetName));
                    } else {
                        plugin.getMessageManager().send(admin, "home-not-found",
                                Map.of("{name}", oldName));
                    }
                }));
        return true;
    }

    @SuppressWarnings("deprecation")
    private UUID resolveUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        return offline != null ? offline.getUniqueId() : null;
    }
}
