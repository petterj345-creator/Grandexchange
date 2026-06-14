package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.storage.MarketSummary;
import com.github.petterj345.grandexchange.util.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The price viewer: one row per item on the market, showing the lowest sell price
 * (what you'd pay to buy) and the highest buy offer (what you'd get for selling).
 */
public final class MarketMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    public static final int SLOT_NEW_BUY = 46;

    private final Grandexchange plugin;
    private final List<MarketSummary> summaries;
    private final Map<Integer, MarketSummary> slotToSummary = new HashMap<>();
    private int page;
    private Inventory inventory;

    public MarketMenu(Grandexchange plugin, List<MarketSummary> summaries, int page) {
        this.plugin = plugin;
        this.summaries = summaries;
        this.page = page;
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
        inventory = Bukkit.createInventory(this, SIZE,
                Component.text("Buy / Prices (" + (page + 1) + "/" + totalPages() + ")"));
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
            inventory.setItem(22, Gui.button(org.bukkit.Material.PAPER,
                    "The market is empty", List.of(
                            Gui.line("Use 'Create a buy offer' to request an item,", NamedTextColor.GRAY),
                            Gui.line("or the Sell window to list something.", NamedTextColor.GRAY))));
        }

        Nav.render(inventory, page > 0, page < totalPages() - 1);
        inventory.setItem(SLOT_NEW_BUY, Gui.button(org.bukkit.Material.NETHER_STAR,
                "Create a buy offer", List.of(
                        Gui.line("Pick an item from your inventory", NamedTextColor.GRAY),
                        Gui.line("to buy more of it — even if no one", NamedTextColor.GRAY),
                        Gui.line("is selling it yet.", NamedTextColor.GRAY))));
    }

    private ItemStack render(MarketSummary summary) {
        ItemStack icon = summary.item().clone();
        int show = Math.max(summary.askQuantity(), summary.bidQuantity());
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), show)));

        List<Component> lore = new ArrayList<>();
        if (summary.hasAsk()) {
            lore.add(Gui.line("Buy from " + plugin.economy().format(summary.lowestAsk()) + " each",
                    NamedTextColor.GREEN));
            lore.add(Gui.line("  " + summary.askQuantity() + " available", NamedTextColor.DARK_GREEN));
        } else {
            lore.add(Gui.line("No sellers right now", NamedTextColor.GRAY));
        }
        if (summary.hasBid()) {
            lore.add(Gui.line("Sell to " + plugin.economy().format(summary.highestBid()) + " each",
                    NamedTextColor.GOLD));
            lore.add(Gui.line("  " + summary.bidQuantity() + " wanted", NamedTextColor.YELLOW));
        } else {
            lore.add(Gui.line("No buyers right now", NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        if (summary.hasAsk()) {
            lore.add(Gui.line("Click to buy", NamedTextColor.AQUA));
        } else {
            lore.add(Gui.line("Click to sell into these buy orders", NamedTextColor.AQUA));
        }
        return Gui.decorate(icon, lore);
    }
}
