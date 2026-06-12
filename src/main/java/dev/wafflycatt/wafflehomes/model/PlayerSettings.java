package dev.wafflycatt.wafflehomes.model;

import java.util.UUID;

/**
 * Per-player teleport preferences stored in SQLite.
 */
public record PlayerSettings(
        UUID playerUuid,
        boolean allowTpa,
        boolean allowTphere,
        boolean autoAcceptTpa,
        boolean autoAcceptTphere
) {
    /** Default settings for a new player. */
    public static PlayerSettings defaultFor(UUID uuid) {
        return new PlayerSettings(uuid, true, true, false, false);
    }

    public PlayerSettings withAllowTpa(boolean value)          { return new PlayerSettings(playerUuid, value, allowTphere, autoAcceptTpa, autoAcceptTphere); }
    public PlayerSettings withAllowTphere(boolean value)       { return new PlayerSettings(playerUuid, allowTpa, value, autoAcceptTpa, autoAcceptTphere); }
    public PlayerSettings withAutoAcceptTpa(boolean value)     { return new PlayerSettings(playerUuid, allowTpa, allowTphere, value, autoAcceptTphere); }
    public PlayerSettings withAutoAcceptTphere(boolean value)  { return new PlayerSettings(playerUuid, allowTpa, allowTphere, autoAcceptTpa, value); }
}
