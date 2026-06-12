package dev.wafflycatt.wafflehomes.form;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.manager.HomeManager;
import dev.wafflycatt.wafflehomes.model.Home;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.List;
import java.util.Map;

/**
 * Bedrock-native Floodgate forms for the homes system.
 *
 * Flow:
 *  1. showHomesForm()  → SimpleForm listing all homes as buttons
 *  2. Clicking a home  → showActionForm() → SimpleForm (Teleport / Rename / Change Icon / Delete)
 *  3. Action chosen    → execute (with countdown where applicable)
 */
public final class HomesForm {

    private HomesForm() {}

    /** Show the main homes list. */
    public static void showHomesForm(WaffleHomes plugin, Player player, List<Home> homes,
                                     boolean isAdmin, String adminTarget) {
        int limit  = plugin.getConfigManager().getHomesLimit();
        String title = isAdmin
                ? plugin.getMessageManager().getRaw("gui-adminhomes-title")
                        .replace("{player}", adminTarget)
                        .replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                : "Your Homes";
        String content = "Homes: " + homes.size() + " / " + limit + "\nSelect a home:";

        SimpleForm.Builder builder = SimpleForm.builder().title(title).content(content);

        if (homes.isEmpty()) {
            builder.button("No homes yet — use /sethome <name>",
                    FormImage.Type.PATH, "textures/ui/icon_recipe_item");
        } else {
            for (Home home : homes) {
                builder.button(home.name() + "\n" + home.coordsDisplay() + " | " + home.worldName(),
                        FormImage.Type.PATH, worldIcon(home.worldName()));
            }
        }

        SimpleForm form = builder
                .validResultHandler(response -> {
                    if (homes.isEmpty()) return;
                    int idx = response.clickedButtonId();
                    if (idx < 0 || idx >= homes.size()) return;
                    Home selected = homes.get(idx);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            showActionForm(plugin, player, selected, homes, isAdmin, adminTarget));
                })
                .build();

        FloodgateUtil.sendForm(player, form);
    }

    /** Show action options for a specific home. */
    private static void showActionForm(WaffleHomes plugin, Player player, Home home,
                                       List<Home> allHomes, boolean isAdmin, String adminTarget) {
        SimpleForm form = SimpleForm.builder()
                .title(home.name())
                .content("World: " + home.worldName()
                        + "\nCoords: " + home.coordsDisplay()
                        + "\n\nChoose an action:")
                .button("Teleport Here",   FormImage.Type.PATH, "textures/ui/realms_green_check")
                .button("Rename Home",     FormImage.Type.PATH, "textures/ui/editIcon")
                .button("Change Icon",     FormImage.Type.PATH, "textures/ui/color_plus")
                .button("Delete Home",     FormImage.Type.PATH, "textures/ui/cancel")
                .button("← Back to list", FormImage.Type.PATH, "textures/ui/arrow_left")
                .validResultHandler(response -> {
                    int clicked = response.clickedButtonId();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        switch (clicked) {
                            case 0 -> teleport(plugin, player, home);
                            case 1 -> showRenameForm(plugin, player, home, allHomes, isAdmin, adminTarget);
                            case 2 -> HomeIconForm.show(plugin, player, home, allHomes, adminTarget);
                            case 3 -> showDeleteConfirm(plugin, player, home, allHomes, isAdmin, adminTarget);
                            case 4 -> showHomesForm(plugin, player, allHomes, isAdmin, adminTarget);
                        }
                    });
                })
                .closedOrInvalidResultHandler(() ->
                        Bukkit.getScheduler().runTask(plugin, () ->
                                showHomesForm(plugin, player, allHomes, isAdmin, adminTarget)))
                .build();

        FloodgateUtil.sendForm(player, form);
    }

    private static void showRenameForm(WaffleHomes plugin, Player player, Home home,
                                       List<Home> allHomes, boolean isAdmin, String adminTarget) {
        CustomForm form = CustomForm.builder()
                .title("Rename: " + home.name())
                .input("New name", home.name(), home.name())
                .validResultHandler(response -> {
                    String newName = response.asInput(0);
                    if (newName == null || newName.isBlank()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                showActionForm(plugin, player, home, allHomes, isAdmin, adminTarget));
                        return;
                    }
                    newName = newName.trim();
                    if (!HomeManager.isValidName(newName)) {
                        final String fn = newName;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getMessageManager().send(player, "home-invalid-name");
                            showActionForm(plugin, player, home, allHomes, isAdmin, adminTarget);
                        });
                        return;
                    }
                    final String finalName = newName;
                    plugin.getHomeManager().renameHome(home.playerUuid(), home.name(), finalName)
                            .thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                                if (ok) {
                                    plugin.getMessageManager().send(player, "home-renamed",
                                            Map.of("{old}", home.name(), "{new}", finalName));
                                } else {
                                    ChatUtil.send(player, "&cFailed to rename home.");
                                }
                                plugin.getHomeManager().getHomes(home.playerUuid())
                                        .thenAccept(updated -> Bukkit.getScheduler().runTask(plugin, () ->
                                                showHomesForm(plugin, player, updated, isAdmin, adminTarget)));
                            }));
                })
                .closedOrInvalidResultHandler(() ->
                        Bukkit.getScheduler().runTask(plugin, () ->
                                showActionForm(plugin, player, home, allHomes, isAdmin, adminTarget)))
                .build();

        FloodgateUtil.sendForm(player, form);
    }

    private static void showDeleteConfirm(WaffleHomes plugin, Player player, Home home,
                                           List<Home> allHomes, boolean isAdmin, String adminTarget) {
        ModalForm form = ModalForm.builder()
                .title("Delete: " + home.name())
                .content("Delete \"" + home.name() + "\"?\nThis cannot be undone!")
                .button1("Yes, delete it!")
                .button2("No, go back")
                .validResultHandler(response -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (response.clickedFirst()) {
                        plugin.getHomeManager().deleteHome(home.playerUuid(), home.name())
                                .thenAccept(deleted -> Bukkit.getScheduler().runTask(plugin, () -> {
                                    if (deleted) {
                                        plugin.getMessageManager().send(player, "home-deleted",
                                                Map.of("{name}", home.name()));
                                    }
                                    plugin.getHomeManager().getHomes(home.playerUuid())
                                            .thenAccept(updated -> Bukkit.getScheduler().runTask(plugin, () ->
                                                    showHomesForm(plugin, player, updated, isAdmin, adminTarget)));
                                }));
                    } else {
                        showActionForm(plugin, player, home, allHomes, isAdmin, adminTarget);
                    }
                }))
                .closedOrInvalidResultHandler(() ->
                        Bukkit.getScheduler().runTask(plugin, () ->
                                showActionForm(plugin, player, home, allHomes, isAdmin, adminTarget)))
                .build();

        FloodgateUtil.sendForm(player, form);
    }

    private static void teleport(WaffleHomes plugin, Player player, Home home) {
        org.bukkit.World world = Bukkit.getWorld(home.worldName());
        if (world == null) {
            ChatUtil.send(player, "&cWorld &e" + home.worldName() + "&c is not loaded.");
            return;
        }
        plugin.getMessageManager().send(player, "home-teleporting", Map.of("{name}", home.name()));
        Location loc = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        plugin.getCountdownManager().start(player, () ->
                player.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN));
    }

    private static String worldIcon(String worldName) {
        String lower = worldName.toLowerCase();
        if (lower.contains("nether")) return "textures/ui/icon_new_nether";
        if (lower.contains("end"))    return "textures/ui/icon_new_end";
        return "textures/ui/icon_new_world";
    }
}
