package dev.wafflycatt.wafflehomes.form;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.model.PlayerSettings;
import dev.wafflycatt.wafflehomes.util.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;

/**
 * Bedrock-native Floodgate CustomForm for teleport settings.
 * Shows 4 toggle controls; on submit, saves and confirms to the player.
 */
public final class TpSettingsForm {

    private TpSettingsForm() {}

    public static void show(WaffleHomes plugin, Player player, PlayerSettings settings) {
        CustomForm form = CustomForm.builder()
                .title("Teleport Settings")
                .toggle("Allow TPA Requests",    settings.allowTpa())
                .toggle("Allow TPHERE Requests", settings.allowTphere())
                .toggle("Auto-Accept TPA",        settings.autoAcceptTpa())
                .toggle("Auto-Accept TPHERE",     settings.autoAcceptTphere())
                .validResultHandler(response -> {
                    boolean allowTpa          = response.asToggle(0);
                    boolean allowTphere       = response.asToggle(1);
                    boolean autoAcceptTpa     = response.asToggle(2);
                    boolean autoAcceptTphere  = response.asToggle(3);

                    PlayerSettings updated = new PlayerSettings(
                            player.getUniqueId(),
                            allowTpa, allowTphere, autoAcceptTpa, autoAcceptTphere);

                    plugin.getDatabaseManager().saveSettings(updated)
                            .thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () -> {
                                if (ok) {
                                    plugin.getMessageManager().send(player, "tpsettings-updated");
                                }
                            }));
                })
                .build();

        FloodgateUtil.sendForm(player, form);
    }
}
