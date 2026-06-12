package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.form.HomesForm;
import dev.wafflycatt.wafflehomes.gui.HomesGui;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * /homes — opens the homes menu.
 * Java players: inventory GUI.
 * Bedrock players: Floodgate SimpleForm.
 */
public final class HomesCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public HomesCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        plugin.getHomeManager().getHomes(player.getUniqueId())
                .thenAccept(homes -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (FloodgateUtil.isBedrockPlayer(player)) {
                        HomesForm.showHomesForm(plugin, player, homes, false, player.getName());
                    } else {
                        new HomesGui(plugin, player, homes, 0).open();
                    }
                }));
        return true;
    }
}
