package dev.wafflycatt.wafflehomes.command.admin;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.form.HomesForm;
import dev.wafflycatt.wafflehomes.gui.HomesGui;
import dev.wafflycatt.wafflehomes.util.AdminUtil;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
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
 * /adminhomes <player> — view any player's homes.
 * Requires wafflehomes.admin permission OR being listed in config admins.
 */
public final class AdminHomesCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public AdminHomesCommand(WaffleHomes plugin) {
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

        if (args.length < 1) return false;

        String targetName = args[0];

        // Resolve UUID — check online first, then offline
        UUID targetUuid = resolveUuid(targetName);
        if (targetUuid == null) {
            plugin.getMessageManager().send(admin, "player-not-found",
                    Map.of("{player}", targetName));
            return true;
        }

        plugin.getHomeManager().getHomes(targetUuid)
                .thenAccept(homes -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (homes.isEmpty()) {
                        plugin.getMessageManager().send(admin, "admin-no-homes",
                                Map.of("{player}", targetName));
                        return;
                    }
                    if (FloodgateUtil.isBedrockPlayer(admin)) {
                        HomesForm.showHomesForm(plugin, admin, homes, true, targetName);
                    } else {
                        new HomesGui(plugin, admin, homes, 0).open();
                    }
                }));
        return true;
    }

    @SuppressWarnings("deprecation")
    private UUID resolveUuid(String name) {
        // Check online players first
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        // Fall back to offline player lookup (cached)
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        return offline != null ? offline.getUniqueId() : null;
    }
}
