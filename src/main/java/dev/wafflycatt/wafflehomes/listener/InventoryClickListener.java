package dev.wafflycatt.wafflehomes.listener;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.form.HomeIconForm;
import dev.wafflycatt.wafflehomes.gui.*;
import dev.wafflycatt.wafflehomes.manager.HomeManager;
import dev.wafflycatt.wafflehomes.model.Home;
import dev.wafflycatt.wafflehomes.model.PlayerSettings;
import dev.wafflycatt.wafflehomes.util.ChatInputManager;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;
import java.util.Map;

/**
 * Handles all WaffleHomes GUI click interactions (Java Edition only).
 *
 * Dispatches to:
 *  - HomesGui     → opens HomeActionGui on home slot click
 *  - HomeActionGui → Teleport (with countdown) / Rename / Change Icon / Delete / Back
 *  - HomeIconGui   → updates home icon material
 *  - TpSettingsGui → toggle settings
 */
public final class InventoryClickListener implements Listener {

    private final WaffleHomes plugin;

    public InventoryClickListener(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Object h = event.getWhoClicked().getOpenInventory().getTopInventory().getHolder();
        if (h instanceof HomesGui || h instanceof HomeActionGui
                || h instanceof HomeIconGui || h instanceof TpSettingsGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Object holder = event.getInventory().getHolder();

        if (holder instanceof HomesGui gui)      { handleHomesGui(event, player, gui); }
        else if (holder instanceof HomeActionGui gui) { handleActionGui(event, player, gui); }
        else if (holder instanceof HomeIconGui gui)   { handleIconGui(event, player, gui); }
        else if (holder instanceof TpSettingsGui gui) { handleTpSettingsGui(event, player, gui); }
    }

    // ─── HomesGui ──────────────────────────────────────────────────────────────

    private void handleHomesGui(InventoryClickEvent event, Player player, HomesGui gui) {
        event.setCancelled(true);
        if (!event.getClickedInventory().equals(gui.getInventory())) return;

        int slot = event.getRawSlot();

        // Pagination
        if (slot == 46 && gui.getPage() > 0) {
            refreshHomes(player, gui.getPage() - 1);
            return;
        }
        if (slot == 52 && gui.getPage() < gui.getTotalPages() - 1) {
            refreshHomes(player, gui.getPage() + 1);
            return;
        }

        Home home = gui.getHomeAt(slot);
        if (home == null) return;

        String title = plugin.getMessageManager().getRaw("gui-action-title");
        new HomeActionGui(home, player, title).open();
    }

    // ─── HomeActionGui ─────────────────────────────────────────────────────────

    private void handleActionGui(InventoryClickEvent event, Player player, HomeActionGui gui) {
        event.setCancelled(true);
        if (!event.getClickedInventory().equals(gui.getInventory())) return;

        switch (event.getRawSlot()) {
            case HomeActionGui.SLOT_TELEPORT -> teleportToHome(player, gui.getHome());
            case HomeActionGui.SLOT_RENAME   -> startRename(player, gui.getHome());
            case HomeActionGui.SLOT_ICON     -> openIconPicker(player, gui.getHome());
            case HomeActionGui.SLOT_DELETE   -> deleteHome(player, gui.getHome());
            case HomeActionGui.SLOT_BACK     -> refreshHomes(player, 0);
        }
    }

    private void teleportToHome(Player player, Home home) {
        player.closeInventory();
        org.bukkit.World world = Bukkit.getWorld(home.worldName());
        if (world == null) {
            plugin.getMessageManager().send(player, "home-not-found", Map.of("{name}", home.name()));
            return;
        }
        plugin.getMessageManager().send(player, "home-teleporting", Map.of("{name}", home.name()));
        Location dest = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        plugin.getCountdownManager().start(player, () ->
                player.teleportAsync(dest, PlayerTeleportEvent.TeleportCause.PLUGIN));
    }

    private void startRename(Player player, Home home) {
        player.closeInventory();
        ChatUtil.send(player,
                "&eType the new name for &6" + home.name() + "&e. Type &ccancel&e to cancel.");

        ChatInputManager.expect(player.getUniqueId(), input -> {
            if (input == null) {
                ChatUtil.send(player, "&cRename cancelled.");
                return;
            }
            if (!HomeManager.isValidName(input)) {
                plugin.getMessageManager().send(player, "home-invalid-name");
                return;
            }
            plugin.getHomeManager().renameHome(home.playerUuid(), home.name(), input)
                    .thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (ok) {
                            plugin.getMessageManager().send(player, "home-renamed",
                                    Map.of("{old}", home.name(), "{new}", input));
                        } else {
                            ChatUtil.send(player, "&cFailed to rename. Name may already exist.");
                        }
                    }));
        });
    }

    private void openIconPicker(Player player, Home home) {
        if (FloodgateUtil.isBedrockPlayer(player)) {
            // For Bedrock: show dropdown form (no inventory GUI)
            plugin.getHomeManager().getHomes(home.playerUuid())
                    .thenAccept(all -> Bukkit.getScheduler().runTask(plugin, () ->
                            HomeIconForm.show(plugin, player, home, all, player.getName())));
        } else {
            player.closeInventory();
            new HomeIconGui(home, player).open();
        }
    }

    private void deleteHome(Player player, Home home) {
        player.closeInventory();
        plugin.getHomeManager().deleteHome(home.playerUuid(), home.name())
                .thenAccept(deleted -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (deleted) {
                        plugin.getMessageManager().send(player, "home-deleted", Map.of("{name}", home.name()));
                    } else {
                        plugin.getMessageManager().send(player, "home-not-found", Map.of("{name}", home.name()));
                    }
                }));
    }

    // ─── HomeIconGui ───────────────────────────────────────────────────────────

    private void handleIconGui(InventoryClickEvent event, Player player, HomeIconGui gui) {
        event.setCancelled(true);
        if (!event.getClickedInventory().equals(gui.getInventory())) return;

        int slot = event.getRawSlot();

        // Back button
        if (slot == 49) {
            String title = plugin.getMessageManager().getRaw("gui-action-title");
            new HomeActionGui(gui.getHome(), player, title).open();
            return;
        }

        Material selected = gui.getMaterialAt(slot);
        if (selected == null) return;

        player.closeInventory();
        plugin.getHomeManager()
                .setHomeMaterial(gui.getHome().playerUuid(), gui.getHome().name(), selected.name())
                .thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ok) {
                        plugin.getMessageManager().send(player, "home-icon-set",
                                Map.of("{name}", gui.getHome().name(),
                                        "{icon}", HomeIconGui.formatName(selected)));
                    }
                }));
    }

    // ─── TpSettingsGui ─────────────────────────────────────────────────────────

    private void handleTpSettingsGui(InventoryClickEvent event, Player player, TpSettingsGui gui) {
        event.setCancelled(true);
        if (!event.getClickedInventory().equals(gui.getInventory())) return;

        int slot = event.getRawSlot();
        PlayerSettings current = gui.getSettings();
        PlayerSettings updated = switch (slot) {
            case TpSettingsGui.SLOT_ALLOW_TPA          -> current.withAllowTpa(!current.allowTpa());
            case TpSettingsGui.SLOT_ALLOW_TPHERE       -> current.withAllowTphere(!current.allowTphere());
            case TpSettingsGui.SLOT_AUTO_ACCEPT_TPA    -> current.withAutoAcceptTpa(!current.autoAcceptTpa());
            case TpSettingsGui.SLOT_AUTO_ACCEPT_TPHERE -> current.withAutoAcceptTphere(!current.autoAcceptTphere());
            default -> null;
        };

        if (updated == null) return;
        final PlayerSettings finalUpd = updated;
        plugin.getDatabaseManager().saveSettings(finalUpd)
                .thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                    gui.updateSettings(finalUpd);
                    plugin.getMessageManager().send(player, "tpsettings-updated");
                }));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void refreshHomes(Player player, int page) {
        plugin.getHomeManager().getHomes(player.getUniqueId())
                .thenAccept(homes -> Bukkit.getScheduler().runTask(plugin, () ->
                        new HomesGui(plugin, player, homes, page).open()));
    }
}
