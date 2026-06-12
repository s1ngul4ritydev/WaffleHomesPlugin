package dev.wafflycatt.wafflehomes.config;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Typed accessor for config.yml values.
 * Call reload() to pick up changes without a full server restart.
 */
public final class ConfigManager {

    private final WaffleHomes plugin;

    private int     homesLimit;
    private int     teleportWarmup;
    private boolean rtpEnabled;
    private int     rtpMinRadius;
    private int     rtpMaxRadius;
    private int     rtpCooldown;
    private int     rtpMaxAttempts;
    private double  rtpNearbyRadius;
    private boolean tpaEnabled;
    private int     requestTimeout;
    private int     requestCooldown;
    private List<String> admins;

    public ConfigManager(WaffleHomes plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-read all values from the current config.yml. */
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        homesLimit       = c.getInt("homes-limit", 6);
        teleportWarmup   = c.getInt("teleport-warmup", 8);
        rtpEnabled       = c.getBoolean("rtp-enabled", true);
        rtpMinRadius     = c.getInt("rtp-min-radius", 1000);
        rtpMaxRadius     = c.getInt("rtp-max-radius", 10000);
        rtpCooldown      = c.getInt("rtp-cooldown", 300);
        rtpMaxAttempts   = c.getInt("rtp-max-attempts", 50);
        rtpNearbyRadius  = c.getDouble("rtp-nearby-radius", 34.0);
        tpaEnabled       = c.getBoolean("tpa-enabled", true);
        requestTimeout   = c.getInt("request-timeout", 30);
        requestCooldown  = c.getInt("request-cooldown", 10);
        admins           = c.getStringList("admins");
    }

    public int     getHomesLimit()       { return homesLimit; }
    public int     getTeleportWarmup()   { return teleportWarmup; }
    public boolean isRtpEnabled()        { return rtpEnabled; }
    public int     getRtpMinRadius()     { return rtpMinRadius; }
    public int     getRtpMaxRadius()     { return rtpMaxRadius; }
    public int     getRtpCooldown()      { return rtpCooldown; }
    public int     getRtpMaxAttempts()   { return rtpMaxAttempts; }
    public double  getRtpNearbyRadius()  { return rtpNearbyRadius; }
    public boolean isTpaEnabled()        { return tpaEnabled; }
    public int     getRequestTimeout()   { return requestTimeout; }
    public int     getRequestCooldown()  { return requestCooldown; }
    public List<String> getAdmins()      { return admins; }
}
