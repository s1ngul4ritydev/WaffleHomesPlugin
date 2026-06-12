package dev.wafflycatt.wafflehomes.manager;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles random teleportation (/rtp).
 *
 * Safety requirements enforced:
 * - Surface only (HeightMap.WORLD_SURFACE)
 * - Ground block must be solid and safe to stand on
 * - Feet and head blocks (y+1, y+2) must be passable and safe
 * - Never inside lava, water, caves, leaves, powder snow, fire, cactus, etc.
 */
public final class RtpManager {

    private static final Set<Material> DANGEROUS_GROUND = EnumSet.of(
            Material.LAVA, Material.MAGMA_BLOCK, Material.CACTUS,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.WITHER_ROSE, Material.SWEET_BERRY_BUSH,
            Material.POINTED_DRIPSTONE, Material.POWDER_SNOW
    );

    private static final Set<Material> DANGEROUS_INTERIOR = EnumSet.of(
            Material.LAVA, Material.WATER, Material.FIRE, Material.SOUL_FIRE,
            Material.CACTUS, Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE, Material.POINTED_DRIPSTONE,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.COBWEB
    );

    private final WaffleHomes plugin;
    /** playerUuid → cooldown expiry (System.currentTimeMillis()) */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public RtpManager(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    // ─── Cooldown ──────────────────────────────────────────────────────────────

    /** Returns remaining cooldown seconds, or 0 if none. */
    public int getCooldownRemaining(UUID uuid) {
        Long expiry = cooldowns.get(uuid);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /** Apply the RTP cooldown for a player after a successful teleport. */
    public void applyCooldown(UUID uuid) {
        int seconds = plugin.getConfigManager().getRtpCooldown();
        cooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    // ─── Nearby-players check ──────────────────────────────────────────────────

    /**
     * Returns the number of other online players within the configured nearby-radius.
     * Returns 0 if the radius is ≤ 0 (check disabled).
     */
    public int countNearbyPlayers(org.bukkit.entity.Player player) {
        double radius = plugin.getConfigManager().getRtpNearbyRadius();
        if (radius <= 0) return 0;
        return (int) player.getWorld()
                .getNearbyPlayers(player.getLocation(), radius)
                .stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .count();
    }

    // ─── Location finding ──────────────────────────────────────────────────────

    /**
     * Async: find a safe random surface location in {@code world}.
     * Returns null if no safe location is found within max-attempts.
     * Heavy work runs off the main thread.
     */
    public CompletableFuture<Location> findSafeLocationAsync(World world) {
        return CompletableFuture.supplyAsync(() -> findSafeLocation(world));
    }

    private Location findSafeLocation(World world) {
        int minR        = plugin.getConfigManager().getRtpMinRadius();
        int maxR        = plugin.getConfigManager().getRtpMaxRadius();
        int maxAttempts = plugin.getConfigManager().getRtpMaxAttempts();

        Location spawnLoc = world.getSpawnLocation();
        int spawnX = spawnLoc.getBlockX();
        int spawnZ = spawnLoc.getBlockZ();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle  = rng.nextDouble(0, 2 * Math.PI);
            double radius = rng.nextDouble(minR, maxR);
            int x = spawnX + (int) (radius * Math.cos(angle));
            int z = spawnZ + (int) (radius * Math.sin(angle));

            int surfaceY;
            try {
                surfaceY = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
            } catch (Exception e) {
                continue;
            }

            Block ground = world.getBlockAt(x, surfaceY,     z);
            Block feet   = world.getBlockAt(x, surfaceY + 1, z);
            Block head   = world.getBlockAt(x, surfaceY + 2, z);

            if (!isSafeGround(ground))   continue;
            if (!isSafeInterior(feet))   continue;
            if (!isSafeInterior(head))   continue;

            return new Location(world, x + 0.5, surfaceY + 1.0, z + 0.5,
                    rng.nextFloat(360f), 0f);
        }
        return null;
    }

    private boolean isSafeGround(Block block) {
        Material m = block.getType();
        return m.isSolid() && !DANGEROUS_GROUND.contains(m) && !isLeaves(m);
    }

    private boolean isSafeInterior(Block block) {
        Material m = block.getType();
        return !DANGEROUS_INTERIOR.contains(m) && !isLeaves(m) && block.isPassable();
    }

    private boolean isLeaves(Material m) {
        return m.name().endsWith("_LEAVES");
    }
}
