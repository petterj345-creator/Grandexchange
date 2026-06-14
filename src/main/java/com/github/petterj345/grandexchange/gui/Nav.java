package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.util.Gui;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/**
 * Shared bottom navigation row for the hub menus (Market, My Offers, Collection):
 * page arrows plus tabs that jump between screens.
 */
public final class Nav {

    public static final int PREV = 45;
    public static final int MARKET = 47;
    public static final int SELL = 48;
    public static final int MY_OFFERS = 49;
    public static final int COLLECTION = 50;
    public static final int NEXT = 53;

    private Nav() {
    }

    public static boolean isTab(int slot) {
        return slot == MARKET || slot == SELL || slot == MY_OFFERS || slot == COLLECTION;
    }

    public static void render(Inventory inv, boolean hasPrev, boolean hasNext) {
        if (hasPrev) {
            inv.setItem(PREV, Gui.button(Material.ARROW, "Previous page", null));
        }
        if (hasNext) {
            inv.setItem(NEXT, Gui.button(Material.ARROW, "Next page", null));
        }
        inv.setItem(MARKET, Gui.button(Material.EMERALD, "Buy / Prices", null));
        inv.setItem(SELL, Gui.button(Material.HOPPER, "Sell window", null));
        inv.setItem(MY_OFFERS, Gui.button(Material.WRITABLE_BOOK, "My offers", null));
        inv.setItem(COLLECTION, Gui.button(Material.ENDER_CHEST, "Collection box", null));
    }
}
