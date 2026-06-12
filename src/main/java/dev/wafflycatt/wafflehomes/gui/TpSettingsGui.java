package dev.wafflycatt.wafflehomes.gui;

import dev.wafflycatt.wafflehomes.model.PlayerSettings;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 3-row GUI showing toggleable TP settings (Java Edition only).
 *
 * Slots:
 *   10 — Allow TPA
 *   12 — Allow TPHERE
 *   14 — Auto-Accept TPA
 *   16 — Auto-Accept TPHERE
 */
public final class TpSettingsGui implements InventoryHolder {

    public static final int SLOT_ALLOW_TPA           = 10;
    public static final int SLOT_ALLOW_TPHERE        = 12;
    public static final int SLOT_AUTO_ACCEPT_TPA     = 14;
    public static final int SLOT_AUTO_ACCEPT_TPHERE  = 16;

    private final Inventory inventory;
    private final Player player;
    private PlayerSettings settings;

    public TpSettingsGui(Player player, PlayerSettings settings, String title) {
        this.player   = player;
        this.settings = settings;
        this.inventory = Bukkit.createInventory(this, 27, ChatUtil.color(title));
        populate();
    }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    public void open() { player.openInventory(inventory); }

    public Player getPlayer() { return player; }

    public PlayerSettings getSettings() { return settings; }

    public void updateSettings(PlayerSettings updated) {
        this.settings = updated;
        populate();
    }

    private void populate() {
        ItemStack glass = makeGlass();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        inventory.setItem(4, makeTitle());

        inventory.setItem(SLOT_ALLOW_TPA,          makeToggle("Allow TPA Requests",    settings.allowTpa()));
        inventory.setItem(SLOT_ALLOW_TPHERE,        makeToggle("Allow TPHERE Requests", settings.allowTphere()));
        inventory.setItem(SLOT_AUTO_ACCEPT_TPA,     makeToggle("Auto-Accept TPA",        settings.autoAcceptTpa()));
        inventory.setItem(SLOT_AUTO_ACCEPT_TPHERE,  makeToggle("Auto-Accept TPHERE",     settings.autoAcceptTphere()));
    }

    private ItemStack makeToggle(String label, boolean enabled) {
        Material mat  = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = enabled ? "&aEnabled" : "&cDisabled";
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.color("&f" + label));
        meta.lore(List.of(
                Component.empty(),
                ChatUtil.color("&7Status: " + status),
                Component.empty(),
                ChatUtil.color("&eClick to toggle")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeTitle() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.color("&6&l✦ Teleport Settings"));
        meta.lore(List.of(ChatUtil.color("&7Click any setting to toggle it.")));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
