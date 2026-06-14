package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.MarketStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.github.petterj345.grandexchange.util.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for composing a sell listing: adjust quantity with buttons (or via chat),
 * set the price, and see the current market price for that item.
 */
public final class SellMenu implements InventoryHolder {

    public static final int SLOT_ITEM = 13;
    public static final int SLOT_DEC_10 = 10;
    public static final int SLOT_DEC_1 = 11;
    public static final int SLOT_INC_1 = 15;
    public static final int SLOT_INC_10 = 16;
    public static final int SLOT_ALL = 9;
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
        inventory = Bukkit.createInventory(this, 27, Component.text("Sell Item"));

        int available = Items.count(player, session.template());
        if (available > 0) {
            session.amount(Math.max(1, Math.min(session.amount(), available)));
        }
        MarketStats stats = marketStats();

        inventory.setItem(SLOT_ITEM, itemIcon(available, stats));
        inventory.setItem(SLOT_DEC_10, button(Material.RED_STAINED_GLASS_PANE, "-10"));
        inventory.setItem(SLOT_DEC_1, button(Material.PINK_STAINED_GLASS_PANE, "-1"));
        inventory.setItem(SLOT_INC_1, button(Material.LIME_STAINED_GLASS_PANE, "+1"));
        inventory.setItem(SLOT_INC_10, button(Material.GREEN_STAINED_GLASS_PANE, "+10"));
        inventory.setItem(SLOT_ALL, button(Material.HOPPER, "Sell all (" + available + ")"));
        inventory.setItem(SLOT_TYPE_AMOUNT, button(Material.OAK_SIGN, "Type amount in chat"));
        inventory.setItem(SLOT_SET_PRICE, priceButton());
        inventory.setItem(SLOT_USE_MARKET, marketButton(stats));
        inventory.setItem(SLOT_CONFIRM, button(Material.EMERALD_BLOCK, "Confirm listing"));
        inventory.setItem(SLOT_CANCEL, button(Material.BARRIER, "Cancel"));
    }

    private MarketStats marketStats() {
        try {
            return plugin.database().marketStats(session.label());
        } catch (Exception e) {
            return MarketStats.EMPTY;
        }
    }

    private ItemStack itemIcon(int available, MarketStats stats) {
        ItemStack icon = session.template().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), session.amount())));

        ItemMeta meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(line("Amount to sell: " + session.amount(), NamedTextColor.WHITE));
        if (session.pricePerItem() > 0) {
            lore.add(line("Your price each: " + plugin.economy().format(session.pricePerItem()), NamedTextColor.GOLD));
            lore.add(line("Total: " + plugin.economy().format(session.pricePerItem() * session.amount()),
                    NamedTextColor.GOLD));
        } else {
            lore.add(line("Your price each: not set", NamedTextColor.RED));
        }
        lore.add(Component.empty());
        if (stats.hasData()) {
            lore.add(line("Market avg: " + plugin.economy().format(stats.average()), NamedTextColor.AQUA));
            lore.add(line("Market low: " + plugin.economy().format(stats.min()), NamedTextColor.AQUA));
            lore.add(line("(" + stats.count() + " other listings)", NamedTextColor.DARK_AQUA));
        } else {
            lore.add(line("No other listings for this item", NamedTextColor.DARK_AQUA));
        }
        lore.add(Component.empty());
        lore.add(line("You have " + available + " in your inventory", NamedTextColor.GRAY));

        if (meta != null) {
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack priceButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line("Set price (type in chat)", NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(line(session.pricePerItem() > 0
                    ? "Current: " + plugin.economy().format(session.pricePerItem()) + " each"
                    : "Current: not set", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack marketButton(MarketStats stats) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line("Use market price", NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(stats.hasData()
                    ? line("Set to avg: " + plugin.economy().format(stats.average()), NamedTextColor.GRAY)
                    : line("No market data yet", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line(name, NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
