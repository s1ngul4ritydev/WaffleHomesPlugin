package dev.wafflycatt.wafflehomes.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages pending one-shot chat input sessions.
 * Used for home renaming via chat on Java players when no better input method is available.
 *
 * Flow:
 * 1. Some command/GUI calls ChatInputManager.expect(uuid, consumer)
 * 2. ChatInputListener intercepts the player's next chat message
 * 3. The consumer receives the trimmed message text
 * 4. If the player types "cancel", the consumer receives null
 */
public final class ChatInputManager {

    private static final Map<UUID, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    private ChatInputManager() {}

    /** Register an expectation for the player's next chat message. */
    public static void expect(UUID playerUuid, Consumer<String> handler) {
        PENDING.put(playerUuid, handler);
    }

    /** Returns true if we are waiting for input from this player. */
    public static boolean isWaiting(UUID playerUuid) {
        return PENDING.containsKey(playerUuid);
    }

    /**
     * Consume and dispatch the player's chat message.
     * Returns true if the message was consumed (caller should cancel the chat event).
     */
    public static boolean consume(UUID playerUuid, String message) {
        Consumer<String> handler = PENDING.remove(playerUuid);
        if (handler == null) return false;
        if (message.equalsIgnoreCase("cancel")) {
            handler.accept(null);
        } else {
            handler.accept(message.trim());
        }
        return true;
    }

    /** Cancel any pending input for this player without invoking the handler. */
    public static void cancel(UUID playerUuid) {
        PENDING.remove(playerUuid);
    }
}
