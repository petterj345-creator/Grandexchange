package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.storage.CollectionEntry;
import com.github.petterj345.grandexchange.util.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The collection box: filled items and coin payouts waiting to be taken out. */
public final class CollectionMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    public static final int SLOT_COLLECT_ALL = 52;

    private final Grandexchange plugin;
    private final List<CollectionEntry> entries;
    private final Map<Integer, Long> slotToEntry = new HashMap<>();
    private int page;
    private Inventory inventory;

    public CollectionMenu(Grandexchange plugin, List<CollectionEntry> entries, int page) {
        this.plugin = plugin;
        this.entries = entries;
        this.page = page;
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
    }

    public Long entryAt(int slot) {
        return slotToEntry.get(slot);
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
        inventory = Bukkit.createInventory(this, SIZE,
                Component.text("Collection box (" + (page + 1) + "/" + totalPages() + ")"));
        slotToEntry.clear();

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, entries.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            CollectionEntry entry = entries.get(i);
            inventory.setItem(slot, render(entry));
            slotToEntry.put(slot, entry.id());
            slot++;
        }

        if (entries.isEmpty()) {
            inventory.setItem(22, Gui.button(Material.PAPER, "Nothing to collect", List.of(
                    Gui.line("Filled items and coins will appear here.", NamedTextColor.GRAY))));
        } else {
            inventory.setItem(SLOT_COLLECT_ALL, Gui.button(Material.CHEST, "Collect everything", null));
        }

        Nav.render(inventory, page > 0, page < totalPages() - 1);
    }

    private ItemStack render(CollectionEntry entry) {
        if (entry.isCoins()) {
            return Gui.button(Material.SUNFLOWER, plugin.economy().format(entry.coins()), List.of(
                    Gui.line("Coins from a sale", NamedTextColor.GRAY),
                    Gui.line("Click to collect", NamedTextColor.GREEN)));
        }
        ItemStack icon = entry.item().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), entry.quantity())));
        List<Component> lore = new ArrayList<>();
        lore.add(Gui.line(entry.quantity() + "x " + entry.item().getType().name(), NamedTextColor.GRAY));
        lore.add(Gui.line("Click to collect", NamedTextColor.GREEN));
        return Gui.decorate(icon, lore);
    }
}
