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
 * 6-row icon-picker GUI.
 * Displays 28 curated materials the player can choose as their home icon.
 * Clicking a material sets it as the home icon and closes the GUI.
 */
public final class HomeIconGui implements InventoryHolder {

    /** 28 curated materials — fits exactly in rows 1-4, columns 1-7. */
    public static final Material[] ICON_CHOICES = {
        // Row 1 — Nature
        Material.GRASS_BLOCK, Material.SAND, Material.GRAVEL, Material.SNOW_BLOCK,
        Material.ICE,         Material.PODZOL, Material.MYCELIUM,
        // Row 2 — Stone
        Material.STONE,       Material.COBBLESTONE, Material.GRANITE, Material.DIORITE,
        Material.ANDESITE,    Material.OBSIDIAN,    Material.NETHERRACK,
        // Row 3 — Wood
        Material.OAK_PLANKS,  Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
        Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
        Material.BAMBOO_PLANKS,
        // Row 4 — Precious
        Material.GOLD_BLOCK,  Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
        Material.IRON_BLOCK,  Material.AMETHYST_BLOCK, Material.LAPIS_BLOCK,
        Material.BEACON
    };

    // Slots matching HomesGui's home-content area: rows 1-4, cols 1-7
    private static final int[] ICON_SLOTS = buildIconSlots();

    private final Inventory inventory;
    private final Home home;
    private final Player player;

    public HomeIconGui(Home home, Player player) {
        this.home   = home;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54,
                ChatUtil.color("&6Choose Icon: &e" + home.name()));
        populate();
    }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    public void open() { player.openInventory(inventory); }

    public Home getHome()     { return home; }
    public Player getPlayer() { return player; }

    /** Returns the material at the given slot, or null if not an icon slot. */
    public Material getMaterialAt(int slot) {
        for (int i = 0; i < ICON_SLOTS.length; i++) {
            if (ICON_SLOTS[i] == slot) return ICON_CHOICES[i];
        }
        return null;
    }

    private void populate() {
        ItemStack border = makeBorder();
        for (int i = 0; i < 54; i++) {
            if (isBorderSlot(i)) inventory.setItem(i, border);
        }

        for (int i = 0; i < ICON_CHOICES.length; i++) {
            inventory.setItem(ICON_SLOTS[i], makeIconItem(ICON_CHOICES[i],
                    ICON_CHOICES[i].name().equals(home.iconMaterial())));
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta m = back.getItemMeta();
        m.displayName(ChatUtil.color("&7← Back to Home Actions"));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        back.setItemMeta(m);
        inventory.setItem(49, back);
    }

    private ItemStack makeIconItem(Material mat, boolean selected) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String label = selected ? "&a✔ " + formatName(mat) : "&f" + formatName(mat);
        meta.displayName(ChatUtil.color(label));
        if (selected) {
            meta.lore(List.of(ChatUtil.color("&aCurrently selected")));
        } else {
            meta.lore(List.of(ChatUtil.color("&eClick to set as icon")));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9, col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    /** Format a material name: "OAK_PLANKS" → "Oak Planks" */
    public static String formatName(Material m) {
        String[] words = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static int[] buildIconSlots() {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
