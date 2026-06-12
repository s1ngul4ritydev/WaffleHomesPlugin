package dev.wafflycatt.wafflehomes.model;

import java.util.UUID;

/**
 * Immutable representation of a player home stored in SQLite.
 * {@code iconMaterial} is nullable; null means "use the world-based default".
 */
public record Home(
        int id,
        UUID playerUuid,
        String name,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String iconMaterial   // nullable — Bukkit Material name, e.g. "GOLD_BLOCK"
) {
    /** Formatted coordinate string for display. */
    public String coordsDisplay() {
        return String.format("%d, %d, %d", (int) x, (int) y, (int) z);
    }
}
