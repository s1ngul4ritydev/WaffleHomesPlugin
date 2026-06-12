package dev.wafflycatt.wafflehomes.gui;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.model.Home;
import dev.wafflycatt.wafflehomes.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 6-row chest inventory displaying a player's homes (Java Edition only).
 *
 * Layout (54 slots):
 *  Row 0 (0-8):   top glass border
 *  Rows 1-4:      homes, columns 1-7 (28 per page)
 *  Row 5 (45-53): bottom border; prev at 46, info at 49, next at 52
 *
 * Home icon material: uses {@code home.iconMaterial()} if set,
 * otherwise falls back to a world-based default.
 *
 * Clicking a home item opens {@link HomeActionGui}.
 */
public final class HomesGui implements InventoryHolder {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;
    private static final int[] HOME_SLOTS = buildHomeSlots(); // 28 slots
    private static final int HOMES_PER_PAGE = HOME_SLOTS.length;

    private final Inventory inventory;
    private final Player player;
    private final List<Home> homes;
    private final int page;
    private final int totalPages;
    private final Map<Integer, Home> slotMap = new HashMap<>();

    public HomesGui(WaffleHomes plugin, Player player, List<Home> homes, int page) {
        this.player     = player;
        this.homes      = homes;
        this.page       = page;
        this.totalPages = Math.max(1, (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE));

        String title = plugin.getMessageManager().getRaw("gui-homes-title");
        if (totalPages > 1) title += " &8(&7" + (page + 1) + "/" + totalPages + "&8)";

        this.inventory = Bukkit.createInventory(this, SIZE, ChatUtil.color(title));
        populate(plugin);
    }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    public void open()                     { player.openInventory(inventory); }
    public Home getHomeAt(int slot)        { return slotMap.get(slot); }
    public int getPage()                   { return page; }
    public int getTotalPages()             { return totalPages; }
    public Player getPlayer()              { return player; }
    public List<Home> getHomes()           { return homes; }

    // ─── Populate ──────────────────────────────────────────────────────────────

    private void populate(WaffleHomes plugin) {
        ItemStack border = makeBorder();
        for (int i = 0; i < SIZE; i++) {
            if (isBorderSlot(i)) inventory.setItem(i, border);
        }

        int start = page * HOMES_PER_PAGE;
        int end   = Math.min(start + HOMES_PER_PAGE, homes.size());
        for (int i = start; i < end; i++) {
            int slot  = HOME_SLOTS[i - start];
            Home home = homes.get(i);
            inventory.setItem(slot, makeHomeItem(home));
            slotMap.put(slot, home);
        }

        // Pagination
        if (page > 0)              inventory.setItem(46, makeNav("&7← Previous Page", Material.ARROW));
        if (page < totalPages - 1) inventory.setItem(52, makeNav("&7Next Page →",     Material.ARROW));

        // Info button (centre of bottom row)
        int limit = plugin.getConfigManager().getHomesLimit();
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta m = info.getItemMeta();
        m.displayName(ChatUtil.color("&6&l" + homes.size() + " &e/ " + limit + " &6Homes"));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        info.setItemMeta(m);
        inventory.setItem(49, info);
    }

    private ItemStack makeHomeItem(Home home) {
        Material mat  = resolveIcon(home);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ChatUtil.color("&6&l" + home.name()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(ChatUtil.color("&7World  &f" + home.worldName()));
        lore.add(ChatUtil.color("&7Coords &f" + home.coordsDisplay()));
        if (home.iconMaterial() != null) {
            lore.add(ChatUtil.color("&7Icon   &f" + HomeIconGui.formatName(
                    Material.getMaterial(home.iconMaterial()) != null
                            ? Material.getMaterial(home.iconMaterial())
                            : mat)));
        }
        lore.add(Component.empty());
        lore.add(ChatUtil.color("&eClick &7to open actions"));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Resolve the display material for a home.
     * Priority: player-set icon → world default.
     */
    private Material resolveIcon(Home home) {
        if (home.iconMaterial() != null) {
            Material m = Material.getMaterial(home.iconMaterial());
            if (m != null && m.isItem()) return m;
        }
        return worldDefault(home.worldName());
    }

    private Material worldDefault(String worldName) {
        String lower = worldName.toLowerCase();
        if (lower.contains("nether")) return Material.NETHERRACK;
        if (lower.contains("the_end") || lower.contains("end")) return Material.END_STONE;
        return Material.GRASS_BLOCK;
    }

    private ItemStack makeBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeNav(String label, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.color(label));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9, col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    private static int[] buildHomeSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++)
            for (int col = 1; col <= 7; col++)
                slots.add(row * 9 + col);
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
