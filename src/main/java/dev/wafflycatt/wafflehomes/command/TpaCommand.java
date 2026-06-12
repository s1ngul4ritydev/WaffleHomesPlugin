package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.manager.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** /tpa <player> — request to teleport TO target */
public final class TpaCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public TpaCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player from)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        if (!plugin.getConfigManager().isTpaEnabled()) {
            plugin.getMessageManager().send(from, "tpa-disabled");
            return true;
        }

        if (args.length < 1) return false;

        if (args[0].equalsIgnoreCase(from.getName())) {
            plugin.getMessageManager().send(from, "tpa-self");
            return true;
        }

        Player to = Bukkit.getPlayerExact(args[0]);
        if (to == null) {
            plugin.getMessageManager().send(from, "player-not-found", Map.of("{player}", args[0]));
            return true;
        }

        int cd = plugin.getTpaManager().getCooldownRemaining(from.getUniqueId());
        if (cd > 0) {
            plugin.getMessageManager().send(from, "tpa-cooldown", Map.of("{time}", String.valueOf(cd)));
            return true;
        }

        if (plugin.getTpaManager().hasPendingRequest(from.getUniqueId(), to.getUniqueId())) {
            plugin.getMessageManager().send(from, "tpa-already-pending", Map.of("{player}", to.getName()));
            return true;
        }

        plugin.getTpaManager().sendRequest(from, to, false)
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
                    switch (result) {
                        case SENT ->
                            plugin.getMessageManager().send(from, "tpa-sent", Map.of("{player}", to.getName()));
                        case AUTO_ACCEPTED ->
                            plugin.getMessageManager().send(from, "tpa-sent", Map.of("{player}", to.getName()));
                        case TARGET_BLOCKED ->
                            plugin.getMessageManager().send(from, "tpa-target-no-tpa", Map.of("{player}", to.getName()));
                    }
                }));
        return true;
    }
}
