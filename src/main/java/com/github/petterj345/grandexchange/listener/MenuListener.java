package com.github.petterj345.grandexchange.listener;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.ExchangeMenu;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.gui.SellSelectMenu;
import com.github.petterj345.grandexchange.input.Prompt;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.Listing;
import com.github.petterj345.grandexchange.storage.MarketStats;
import com.github.petterj345.grandexchange.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Handles clicks in the browse/mine GUI and the sell GUI. Both menus are read-only,
 * so every click is cancelled and then interpreted as an action.
 */
public final class MenuListener implements Listener {

    private final Grandexchange plugin;

    public MenuListener(Grandexchange plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ExchangeMenu || holder instanceof SellMenu
                || holder instanceof SellSelectMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof ExchangeMenu) && !(holder instanceof SellMenu)
                && !(holder instanceof SellSelectMenu)) {
            return;
        }

        // Every one of our menus is read-only; cancel before doing anything.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // The sell-picker reacts to clicks in the player's own (bottom) inventory.
        if (holder instanceof SellSelectMenu select) {
            handleSelectClick(player, select, event, top);
            return;
        }

        // The other menus only react to clicks inside the top inventory.
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }
        if (holder instanceof ExchangeMenu menu) {
            handleExchangeClick(player, menu, event.getSlot());
        } else {
            handleSellClick(player, (SellMenu) holder, event.getSlot());
        }
    }

    private void handleSelectClick(Player player, SellSelectMenu menu, InventoryClickEvent event, Inventory top) {
        if (event.getClickedInventory() == top) {
            if (event.getSlot() == SellSelectMenu.SLOT_BACK) {
                plugin.exchange().openBrowse(player);
            }
            return;
        }
        // Click in the player's own inventory: that's the item to sell.
        org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        plugin.exchange().openSell(player, clicked);
    }

    private void handleExchangeClick(Player player, ExchangeMenu menu, int slot) {
        switch (slot) {
            case ExchangeMenu.SLOT_PREV -> {
                menu.reopen(player, menu.page() - 1);
                return;
            }
            case ExchangeMenu.SLOT_NEXT -> {
                menu.reopen(player, menu.page() + 1);
                return;
            }
            case ExchangeMenu.SLOT_BROWSE_TAB -> {
                plugin.exchange().openBrowse(player);
                return;
            }
            case ExchangeMenu.SLOT_SELL -> {
                plugin.exchange().openSellSelect(player);
                return;
            }
            case ExchangeMenu.SLOT_MINE_TAB -> {
                plugin.exchange().openMine(player);
                return;
            }
            case ExchangeMenu.SLOT_INFO -> {
                return;
            }
            default -> {
                // not a nav slot — fall through to listing handling
            }
        }
        Long listingId = menu.listingAt(slot);
        if (listingId == null) {
            return;
        }
        try {
            Listing listing = plugin.database().byId(listingId);
            if (listing == null || listing.quantity() <= 0) {
                player.sendMessage(msg("That listing is no longer available.", NamedTextColor.RED));
                return;
            }
            if (menu.mode() == ExchangeMenu.Mode.MINE) {
                cancelListing(player, listing);
            } else {
                startPurchase(player, listing);
            }
        } catch (Exception e) {
            player.sendMessage(msg("Something went wrong: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void cancelListing(Player player, Listing listing) throws Exception {
        if (!listing.sellerUuid().equals(player.getUniqueId())) {
            player.sendMessage(msg("That isn't your listing.", NamedTextColor.RED));
            return;
        }
        int remaining = listing.quantity();
        plugin.database().delete(listing.id());
        Items.give(player, listing.item(), remaining);
        player.sendMessage(msg("Listing cancelled. Returned " + remaining + "x "
                + listing.item().getType().name() + ".", NamedTextColor.GREEN));
        plugin.exchange().openMine(player);
    }

    private void startPurchase(Player player, Listing listing) {
        if (listing.sellerUuid().equals(player.getUniqueId())) {
            player.sendMessage(msg("You can't buy your own listing. Use /ge mine to cancel it.",
                    NamedTextColor.RED));
            return;
        }
        plugin.input().setPrompt(player.getUniqueId(), Prompt.buy(listing.id()));
        player.closeInventory();
        player.sendMessage(msg("How many do you want to buy? (max " + listing.quantity()
                + ") Type a number in chat, or 'cancel'.", NamedTextColor.YELLOW));
    }

    private void handleSellClick(Player player, SellMenu menu, int slot) {
        SellSession session = menu.session();
        switch (slot) {
            case SellMenu.SLOT_DEC_10 -> {
                session.amount(Math.max(1, session.amount() - 10));
                menu.open(player);
            }
            case SellMenu.SLOT_DEC_1 -> {
                session.amount(Math.max(1, session.amount() - 1));
                menu.open(player);
            }
            case SellMenu.SLOT_INC_1 -> {
                session.amount(session.amount() + 1);
                menu.open(player);
            }
            case SellMenu.SLOT_INC_10 -> {
                session.amount(session.amount() + 10);
                menu.open(player);
            }
            case SellMenu.SLOT_ALL -> {
                int available = Items.count(player, session.template());
                session.amount(Math.max(1, available));
                menu.open(player);
            }
            case SellMenu.SLOT_TYPE_AMOUNT -> {
                plugin.input().setPrompt(player.getUniqueId(), Prompt.sellQuantity());
                player.closeInventory();
                player.sendMessage(msg("Type how many you want to sell (or 'cancel').", NamedTextColor.YELLOW));
            }
            case SellMenu.SLOT_SET_PRICE -> {
                plugin.input().setPrompt(player.getUniqueId(), Prompt.sellPrice());
                player.closeInventory();
                player.sendMessage(msg("Type your price per item (or 'cancel').", NamedTextColor.YELLOW));
            }
            case SellMenu.SLOT_USE_MARKET -> {
                applyMarketPrice(player, session);
                menu.open(player);
            }
            case SellMenu.SLOT_CONFIRM -> confirmSell(player, session);
            case SellMenu.SLOT_CANCEL -> {
                plugin.input().clearSell(player.getUniqueId());
                player.sendMessage(msg("Sell cancelled.", NamedTextColor.GRAY));
                plugin.exchange().openBrowse(player);
            }
            default -> {
                // ignore decorative / empty slots
            }
        }
    }

    private void applyMarketPrice(Player player, SellSession session) {
        try {
            MarketStats stats = plugin.database().marketStats(session.label());
            if (stats.hasData()) {
                session.pricePerItem(stats.average());
            } else {
                player.sendMessage(msg("No market data for this item yet — set a price manually.",
                        NamedTextColor.GRAY));
            }
        } catch (Exception e) {
            player.sendMessage(msg("Couldn't read market price: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void confirmSell(Player player, SellSession session) {
        if (session.pricePerItem() <= 0) {
            player.sendMessage(msg("Set a price first.", NamedTextColor.RED));
            return;
        }
        int available = Items.count(player, session.template());
        int amount = Math.min(session.amount(), available);
        if (amount <= 0) {
            player.sendMessage(msg("You don't have any of that item to sell.", NamedTextColor.RED));
            return;
        }
        try {
            int active = plugin.database().bySeller(player.getUniqueId()).size();
            if (active >= plugin.maxListingsPerPlayer()) {
                player.sendMessage(msg("You've reached the max of " + plugin.maxListingsPerPlayer()
                        + " active listings.", NamedTextColor.RED));
                return;
            }
            if (!Items.removeMatching(player, session.template(), amount)) {
                player.sendMessage(msg("Couldn't take the items from your inventory.", NamedTextColor.RED));
                return;
            }
            Listing listing = plugin.database().insert(player.getUniqueId(), player.getName(),
                    session.template(), amount, session.pricePerItem(), System.currentTimeMillis());
            plugin.input().clearSell(player.getUniqueId());
            player.sendMessage(msg("Listed " + amount + "x " + session.template().getType().name()
                    + " at " + plugin.economy().format(session.pricePerItem())
                    + " each. (listing #" + listing.id() + ")", NamedTextColor.GREEN));
            plugin.exchange().openBrowse(player);
        } catch (Exception e) {
            player.sendMessage(msg("Failed to create listing: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private static Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
