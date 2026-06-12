package dev.wafflycatt.wafflehomes.gui;

import dev.wafflycatt.wafflehomes.model.Home;
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
 * 3-row action sub-GUI opened when a player clicks a home in HomesGui.
 *
 * Slot layout (27 slots):
 *   0  — Back arrow
 *   4  — Home info (BOOK)
 *  10  — Teleport (LIME_CONCRETE)
 *  12  — Rename   (ORANGE_CONCRETE)
 *  14  — Change Icon (CYAN_CONCRETE)
 *  16  — Delete   (RED_CONCRETE)
 *  rest — gray glass
 */
public final class HomeActionGui implements InventoryHolder {

    public static final int SLOT_BACK     =  0;
    public static final int SLOT_TELEPORT = 10;
    public static final int SLOT_RENAME   = 12;
    public static final int SLOT_ICON     = 14;
    public static final int SLOT_DELETE   = 16;

    private final Inventory inventory;
    private final Home home;
    private final Player player;

    public HomeActionGui(Home home, Player player, String titleTemplate) {
        this.home   = home;
        this.player = player;

        String title = titleTemplate.replace("{name}", home.name());
        this.inventory = Bukkit.createInventory(this, 27, ChatUtil.color(title));
        populate();
    }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    public void open()            { player.openInventory(inventory); }
    public Home getHome()         { return home; }
    public Player getPlayer()     { return player; }

    private void populate() {
        ItemStack glass = makeGlass();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        inventory.setItem(4, makeInfoItem());

        inventory.setItem(SLOT_TELEPORT, makeButton(Material.LIME_CONCRETE,
                "&a&l⤷  Teleport",
                List.of("&7Click to teleport to &e" + home.name())));

        inventory.setItem(SLOT_RENAME, makeButton(Material.ORANGE_CONCRETE,
                "&6&l✎  Rename",
                List.of("&7Click to rename &e" + home.name())));

        inventory.setItem(SLOT_ICON, makeButton(Material.CYAN_CONCRETE,
                "&b&l◈  Change Icon",
                List.of("&7Click to change the icon for &e" + home.name())));

        inventory.setItem(SLOT_DELETE, makeButton(Material.RED_CONCRETE,
                "&c&l✕  Delete",
                List.of("&7Click to delete &e" + home.name(), "&cThis cannot be undone!")));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(ChatUtil.color("&7← Back to Homes"));
        bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        back.setItemMeta(bm);
        inventory.setItem(SLOT_BACK, back);
    }

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.color("&6&l" + home.name()));
        meta.lore(List.of(
                Component.empty(),
                ChatUtil.color("&7World  &f" + home.worldName()),
                ChatUtil.color("&7Coords &f" + home.coordsDisplay())
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeButton(Material mat, String name, List<String> lorelines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.color(name));
        List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.empty());
        for (String line : lorelines) lore.add(ChatUtil.color(line));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
