package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.util.Gui;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/**
 * Shared bottom navigation row for the hub menus. Buy and Sell are separate windows:
 * Buy lists items for sale (sell orders), Sell lists items wanted (buy orders).
 */
public final class Nav {

    public static final int PREV = 45;
    public static final int BUY = 47;
    public static final int SELL = 48;
    public static final int MY_OFFERS = 49;
    public static final int COLLECTION = 50;
    public static final int NEXT = 53;

    private Nav() {
    }

    public static boolean isTab(int slot) {
        return slot == BUY || slot == SELL || slot == MY_OFFERS || slot == COLLECTION;
    }

    public static void render(Inventory inv, boolean hasPrev, boolean hasNext) {
        if (hasPrev) {
            inv.setItem(PREV, Gui.button(Material.ARROW, "Previous page", null));
        }
        if (hasNext) {
            inv.setItem(NEXT, Gui.button(Material.ARROW, "Next page", null));
        }
        inv.setItem(BUY, Gui.button(Material.EMERALD, "Buy (sell orders)", null));
        inv.setItem(SELL, Gui.button(Material.HOPPER, "Sell (buy orders)", null));
        inv.setItem(MY_OFFERS, Gui.button(Material.WRITABLE_BOOK, "My offers", null));
        inv.setItem(COLLECTION, Gui.button(Material.ENDER_CHEST, "Collection box", null));
    }
}
