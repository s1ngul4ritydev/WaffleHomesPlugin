package dev.wafflycatt.wafflehomes.config;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads and serves messages from messages.yml.
 * Supports {placeholder} replacement and & color codes.
 */
public final class MessageManager {

    private final WaffleHomes plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(WaffleHomes plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reload messages.yml from disk. */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);

        // Merge defaults from jar
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }

        prefix = messages.getString("prefix", "&8[&6WaffleHomes&8] ");
    }

    /** Get a raw (uncolored) message string. */
    public String getRaw(String key) {
        return messages.getString(key, "&cMissing message: " + key);
    }

    /** Get a formatted Component for the given key with no placeholders. */
    public Component get(String key) {
        return ChatUtil.color(prefix + getRaw(key));
    }

    /** Get a formatted Component without the prefix. */
    public Component getNoPrefix(String key) {
        return ChatUtil.color(getRaw(key));
    }

    /**
     * Get a formatted Component with placeholder substitution.
     * Placeholders map: e.g. Map.of("{name}", "MyHome", "{limit}", "6")
     */
    public Component get(String key, Map<String, String> placeholders) {
        String raw = prefix + getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return ChatUtil.color(raw);
    }

    /** Get a no-prefix formatted Component with placeholder substitution. */
    public Component getNoPrefix(String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        return ChatUtil.color(raw);
    }

    /** Send a prefixed message to a CommandSender. */
    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    /** Send a prefixed message with placeholders to a CommandSender. */
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    /** Get the raw string value for use in forms (Bedrock UI — no color codes). */
    public String getFormText(String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace(entry.getKey(), entry.getValue());
        }
        // Strip & color codes for Bedrock form text
        return raw.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }

    public String getFormText(String key) {
        return getFormText(key, Map.of());
    }

    public String getPrefix() { return prefix; }
}
