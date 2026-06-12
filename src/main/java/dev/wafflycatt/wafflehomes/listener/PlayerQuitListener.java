package dev.wafflycatt.wafflehomes.listener;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.util.ChatInputManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up all per-player WaffleHomes state when a player disconnects.
 *
 *  - Cancels any active teleport warmup countdown
 *  - Cancels any pending TPA/TPHERE requests
 *  - Cancels any pending chat-input capture (rename, etc.)
 */
public final class PlayerQuitListener implements Listener {

    private final WaffleHomes plugin;

    public PlayerQuitListener(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        plugin.getCountdownManager().cancelExisting(uuid);
        plugin.getTpaManager().handlePlayerQuit(uuid);
        ChatInputManager.cancel(uuid);
    }
}
