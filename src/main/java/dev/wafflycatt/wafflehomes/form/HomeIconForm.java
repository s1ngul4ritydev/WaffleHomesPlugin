package dev.wafflycatt.wafflehomes.form;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.gui.HomeIconGui;
import dev.wafflycatt.wafflehomes.model.Home;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bedrock-native Floodgate form for choosing a home icon.
 * Shows a dropdown with all 28 curated material names.
 */
public final class HomeIconForm {

    private HomeIconForm() {}

    public static void show(WaffleHomes plugin, Player player, Home home,
                            List<Home> allHomes, String adminTarget) {
        List<String> names = Arrays.stream(HomeIconGui.ICON_CHOICES)
                .map(HomeIconGui::formatName)
                .collect(Collectors.toList());

        // Find index of current icon (if any)
        int currentIndex = 0;
        if (home.iconMaterial() != null) {
            for (int i = 0; i < HomeIconGui.ICON_CHOICES.length; i++) {
                if (HomeIconGui.ICON_CHOICES[i].name().equals(home.iconMaterial())) {
                    currentIndex = i;
                    break;
                }
            }
        }

        CustomForm form = CustomForm.builder()
                .title("Icon: " + home.name())
                .dropdown("Choose an icon", names, currentIndex)
                .validResultHandler(response -> {
                    int idx = response.asDropdown(0);
                    if (idx < 0 || idx >= HomeIconGui.ICON_CHOICES.length) return;
                    Material selected = HomeIconGui.ICON_CHOICES[idx];

                    plugin.getHomeManager()
                            .setHomeMaterial(home.playerUuid(), home.name(), selected.name())
                            .thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                                if (ok) {
                                    plugin.getMessageManager().send(player, "home-icon-set",
                                            Map.of("{name}", home.name(),
                                                    "{icon}", HomeIconGui.formatName(selected)));
                                }
                                // Refresh the homes list and re-open the homes form
                                plugin.getHomeManager()
                                        .getHomes(home.playerUuid())
                                        .thenAccept(updated -> Bukkit.getScheduler().runTask(plugin, () ->
                                                HomesForm.showHomesForm(plugin, player, updated,
                                                        !player.getUniqueId().equals(home.playerUuid()),
                                                        adminTarget)));
                            }));
                })
                .closedOrInvalidResultHandler(() ->
                        Bukkit.getScheduler().runTask(plugin, () ->
                                plugin.getHomeManager().getHomes(home.playerUuid())
                                        .thenAccept(updated -> Bukkit.getScheduler().runTask(plugin, () ->
                                                HomesForm.showHomesForm(plugin, player, updated,
                                                        !player.getUniqueId().equals(home.playerUuid()),
                                                        adminTarget)))))
                .build();

        FloodgateUtil.sendForm(player, form);
    }
}
