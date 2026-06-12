package dev.wafflycatt.wafflehomes.manager;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.model.Home;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Business-logic layer for the home system.
 * Delegates all persistence to DatabaseManager.
 */
public final class HomeManager {

    /** Allowed home-name pattern: letters, digits, hyphens, underscores, 1–32 chars. */
    public static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]{1,32}$");

    private final WaffleHomes plugin;

    public HomeManager(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    /** Async: get all homes for a player (sorted by name). */
    public CompletableFuture<List<Home>> getHomes(UUID uuid) {
        return plugin.getDatabaseManager().getHomes(uuid);
    }

    /** Async: get a home by name (case-insensitive). */
    public CompletableFuture<Optional<Home>> getHome(UUID uuid, String name) {
        return plugin.getDatabaseManager().getHome(uuid, name);
    }

    /** Async: count a player's homes. */
    public CompletableFuture<Integer> countHomes(UUID uuid) {
        return plugin.getDatabaseManager().countHomes(uuid);
    }

    /**
     * Async: set a home, respecting the configured home limit.
     *
     * @return SetResult indicating the outcome
     */
    public CompletableFuture<SetResult> setHome(UUID uuid, String name, String world,
                                                 double x, double y, double z,
                                                 float yaw, float pitch) {
        return plugin.getDatabaseManager().getHome(uuid, name).thenCompose(existing -> {
            if (existing.isPresent()) {
                // Overwriting — no limit check needed
                return plugin.getDatabaseManager()
                        .setHome(uuid, name, world, x, y, z, yaw, pitch)
                        .thenApply(ok -> ok ? SetResult.UPDATED : SetResult.ERROR);
            }
            return plugin.getDatabaseManager().countHomes(uuid).thenCompose(count -> {
                if (count >= plugin.getConfigManager().getHomesLimit()) {
                    return CompletableFuture.completedFuture(SetResult.LIMIT_REACHED);
                }
                return plugin.getDatabaseManager()
                        .setHome(uuid, name, world, x, y, z, yaw, pitch)
                        .thenApply(ok -> ok ? SetResult.CREATED : SetResult.ERROR);
            });
        });
    }

    /** Async: delete a home. Returns true if deleted. */
    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        return plugin.getDatabaseManager().deleteHome(uuid, name);
    }

    /** Async: rename a home. Returns true if renamed. */
    public CompletableFuture<Boolean> renameHome(UUID uuid, String oldName, String newName) {
        return plugin.getDatabaseManager().renameHome(uuid, oldName, newName);
    }

    /** Async: update the icon material for a home. Returns true on success. */
    public CompletableFuture<Boolean> setHomeMaterial(UUID uuid, String homeName, String material) {
        return plugin.getDatabaseManager().updateHomeMaterial(uuid, homeName, material);
    }

    public enum SetResult {
        CREATED,
        UPDATED,
        LIMIT_REACHED,
        ERROR
    }
}
