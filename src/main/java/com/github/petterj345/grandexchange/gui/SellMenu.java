package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.MarketSummary;
import com.github.petterj345.grandexchange.util.Gui;
import com.github.petterj345.grandexchange.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Compose a sell offer: pick quantity (buttons or chat) and your price (chat),
 * with the current market prices shown for reference.
 */
public final class SellMenu implements InventoryHolder {

    public static final int SLOT_ITEM = 13;
    public static final int SLOT_DEC_10 = 10;
    public static final int SLOT_DEC_1 = 11;
    public static final int SLOT_INC_1 = 15;
    public static final int SLOT_INC_10 = 16;
    public static final int SLOT_ALL = 9;
    public static final int SLOT_FILL = 6;
    public static final int SLOT_TYPE_AMOUNT = 17;
    public static final int SLOT_SET_PRICE = 4;
    public static final int SLOT_USE_MARKET = 5;
    public static final int SLOT_CONFIRM = 22;
    public static final int SLOT_CANCEL = 26;

    private final Grandexchange plugin;
    private final SellSession session;
    private Inventory inventory;

    public SellMenu(Grandexchange plugin, SellSession session) {
        this.plugin = plugin;
        this.session = session;
    }

    public SellSession session() {
        return session;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        build(player);
        player.openInventory(inventory);
    }

    private void build(Player player) {
        inventory = Bukkit.createInventory(this, 27, Component.text("Sell offer"));

        int available = Items.count(player, session.template());
        if (available > 0) {
            session.amount(Math.max(1, Math.min(session.amount(), available)));
        }
        MarketSummary summary = summary();

        inventory.setItem(SLOT_ITEM, itemIcon(available, summary));
        inventory.setItem(SLOT_DEC_10, Gui.button(Material.RED_STAINED_GLASS_PANE, "-10", null));
        inventory.setItem(SLOT_DEC_1, Gui.button(Material.PINK_STAINED_GLASS_PANE, "-1", null));
        inventory.setItem(SLOT_INC_1, Gui.button(Material.LIME_STAINED_GLASS_PANE, "+1", null));
        inventory.setItem(SLOT_INC_10, Gui.button(Material.GREEN_STAINED_GLASS_PANE, "+10", null));
        inventory.setItem(SLOT_ALL, Gui.button(Material.HOPPER, "Sell all (" + available + ")", null));
        inventory.setItem(SLOT_FILL, fillButton(player, available));
        inventory.setItem(SLOT_TYPE_AMOUNT, Gui.button(Material.OAK_SIGN, "Type amount in chat", null));
        inventory.setItem(SLOT_SET_PRICE, priceButton());
        inventory.setItem(SLOT_USE_MARKET, marketButton(summary));
        inventory.setItem(SLOT_CONFIRM, Gui.button(Material.EMERALD_BLOCK, "Place sell offer", null));
        inventory.setItem(SLOT_CANCEL, Gui.button(Material.BARRIER, "Cancel", null));
    }

    private MarketSummary summary() {
        try {
            return plugin.database().marketSummary(session.label());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack itemIcon(int available, MarketSummary summary) {
        ItemStack icon = session.template().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), session.amount())));

        List<Component> lore = new ArrayList<>();
        lore.add(Gui.line("Amount to sell: " + session.amount(), NamedTextColor.WHITE));
        if (session.pricePerItem() > 0) {
            lore.add(Gui.line("Your price each: " + plugin.economy().format(session.pricePerItem()),
                    NamedTextColor.GOLD));
            lore.add(Gui.line("Total if all sells: "
                    + plugin.economy().format(session.pricePerItem() * session.amount()), NamedTextColor.GOLD));
        } else {
            lore.add(Gui.line("Your price each: not set", NamedTextColor.RED));
        }
        lore.add(Component.empty());
        if (summary != null && summary.hasBid()) {
            lore.add(Gui.line("Highest buy offer: " + plugin.economy().format(summary.highestBid()),
                    NamedTextColor.AQUA));
            lore.add(Gui.line(summary.bidQuantity() + " wanted now", NamedTextColor.DARK_AQUA));
        } else {
            lore.add(Gui.line("No buyers yet — your offer will rest", NamedTextColor.DARK_AQUA));
        }
        if (summary != null && summary.hasAsk()) {
            lore.add(Gui.line("Lowest sell price: " + plugin.economy().format(summary.lowestAsk()),
                    NamedTextColor.AQUA));
        }
        lore.add(Component.empty());
        lore.add(Gui.line("You have " + available + " in your inventory", NamedTextColor.GRAY));
        return Gui.decorate(icon, lore);
    }

    /** How much the buy orders want at the current price (capped at what the player has). */
    public int buyDemand(Player player) {
        try {
            return plugin.database().matchableBuyQuantity(session.label(), session.pricePerItem(),
                    player.getUniqueId());
        } catch (Exception e) {
            return 0;
        }
    }

    private ItemStack fillButton(Player player, int available) {
        int demand = buyDemand(player);
        int settable = Math.min(available, demand);
        List<Component> lore = new ArrayList<>();
        lore.add(Gui.line("Buy orders want: " + demand + " at your price", NamedTextColor.AQUA));
        if (demand > 0) {
            lore.add(Gui.line("Click to sell exactly " + settable, NamedTextColor.GRAY));
        } else {
            lore.add(Gui.line("No buy orders at your price", NamedTextColor.GRAY));
        }
        return Gui.button(Material.TARGET, "Sell exact amount wanted", lore);
    }

    private ItemStack priceButton() {
        List<Component> lore = new ArrayList<>();
        lore.add(session.pricePerItem() > 0
                ? Gui.line("Current: " + plugin.economy().format(session.pricePerItem()) + " each",
                NamedTextColor.GRAY)
                : Gui.line("Current: not set", NamedTextColor.GRAY));
        return Gui.button(Material.GOLD_INGOT, "Set price (type in chat)", lore);
    }

    private ItemStack marketButton(MarketSummary summary) {
        List<Component> lore = new ArrayList<>();
        if (summary != null && summary.hasBid()) {
            lore.add(Gui.line("Sell into top buy offer: "
                    + plugin.economy().format(summary.highestBid()), NamedTextColor.GRAY));
        } else if (summary != null && summary.hasAsk()) {
            lore.add(Gui.line("Match lowest ask: "
                    + plugin.economy().format(summary.lowestAsk()), NamedTextColor.GRAY));
        } else {
            lore.add(Gui.line("No market data yet", NamedTextColor.GRAY));
        }
        return Gui.button(Material.GOLD_NUGGET, "Use market price", lore);
    }
}
