package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.util.Gui;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Prompts the player to click an item in their own inventory to choose what to buy.
 * Lets a buy offer be created for any item the player has a sample of, even when the
 * market is empty. The selection is handled by the click listener (a bottom-inventory click).
 */
public final class BuySelectMenu implements InventoryHolder {

    public static final int SLOT_BACK = 0;
    public static final int SLOT_INFO = 4;

    private final Grandexchange plugin;
    private Inventory inventory;

    public BuySelectMenu(Grandexchange plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 9, net.kyori.adventure.text.Component.text("Buy — pick an item"));
        inventory.setItem(SLOT_BACK, Gui.button(Material.ARROW, "Back", null));
        inventory.setItem(SLOT_INFO, Gui.button(Material.PAPER, "Click an item to buy", List.of(
                Gui.line("Click any item in your inventory below", NamedTextColor.GRAY),
                Gui.line("to set up a buy offer for it.", NamedTextColor.GRAY))));
        player.openInventory(inventory);
    }
}
