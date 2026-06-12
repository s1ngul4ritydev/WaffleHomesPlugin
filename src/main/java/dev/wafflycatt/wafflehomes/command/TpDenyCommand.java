package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** /tpdeny [player] — deny an incoming TPA request */
public final class TpDenyCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public TpDenyCommand(WaffleHomes plugin) {
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
            Player from = Bukkit.getPlayerExact(args[0]);
            if (from == null) {
                plugin.getMessageManager().send(target, "player-not-found",
                        Map.of("{player}", args[0]));
                return true;
            }
            boolean denied = plugin.getTpaManager().deny(target, from.getUniqueId());
            if (!denied) {
                plugin.getMessageManager().send(target, "tpa-no-pending-from",
                        Map.of("{player}", from.getName()));
            }
        } else {
            boolean denied = plugin.getTpaManager().denyMostRecent(target);
            if (!denied) {
                plugin.getMessageManager().send(target, "tpa-no-pending");
            }
        }
        return true;
    }
}
