package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
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
 * Detail screen for one item: shows its market prices and offers Buy / Sell actions.
 */
public final class ItemDetailMenu implements InventoryHolder {

    public static final int SLOT_ITEM = 13;
    public static final int SLOT_BUY = 11;
    public static final int SLOT_SELL = 15;
    public static final int SLOT_BACK = 22;

    private final Grandexchange plugin;
    private final ItemStack template;
    private Inventory inventory;

    public ItemDetailMenu(Grandexchange plugin, ItemStack template) {
        this.plugin = plugin;
        this.template = template;
    }

    public ItemStack template() {
        return template;
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
        inventory = Bukkit.createInventory(this, 27,
                Component.text(prettyName()));

        inventory.setItem(SLOT_ITEM, infoIcon(player));
        inventory.setItem(SLOT_BUY, Gui.button(Material.EMERALD_BLOCK, "Place a buy offer", List.of(
                Gui.line("Choose quantity and your max price.", NamedTextColor.GRAY))));
        inventory.setItem(SLOT_SELL, Gui.button(Material.HOPPER, "Sell this item", List.of(
                Gui.line("You have " + Items.count(player, template) + " to sell.", NamedTextColor.GRAY))));
        inventory.setItem(SLOT_BACK, Gui.button(Material.ARROW, "Back to market", null));
    }

    private ItemStack infoIcon(Player player) {
        ItemStack icon = template.clone();
        icon.setAmount(1);
        MarketSummary summary = summary();
        List<Component> lore = new ArrayList<>();
        if (summary != null && summary.hasAsk()) {
            lore.add(Gui.line("Lowest sell price: " + plugin.economy().format(summary.lowestAsk())
                    + " (" + summary.askQuantity() + " available)", NamedTextColor.GREEN));
        } else {
            lore.add(Gui.line("No sellers right now", NamedTextColor.GRAY));
        }
        if (summary != null && summary.hasBid()) {
            lore.add(Gui.line("Highest buy offer: " + plugin.economy().format(summary.highestBid())
                    + " (" + summary.bidQuantity() + " wanted)", NamedTextColor.GOLD));
        } else {
            lore.add(Gui.line("No buyers right now", NamedTextColor.GRAY));
        }
        return Gui.decorate(icon, lore);
    }

    private MarketSummary summary() {
        try {
            return plugin.database().marketSummary(template.getType().name());
        } catch (Exception e) {
            return null;
        }
    }

    private String prettyName() {
        return template.getType().name();
    }
}
