package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.form.TpSettingsForm;
import dev.wafflycatt.wafflehomes.gui.TpSettingsGui;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /tpsettings — open teleport settings.
 * Java: inventory GUI. Bedrock: Floodgate CustomForm.
 */
public final class TpSettingsCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public TpSettingsCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        plugin.getDatabaseManager().getSettings(player.getUniqueId())
                .thenAccept(settings -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (FloodgateUtil.isBedrockPlayer(player)) {
                        TpSettingsForm.show(plugin, player, settings);
                    } else {
                        String title = plugin.getMessageManager().getRaw("gui-tpsettings-title");
                        new TpSettingsGui(player, settings, title).open();
                    }
                }));
        return true;
    }
}
