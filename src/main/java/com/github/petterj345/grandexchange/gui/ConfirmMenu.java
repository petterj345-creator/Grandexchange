package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.input.BuySession;
import com.github.petterj345.grandexchange.storage.Offer;
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
import java.util.List;

/**
 * A one-click confirmation for buying directly from a sell order the player clicked in the
 * Buy window. The price and quantity are fixed to that order — the player just Accepts.
 * Composing a custom buy offer with adjustable price/quantity is the separate {@link BuyMenu};
 * selling into a buy order always uses the adjustable {@link SellMenu} (you sell what you hold).
 */
public final class ConfirmMenu implements InventoryHolder {

    public static final int SLOT_ITEM = 13;
    public static final int SLOT_ACCEPT = 21;
    public static final int SLOT_CANCEL = 23;

    private final Grandexchange plugin;
    private final Offer target;
    private final BuySession session;
    private Inventory inventory;

    public ConfirmMenu(Grandexchange plugin, Offer sellOffer, BuySession session) {
        this.plugin = plugin;
        this.target = sellOffer;
        this.session = session;
    }

    public BuySession session() {
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
        inventory = Bukkit.createInventory(this, 27, Component.text("Confirm purchase"));
        inventory.setItem(SLOT_ITEM, icon(player));
        inventory.setItem(SLOT_ACCEPT, Gui.button(Material.EMERALD_BLOCK, "Accept — buy now", null));
        inventory.setItem(SLOT_CANCEL, Gui.button(Material.BARRIER, "Back", null));
    }

    private ItemStack icon(Player player) {
        int qty = session.amount();
        double price = session.maxPricePerItem();
        ItemStack icon = session.template().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), qty)));

        List<Component> lore = new ArrayList<>();
        lore.add(Gui.line("Buy " + qty + " from " + target.ownerName(), NamedTextColor.GREEN));
        lore.add(Gui.line("Price each: " + plugin.economy().format(price), NamedTextColor.GOLD));
        lore.add(Gui.line("Total cost: " + plugin.economy().format(price * qty), NamedTextColor.GOLD));
        lore.add(Component.empty());
        lore.add(Gui.line("Your balance: " + plugin.economy().format(plugin.economy().balanceOf(player)),
                NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Gui.line("Click Accept to buy at this price", NamedTextColor.AQUA));
        return Gui.decorate(icon, lore);
    }
}
