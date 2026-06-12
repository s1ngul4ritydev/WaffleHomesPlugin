package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.manager.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class SetHomeCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public SetHomeCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        if (args.length < 1) {
            return false; // Bukkit shows the usage string from plugin.yml
        }

        String name = args[0];
        if (!HomeManager.isValidName(name)) {
            plugin.getMessageManager().send(player, "home-invalid-name");
            return true;
        }

        var loc = player.getLocation();
        plugin.getHomeManager().setHome(
                player.getUniqueId(), name,
                player.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        ).thenAccept(result -> {
            switch (result) {
                case CREATED -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getMessageManager().send(player, "home-set",
                                Map.of("{name}", name)));
                case UPDATED -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getMessageManager().send(player, "home-set-overwrite",
                                Map.of("{name}", name)));
                case LIMIT_REACHED -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getMessageManager().send(player, "home-limit-reached",
                                Map.of("{limit}",
                                        String.valueOf(plugin.getConfigManager().getHomesLimit()))));
                case ERROR -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getMessageManager().send(player, "home-not-found",
                                Map.of("{name}", name)));
            }
        });
        return true;
    }
}
