package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** /tpaccept [player] — accept an incoming TPA request */
public final class TpAcceptCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public TpAcceptCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player target)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        if (args.length >= 1) {
            // Accept from specific player
            Player from = Bukkit.getPlayerExact(args[0]);
            if (from == null) {
                plugin.getMessageManager().send(target, "player-not-found",
                        Map.of("{player}", args[0]));
                return true;
            }
            boolean accepted = plugin.getTpaManager().accept(target, from.getUniqueId());
            if (!accepted) {
                plugin.getMessageManager().send(target, "tpa-no-pending-from",
                        Map.of("{player}", from.getName()));
            }
        } else {
            // Accept most recent
            boolean accepted = plugin.getTpaManager().acceptMostRecent(target);
            if (!accepted) {
                plugin.getMessageManager().send(target, "tpa-no-pending");
            }
        }
        return true;
    }
}
