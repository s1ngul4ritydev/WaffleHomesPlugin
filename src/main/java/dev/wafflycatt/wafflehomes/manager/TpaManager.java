package dev.wafflycatt.wafflehomes.manager;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.model.PlayerSettings;
import dev.wafflycatt.wafflehomes.model.TpaRequest;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.cumulus.form.ModalForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Manages all TPA/TPHERE requests, cooldowns, and expiry tasks.
 *
 * Request flow:
 *  1. Sender calls /tpa <player>  → sendRequest()
 *  2. Target receives clickable chat (Java) or ModalForm (Bedrock)
 *  3. Target accepts/denies via /tpaccept|tpdeny or form button
 *  4. On accept: the teleportee must stand still for the configured warmup duration
 *  5. Request expires automatically after request-timeout seconds
 */
public final class TpaManager {

    /** Pending requests: targetUUID → list of TpaRequests directed at them */
    private final Map<UUID, List<TpaRequest>> pendingByTarget = new ConcurrentHashMap<>();

    /** Cooldowns: senderUUID → expiry time (millis) */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /** Expiry tasks keyed by "fromUUID:toUUID" */
    private final Map<String, BukkitTask> expiryTasks = new ConcurrentHashMap<>();

    private final WaffleHomes plugin;

    public TpaManager(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    // ─── Cooldown ──────────────────────────────────────────────────────────────

    public int getCooldownRemaining(UUID uuid) {
        Long expiry = cooldowns.get(uuid);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    private void applyCooldown(UUID uuid) {
        int seconds = plugin.getConfigManager().getRequestCooldown();
        cooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    // ─── Request management ────────────────────────────────────────────────────

    /** Returns all pending requests directed at the given target. */
    public List<TpaRequest> getRequestsFor(UUID targetUuid) {
        return pendingByTarget.getOrDefault(targetUuid, List.of());
    }

    /** Returns the most recent non-expired request directed at targetUuid. */
    public TpaRequest getMostRecentRequest(UUID targetUuid) {
        List<TpaRequest> requests = pendingByTarget.get(targetUuid);
        if (requests == null || requests.isEmpty()) return null;
        return requests.stream()
                .filter(r -> !r.isExpired())
                .reduce((a, b) -> b)  // last = most recent
                .orElse(null);
    }

    /** Returns a specific non-expired request from fromUuid to toUuid. */
    public TpaRequest getRequest(UUID fromUuid, UUID toUuid) {
        List<TpaRequest> requests = pendingByTarget.get(toUuid);
        if (requests == null) return null;
        return requests.stream()
                .filter(r -> r.from().equals(fromUuid) && !r.isExpired())
                .findFirst()
                .orElse(null);
    }

    /** Check if fromUuid already has a pending non-expired request to toUuid. */
    public boolean hasPendingRequest(UUID fromUuid, UUID toUuid) {
        return getRequest(fromUuid, toUuid) != null;
    }

    // ─── Send request ──────────────────────────────────────────────────────────

    /**
     * Send a TPA or TPHERE request from {@code from} to {@code to}.
     * Handles cooldown check, settings check, auto-accept, notification, and expiry.
     *
     * @param isHere true = TPHERE (to teleports to from); false = TPA (from teleports to to)
     * @return a CompletableFuture<SendResult> describing the outcome
     */
    public CompletableFuture<SendResult> sendRequest(Player from, Player to, boolean isHere) {
        return plugin.getDatabaseManager().getSettings(to.getUniqueId()).thenApplyAsync(settings -> {
            if (isHere && !settings.allowTphere()) return SendResult.TARGET_BLOCKED;
            if (!isHere && !settings.allowTpa())   return SendResult.TARGET_BLOCKED;

            int timeout = plugin.getConfigManager().getRequestTimeout();
            long expiry = System.currentTimeMillis() + (timeout * 1000L);
            TpaRequest request = new TpaRequest(from.getUniqueId(), to.getUniqueId(), isHere, expiry);

            pendingByTarget.computeIfAbsent(to.getUniqueId(), k -> new ArrayList<>()).add(request);
            applyCooldown(from.getUniqueId());

            // Auto-accept?
            boolean autoAccept = isHere ? settings.autoAcceptTphere() : settings.autoAcceptTpa();
            if (autoAccept) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    removeRequest(from.getUniqueId(), to.getUniqueId());
                    executeAccept(from, to, isHere);
                });
                return SendResult.AUTO_ACCEPTED;
            }

            // Schedule expiry
            String key = expiryKey(from.getUniqueId(), to.getUniqueId());
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                boolean removed = removeRequest(from.getUniqueId(), to.getUniqueId());
                if (removed) {
                    expiryTasks.remove(key);
                    notifyExpired(from.getUniqueId(), to.getUniqueId());
                }
            }, timeout * 20L);
            expiryTasks.put(key, task);

            Bukkit.getScheduler().runTask(plugin, () ->
                    notifyTarget(from, to, isHere, timeout));

            return SendResult.SENT;
        }, Bukkit.getScheduler().getMainThreadExecutor(plugin));
    }

    // ─── Accept / Deny ─────────────────────────────────────────────────────────

    /**
     * Accept the most recent request directed at {@code target}.
     * @return true if a request was found and processed
     */
    public boolean acceptMostRecent(Player target) {
        TpaRequest req = getMostRecentRequest(target.getUniqueId());
        if (req == null) return false;
        return accept(target, req.from());
    }

    /**
     * Accept the request from a specific player directed at {@code target}.
     * @return true if the request was found and processed
     */
    public boolean accept(Player target, UUID fromUuid) {
        TpaRequest req = getRequest(fromUuid, target.getUniqueId());
        if (req == null) return false;

        cancelExpiryTask(fromUuid, target.getUniqueId());
        removeRequest(fromUuid, target.getUniqueId());

        Player from = Bukkit.getPlayer(fromUuid);
        executeAccept(from, target, req.isHere());
        return true;
    }

    /**
     * Deny the most recent request directed at {@code target}.
     * @return true if a request was found and processed
     */
    public boolean denyMostRecent(Player target) {
        TpaRequest req = getMostRecentRequest(target.getUniqueId());
        if (req == null) return false;
        return deny(target, req.from());
    }

    /**
     * Deny the request from a specific player directed at {@code target}.
     * @return true if the request was found and processed
     */
    public boolean deny(Player target, UUID fromUuid) {
        TpaRequest req = getRequest(fromUuid, target.getUniqueId());
        if (req == null) return false;

        cancelExpiryTask(fromUuid, target.getUniqueId());
        removeRequest(fromUuid, target.getUniqueId());

        Player from = Bukkit.getPlayer(fromUuid);
        if (from != null && from.isOnline()) {
            plugin.getMessageManager().send(from, "tpa-denied-sender",
                    Map.of("{player}", target.getName()));
            from.playSound(from.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        plugin.getMessageManager().send(target, "tpa-denied-target",
                Map.of("{player}", from != null ? from.getName() : "Unknown"));
        return true;
    }

    // ─── Cancel all on shutdown ────────────────────────────────────────────────

    public void cancelAll() {
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        pendingByTarget.clear();
    }

    /** Cancel all pending requests involving a player who just quit. */
    public void handlePlayerQuit(UUID playerUuid) {
        pendingByTarget.values().forEach(list ->
                list.removeIf(r -> r.from().equals(playerUuid)));

        List<TpaRequest> incoming = pendingByTarget.remove(playerUuid);
        if (incoming != null) {
            for (TpaRequest req : incoming) {
                cancelExpiryTask(req.from(), playerUuid);
            }
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    /**
     * Execute an accepted TPA/TPHERE request — starts the warmup countdown for the teleportee.
     *
     * isHere=false (TPA)   → {@code from} teleports to {@code to}'s location
     * isHere=true  (TPHERE)→ {@code to}   teleports to {@code from}'s location
     *
     * The destination is captured at acceptance time so it stays fixed during warmup.
     */
    private void executeAccept(Player from, Player to, boolean isHere) {
        if (from == null || !from.isOnline()) return;

        Player teleportee   = isHere ? to   : from;
        Player destination  = isHere ? from : to;

        plugin.getMessageManager().send(from, "tpa-accepted-sender",
                Map.of("{player}", to.getName()));
        plugin.getMessageManager().send(to, "tpa-accepted-target",
                Map.of("{player}", from.getName()));

        from.playSound(from.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        to.playSound(to.getLocation(),     Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);

        // Capture destination location now — stays fixed for the countdown duration
        Location destLoc = destination.getLocation().clone();

        // Start warmup countdown for the player who will be moved
        plugin.getCountdownManager().start(teleportee, () ->
                teleportee.teleportAsync(destLoc, PlayerTeleportEvent.TeleportCause.PLUGIN)
                          .thenAccept(success -> {
                              if (!success) {
                                  Bukkit.getScheduler().runTask(plugin, () ->
                                          ChatUtil.send(teleportee,
                                                  "&cTeleport failed. Please try again."));
                              }
                          }));
    }

    private void notifyTarget(Player from, Player to, boolean isHere, int timeout) {
        if (!to.isOnline()) return;

        if (FloodgateUtil.isBedrockPlayer(to)) {
            String msgKey   = isHere ? "tphere-form-content" : "tpa-form-content";
            String titleKey = isHere ? "tphere-form-title"   : "tpa-form-title";
            String title    = plugin.getMessageManager().getFormText(titleKey);
            String content  = plugin.getMessageManager().getFormText(msgKey,
                    Map.of("{from}", from.getName(), "{timeout}", String.valueOf(timeout)));
            String acceptBtn = plugin.getMessageManager().getFormText("tpa-form-accept");
            String denyBtn   = plugin.getMessageManager().getFormText("tpa-form-deny");

            ModalForm form = ModalForm.builder()
                    .title(title)
                    .content(content)
                    .button1(acceptBtn)
                    .button2(denyBtn)
                    .validResultHandler(response ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                TpaRequest req = getRequest(from.getUniqueId(), to.getUniqueId());
                                if (req == null) {
                                    ChatUtil.send(to, plugin.getMessageManager().getPrefix()
                                            + "&cThis request has already expired.");
                                    return;
                                }
                                if (response.clickedFirst()) {
                                    accept(to, from.getUniqueId());
                                } else {
                                    deny(to, from.getUniqueId());
                                }
                            }))
                    .closedOrInvalidResultHandler(() ->
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    deny(to, from.getUniqueId())))
                    .build();

            FloodgateUtil.sendForm(to, form);
        } else {
            // Java: clickable chat
            String msgKey = isHere ? "tphere-request-received" : "tpa-request-received";
            String rawMsg = plugin.getMessageManager().getRaw(msgKey)
                    .replace("{from}", from.getName())
                    .replace("{timeout}", String.valueOf(timeout));

            String acceptCmd = isHere ? "/tphereaccept " + from.getName()
                                      : "/tpaccept "     + from.getName();
            String denyCmd   = isHere ? "/tpheredeny "   + from.getName()
                                      : "/tpdeny "       + from.getName();

            String prefix = plugin.getMessageManager().getPrefix();
            Component msg = Component.text()
                    .append(ChatUtil.color(prefix + rawMsg + " "))
                    .append(Component.text("[Accept]")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand(acceptCmd))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to accept"))))
                    .append(Component.text(" "))
                    .append(Component.text("[Deny]")
                            .color(NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand(denyCmd))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))))
                    .build();

            to.sendMessage(msg);
            to.playSound(to.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        }

        to.sendActionBar(ChatUtil.color("&eIncoming teleport request from &6" + from.getName()));
    }

    private void notifyExpired(UUID fromUuid, UUID toUuid) {
        Player from = Bukkit.getPlayer(fromUuid);
        Player to   = Bukkit.getPlayer(toUuid);
        String toName   = to   != null ? to.getName()   : "Unknown";
        String fromName = from != null ? from.getName() : "Unknown";
        if (from != null && from.isOnline()) {
            plugin.getMessageManager().send(from, "tpa-expired-sender",
                    Map.of("{player}", toName));
        }
        if (to != null && to.isOnline()) {
            plugin.getMessageManager().send(to, "tpa-expired-target",
                    Map.of("{player}", fromName));
        }
    }

    private boolean removeRequest(UUID fromUuid, UUID toUuid) {
        List<TpaRequest> list = pendingByTarget.get(toUuid);
        if (list == null) return false;
        boolean removed = list.removeIf(r -> r.from().equals(fromUuid));
        if (list.isEmpty()) pendingByTarget.remove(toUuid);
        return removed;
    }

    private void cancelExpiryTask(UUID fromUuid, UUID toUuid) {
        String key = expiryKey(fromUuid, toUuid);
        BukkitTask task = expiryTasks.remove(key);
        if (task != null) task.cancel();
    }

    private String expiryKey(UUID from, UUID to) {
        return from.toString() + ":" + to.toString();
    }

    // ─── Send result enum ──────────────────────────────────────────────────────

    public enum SendResult {
        SENT,
        AUTO_ACCEPTED,
        TARGET_BLOCKED
    }
}
