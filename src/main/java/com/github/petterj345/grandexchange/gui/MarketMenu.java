package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.storage.MarketSummary;
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

/**
 * A one-sided order browser. In {@link Mode#BUYING} it lists items for sale (sell orders)
 * and clicking buys; in {@link Mode#SELLING} it lists items wanted (buy orders) and
 * clicking sells into them. Buy orders and sell orders are never shown in the same window.
 */
public final class MarketMenu implements InventoryHolder {

    public enum Mode {
        BUYING,
        SELLING
    }

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    public static final int SLOT_NEW = 46;

    private final Grandexchange plugin;
    private final Mode mode;
    private final List<MarketSummary> summaries;
    private final Map<Integer, MarketSummary> slotToSummary = new HashMap<>();
    private int page;
    private Inventory inventory;

    public MarketMenu(Grandexchange plugin, Mode mode, List<MarketSummary> summaries, int page) {
        this.plugin = plugin;
        this.mode = mode;
        this.summaries = summaries;
        this.page = page;
    }

    public Mode mode() {
        return mode;
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil(summaries.size() / (double) PER_PAGE));
    }

    public MarketSummary summaryAt(int slot) {
        return slotToSummary.get(slot);
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
        String title = (mode == Mode.BUYING ? "Buy — items for sale" : "Sell — buy orders")
                + " (" + (page + 1) + "/" + totalPages() + ")";
        inventory = Bukkit.createInventory(this, SIZE, Component.text(title));
        slotToSummary.clear();

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, summaries.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            MarketSummary summary = summaries.get(i);
            inventory.setItem(slot, render(summary));
            slotToSummary.put(slot, summary);
            slot++;
        }

        if (summaries.isEmpty()) {
            String line = mode == Mode.BUYING
                    ? "Nothing is for sale right now."
                    : "Nobody is buying right now.";
            inventory.setItem(22, Gui.button(Material.PAPER, line, List.of(
                    Gui.line(mode == Mode.BUYING
                            ? "Use 'Create a buy offer' to request an item."
                            : "Use 'Sell from inventory' to list one.", NamedTextColor.GRAY))));
        }

        Nav.render(inventory, page > 0, page < totalPages() - 1);

        if (mode == Mode.BUYING) {
            inventory.setItem(SLOT_NEW, Gui.button(Material.NETHER_STAR, "Create a buy offer", List.of(
                    Gui.line("Pick an item from your inventory to", NamedTextColor.GRAY),
                    Gui.line("buy more of it — even with no sellers.", NamedTextColor.GRAY))));
        } else {
            inventory.setItem(SLOT_NEW, Gui.button(Material.CHEST, "Sell from inventory", List.of(
                    Gui.line("Pick an item from your inventory to", NamedTextColor.GRAY),
                    Gui.line("list a sell offer at your own price.", NamedTextColor.GRAY))));
        }
    }

    private ItemStack render(MarketSummary summary) {
        ItemStack icon = summary.item().clone();
        List<Component> lore = new ArrayList<>();
        if (mode == Mode.BUYING) {
            icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), summary.askQuantity())));
            lore.add(Gui.line("Buy from " + plugin.economy().format(summary.lowestAsk()) + " each",
                    NamedTextColor.GREEN));
            lore.add(Gui.line(summary.askQuantity() + " available", NamedTextColor.DARK_GREEN));
            lore.add(Component.empty());
            lore.add(Gui.line("Click to buy", NamedTextColor.AQUA));
        } else {
            icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), summary.bidQuantity())));
            lore.add(Gui.line("Sell to " + plugin.economy().format(summary.highestBid()) + " each",
                    NamedTextColor.GOLD));
            lore.add(Gui.line(summary.bidQuantity() + " wanted", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Gui.line("Click to sell into these buy orders", NamedTextColor.AQUA));
        }
        return Gui.decorate(icon, lore);
    }
}
