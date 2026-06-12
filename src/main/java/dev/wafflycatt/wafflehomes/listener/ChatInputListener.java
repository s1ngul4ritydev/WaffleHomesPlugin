package dev.wafflycatt.wafflehomes.listener;

import dev.wafflycatt.wafflehomes.util.ChatInputManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Intercepts chat messages when a player has a pending ChatInputManager session.
 * Used for home renaming on Java Edition (typed chat input).
 */
public final class ChatInputListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        if (!ChatInputManager.isWaiting(uuid)) return;

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        ChatInputManager.consume(uuid, text);
    }
}
