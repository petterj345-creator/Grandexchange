package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.storage.Offer;
import com.github.petterj345.grandexchange.storage.OfferSide;
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

/** Lists the player's own resting buy and sell offers; clicking one cancels it. */
public final class MyOffersMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;

    private final Grandexchange plugin;
    private final List<Offer> offers;
    private final Map<Integer, Long> slotToOffer = new HashMap<>();
    private int page;
    private Inventory inventory;

    public MyOffersMenu(Grandexchange plugin, List<Offer> offers, int page) {
        this.plugin = plugin;
        this.offers = offers;
        this.page = page;
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil(offers.size() / (double) PER_PAGE));
    }

    public Long offerAt(int slot) {
        return slotToOffer.get(slot);
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
                Component.text("My offers (" + (page + 1) + "/" + totalPages() + ")"));
        slotToOffer.clear();

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, offers.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            Offer offer = offers.get(i);
            inventory.setItem(slot, render(offer));
            slotToOffer.put(slot, offer.id());
            slot++;
        }

        if (offers.isEmpty()) {
            inventory.setItem(22, Gui.button(Material.PAPER, "You have no active offers", List.of(
                    Gui.line("Place buy/sell offers from the Market tab.", NamedTextColor.GRAY))));
        }

        Nav.render(inventory, page > 0, page < totalPages() - 1);
    }

    private ItemStack render(Offer offer) {
        ItemStack icon = offer.item().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), offer.quantity())));

        List<Component> lore = new ArrayList<>();
        if (offer.side() == OfferSide.BUY) {
            lore.add(Gui.line("BUY offer", NamedTextColor.AQUA));
            lore.add(Gui.line("Want: " + offer.quantity() + " @ up to "
                    + plugin.economy().format(offer.pricePerItem()) + " each", NamedTextColor.GRAY));
            lore.add(Gui.line("Reserved: " + plugin.economy().format(offer.escrowCoins()), NamedTextColor.GOLD));
        } else {
            lore.add(Gui.line("SELL offer", NamedTextColor.GREEN));
            lore.add(Gui.line("Selling: " + offer.quantity() + " @ "
                    + plugin.economy().format(offer.pricePerItem()) + " each", NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        lore.add(Gui.line("Click to cancel & reclaim", NamedTextColor.RED));
        return Gui.decorate(icon, lore);
    }
}
