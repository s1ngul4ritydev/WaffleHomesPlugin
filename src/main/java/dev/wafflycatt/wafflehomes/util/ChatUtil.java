package dev.wafflycatt.wafflehomes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Utilities for converting &-color-coded strings to Adventure Components
 * and sending messages to CommandSenders.
 */
public final class ChatUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private ChatUtil() {}

    /** Convert a string with &-color codes to an Adventure Component. */
    public static Component color(String message) {
        return SERIALIZER.deserialize(message);
    }

    /** Send a colored message to any CommandSender. */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    /** Strip all &-color codes from a string. */
    public static String strip(String message) {
        return SERIALIZER.serialize(color(message)).replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
