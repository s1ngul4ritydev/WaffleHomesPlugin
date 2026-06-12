package dev.wafflycatt.wafflehomes.command;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * /rtp
 *
 * Execution order:
 *  1. Check enabled
 *  2. Check cooldown
 *  3. Check for nearby players (configurable radius) — blocks if any found
 *  4. Start 8-second stand-still warmup countdown
 *  5. On countdown complete: find a safe surface location async → teleport
 *  6. Apply cooldown on success
 */
public final class RtpCommand implements CommandExecutor {

    private final WaffleHomes plugin;

    public RtpCommand(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }

        // ── 1. Enabled check ──────────────────────────────────────────────────
        if (!plugin.getConfigManager().isRtpEnabled()) {
            plugin.getMessageManager().send(player, "rtp-disabled");
            return true;
        }

        // ── 2. Cooldown check ─────────────────────────────────────────────────
        int remaining = plugin.getRtpManager().getCooldownRemaining(player.getUniqueId());
        if (remaining > 0) {
            plugin.getMessageManager().send(player, "rtp-cooldown",
                    Map.of("{time}", String.valueOf(remaining)));
            return true;
        }

        // ── 3. Nearby-players check ───────────────────────────────────────────
        int nearbyCount = plugin.getRtpManager().countNearbyPlayers(player);
        if (nearbyCount > 0) {
            double radius = plugin.getConfigManager().getRtpNearbyRadius();
            plugin.getMessageManager().send(player, "rtp-players-nearby",
                    Map.of("{count}",  String.valueOf(nearbyCount),
                           "{radius}", String.valueOf((int) radius)));
            return true;
        }

        // ── 4. Warmup countdown ───────────────────────────────────────────────
        // Countdown callback runs on the main thread after player stands still.
        plugin.getCountdownManager().start(player, () -> {

            // ── 5. Find safe location async, then teleport ────────────────────
            plugin.getMessageManager().send(player, "rtp-searching");

            plugin.getRtpManager()
                    .findSafeLocationAsync(player.getWorld())
                    .thenCompose(loc -> {
                        if (loc == null) {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    plugin.getMessageManager().send(player, "rtp-failed"));
                            return CompletableFuture.completedFuture(false);
                        }
                        return player.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    })
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        // ── 6. Apply cooldown on success ──────────────────────
                        if (success) {
                            plugin.getRtpManager().applyCooldown(player.getUniqueId());
                            plugin.getMessageManager().send(player, "rtp-success");
                        } else {
                            plugin.getMessageManager().send(player, "rtp-failed");
                        }
                    }));
        });

        return true;
    }
}
