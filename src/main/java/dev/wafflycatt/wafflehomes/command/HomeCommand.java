package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * /home <name>
 *
 * Starts the teleport warmup countdown for the player.
 * If the player stands still for the configured duration, they are teleported.
 * If they move, the countdown is cancelled.
 */
public final class HomeCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public HomeCommand(WaffleHomes plugin) {
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
        plugin.getHomeManager().getHome(player.getUniqueId(), name)
                .thenAccept(opt -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (opt.isEmpty()) {
                        plugin.getMessageManager().send(player, "home-not-found",
                                Map.of("{name}", name));
                        return;
                    }
                    var home  = opt.get();
                    var world = Bukkit.getWorld(home.worldName());
                    if (world == null) {
                        plugin.getMessageManager().send(player, "home-not-found",
                                Map.of("{name}", name));
                        return;
                    }

                    plugin.getMessageManager().send(player, "home-teleporting",
                            Map.of("{name}", home.name()));

                    Location dest = new Location(world, home.x(), home.y(), home.z(),
                            home.yaw(), home.pitch());

                    // Start 8-second stand-still warmup; teleport on completion
                    plugin.getCountdownManager().start(player, () ->
                            player.teleportAsync(dest, PlayerTeleportEvent.TeleportCause.PLUGIN));
                }));
        return true;
    }
}
