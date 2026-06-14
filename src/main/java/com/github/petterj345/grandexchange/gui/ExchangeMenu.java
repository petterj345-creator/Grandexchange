package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.storage.Listing;
import com.github.petterj345.grandexchange.storage.MarketStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paginated chest GUI for browsing all listings (BROWSE) or your own listings (MINE).
 */
public final class ExchangeMenu implements InventoryHolder {

    public enum Mode {
        BROWSE,
        MINE
    }

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_BROWSE_TAB = 47;
    public static final int SLOT_SELL = 48;
    public static final int SLOT_MINE_TAB = 49;
    public static final int SLOT_INFO = 50;
    public static final int SLOT_NEXT = 53;

    private final Grandexchange plugin;
    private final Mode mode;
    private final List<Listing> listings;
    private final Map<String, MarketStats> statsCache = new HashMap<>();
    private final Map<Integer, Long> slotToListing = new HashMap<>();
    private int page;
    private Inventory inventory;

    public ExchangeMenu(Grandexchange plugin, Mode mode, List<Listing> listings, int page) {
        this.plugin = plugin;
        this.mode = mode;
        this.listings = listings;
        this.page = page;
    }

    public Mode mode() {
        return mode;
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil(listings.size() / (double) PER_PAGE));
    }

    public Long listingAt(int slot) {
        return slotToListing.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        build();
        player.openInventory(inventory);
    }

    public void reopen(Player player, int newPage) {
        this.page = Math.max(0, Math.min(newPage, totalPages() - 1));
        build();
        player.openInventory(inventory);
    }

    private void build() {
        String title = (mode == Mode.BROWSE ? "Grand Exchange" : "My Listings")
                + " (" + (page + 1) + "/" + totalPages() + ")";
        inventory = Bukkit.createInventory(this, SIZE, Component.text(title));
        slotToListing.clear();

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, listings.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            Listing listing = listings.get(i);
            inventory.setItem(slot, render(listing));
            slotToListing.put(slot, listing.id());
            slot++;
        }

        if (page > 0) {
            inventory.setItem(SLOT_PREV, nav(Material.ARROW, "Previous page"));
        }
        if (page < totalPages() - 1) {
            inventory.setItem(SLOT_NEXT, nav(Material.ARROW, "Next page"));
        }

        // Navigation tabs (present on every page, in both modes)
        inventory.setItem(SLOT_BROWSE_TAB, nav(Material.CHEST, "Browse listings"));
        inventory.setItem(SLOT_SELL, nav(Material.EMERALD, "Sell an item"));
        inventory.setItem(SLOT_MINE_TAB, nav(Material.WRITABLE_BOOK, "My listings"));

        String info;
        if (listings.isEmpty()) {
            info = mode == Mode.BROWSE
                    ? "No listings yet — click 'Sell an item' to add one"
                    : "You have no active listings";
        } else {
            info = mode == Mode.BROWSE ? "Click an item to buy" : "Click an item to cancel & reclaim";
        }
        inventory.setItem(SLOT_INFO, nav(Material.PAPER, info));
    }

    private ItemStack render(Listing listing) {
        ItemStack icon = listing.item().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), listing.quantity())));

        ItemMeta meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(line("Seller: " + listing.sellerName(), NamedTextColor.GRAY));
        lore.add(line("Available: " + listing.quantity(), NamedTextColor.GRAY));
        lore.add(line("Price each: " + plugin.economy().format(listing.pricePerItem()), NamedTextColor.GOLD));

        MarketStats stats = stats(listing.label());
        if (stats.hasData()) {
            lore.add(line("Market avg: " + plugin.economy().format(stats.average())
                    + " (" + stats.count() + " listings)", NamedTextColor.AQUA));
            lore.add(line("Market low: " + plugin.economy().format(stats.min()), NamedTextColor.AQUA));
        }
        lore.add(Component.empty());
        lore.add(line(mode == Mode.BROWSE ? "Click to buy" : "Click to cancel",
                mode == Mode.BROWSE ? NamedTextColor.GREEN : NamedTextColor.RED));

        if (meta != null) {
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private MarketStats stats(String label) {
        return statsCache.computeIfAbsent(label, l -> {
            try {
                return plugin.database().marketStats(l);
            } catch (Exception e) {
                return MarketStats.EMPTY;
            }
        });
    }

    private ItemStack nav(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line(name, NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
}
