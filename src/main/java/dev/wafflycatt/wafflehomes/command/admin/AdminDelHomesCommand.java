package dev.wafflycatt.wafflehomes.command.admin;

import dev.wafflycatt.wafflehomes.WaffleHomes;
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
 * /admindelhomes <player> <home> — delete any player's home.
 * Requires wafflehomes.admin permission OR being listed in config admins.
 */
public final class AdminDelHomesCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public AdminDelHomesCommand(WaffleHomes plugin) {
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

        if (args.length < 2) return false;

        String targetName = args[0];
        String homeName   = args[1];

        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) {
            plugin.getMessageManager().send(admin, "player-not-found",
                    Map.of("{player}", targetName));
            return true;
        }

        plugin.getHomeManager().deleteHome(targetUuid, homeName)
                .thenAccept(deleted -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (deleted) {
                        plugin.getMessageManager().send(admin, "admin-home-deleted",
                                Map.of("{home}", homeName, "{player}", targetName));
                    } else {
                        plugin.getMessageManager().send(admin, "home-not-found",
                                Map.of("{name}", homeName));
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
