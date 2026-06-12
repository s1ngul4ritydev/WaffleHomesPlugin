package dev.wafflycatt.wafflehomes.manager;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player teleport warmup countdowns.
 *
 * Behaviour:
 *  - Player must stand still (XZ delta ≤ 0.5 blocks) for the configured warmup duration.
 *  - Every second the actionbar is updated: "Teleporting in Xs… Don't move!"
 *  - Any XZ movement cancels the countdown and notifies the player.
 *  - Starting a new countdown for the same player cancels any existing one first.
 *  - All callbacks run on the main server thread.
 */
public final class TeleportCountdownManager {

    private final WaffleHomes plugin;
    /** Active countdown tasks keyed by player UUID. */
    private final Map<UUID, BukkitTask> active = new ConcurrentHashMap<>();

    public TeleportCountdownManager(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    /**
     * Start a warmup countdown for {@code player}.
     * When the countdown finishes without movement, {@code onTeleport} is called on the main thread.
     *
     * @param player      The player who triggered the teleport
     * @param onTeleport  Callback executed on the main thread after the warmup completes
     */
    public void start(Player player, Runnable onTeleport) {
        cancelExisting(player.getUniqueId());

        int warmup = plugin.getConfigManager().getTeleportWarmup();
        Location startLoc = player.getLocation().clone();
        int[] remaining = {warmup};

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Safety check — player may have disconnected
            if (!player.isOnline()) {
                cancelExisting(player.getUniqueId());
                return;
            }

            // Movement check (XZ only — jumping in place is fine)
            Location cur = player.getLocation();
            if (!cur.getWorld().equals(startLoc.getWorld())
                    || Math.abs(startLoc.getX() - cur.getX()) > 0.5
                    || Math.abs(startLoc.getZ() - cur.getZ()) > 0.5) {
                cancelExisting(player.getUniqueId());
                plugin.getMessageManager().send(player, "teleport-cancelled-moved");
                player.sendActionBar(Component.empty());
                return;
            }

            // Countdown finished
            if (remaining[0] <= 0) {
                cancelExisting(player.getUniqueId());
                player.sendActionBar(Component.empty());
                onTeleport.run();
                return;
            }

            // Update actionbar
            String raw = plugin.getMessageManager().getRaw("teleport-warmup")
                    .replace("{time}", String.valueOf(remaining[0]));
            player.sendActionBar(ChatUtil.color(raw));
            remaining[0]--;

        }, 0L, 20L); // fire immediately, then every 20 ticks (1 second)

        active.put(player.getUniqueId(), task);
    }

    /** Cancel the active countdown for this player (no-op if none). */
    public void cancelExisting(UUID uuid) {
        BukkitTask task = active.remove(uuid);
        if (task != null) task.cancel();
    }

    /** Returns true if a countdown is currently running for this player. */
    public boolean hasActive(UUID uuid) {
        return active.containsKey(uuid);
    }

    /** Cancel all active countdowns (call on plugin shutdown). */
    public void cancelAll() {
        active.values().forEach(BukkitTask::cancel);
        active.clear();
    }
}
