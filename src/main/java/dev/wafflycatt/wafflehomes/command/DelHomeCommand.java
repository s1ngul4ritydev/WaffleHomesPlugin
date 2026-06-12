package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class DelHomeCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public DelHomeCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        if (args.length < 1) return false;

        String name = args[0];
        plugin.getHomeManager().deleteHome(player.getUniqueId(), name)
                .thenAccept(deleted -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (deleted) {
                        plugin.getMessageManager().send(player, "home-deleted",
                                Map.of("{name}", name));
                    } else {
                        plugin.getMessageManager().send(player, "home-not-found",
                                Map.of("{name}", name));
                    }
                }));
        return true;
    }
}
