package com.github.petterj345.grandexchange.service;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.engine.MatchResult;
import com.github.petterj345.grandexchange.gui.BuyMenu;
import com.github.petterj345.grandexchange.gui.CollectionMenu;
import com.github.petterj345.grandexchange.gui.MarketMenu;
import com.github.petterj345.grandexchange.gui.MyOffersMenu;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.gui.SellSelectMenu;
import com.github.petterj345.grandexchange.input.BuySession;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.CollectionEntry;
import com.github.petterj345.grandexchange.storage.MarketSummary;
import com.github.petterj345.grandexchange.storage.Offer;
import com.github.petterj345.grandexchange.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Central orchestration for every exchange screen and action. The Buy window (market /
 * prices) and Sell window (inventory picker) are kept fully separate so their sessions
 * never overlap. Every screen transition is deferred by one tick because opening an
 * inventory inside an InventoryClickEvent causes client desync ("ghost item") glitches.
 */
public final class ExchangeService {

    private final Grandexchange plugin;

    public ExchangeService(Grandexchange plugin) {
        this.plugin = plugin;
    }

    private void later(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    // ---------------------------------------------------------------- screens

    /** The Buy window: the market / price list. Clicking an item starts a buy offer. */
    public void openMarket(Player player) {
        clearSessions(player.getUniqueId());
        later(() -> {
            try {
                new MarketMenu(plugin, plugin.database().marketSummaries(), 0).open(player);
            } catch (Exception e) {
                error(player, e);
            }
        });
    }

    public void openMyOffers(Player player) {
        plugin.input().clearPrompt(player.getUniqueId());
        later(() -> {
            try {
                List<Offer> offers = plugin.database().offersByOwner(player.getUniqueId());
                new MyOffersMenu(plugin, offers, 0).open(player);
            } catch (Exception e) {
                error(player, e);
            }
        });
    }

    public void openCollection(Player player) {
        plugin.input().clearPrompt(player.getUniqueId());
        later(() -> {
            try {
                List<CollectionEntry> entries = plugin.database().collectionByOwner(player.getUniqueId());
                new CollectionMenu(plugin, entries, 0).open(player);
            } catch (Exception e) {
                error(player, e);
            }
        });
    }

    /** The Sell window entry: pick an item from your own inventory. */
    public void openSellSelect(Player player) {
        clearSessions(player.getUniqueId());
        later(() -> new SellSelectMenu(plugin).open(player));
    }

    // ------------------------------------------------------------- buy window

    public void openBuy(Player player, ItemStack source) {
        ItemStack template = source.clone();
        template.setAmount(1);
        double defaultPrice = 0;
        MarketSummary summary = summary(template.getType().name());
        if (summary != null && summary.hasAsk()) {
            defaultPrice = summary.lowestAsk();
        }
        BuySession session = new BuySession(template, 1, defaultPrice);
        // Entering the buy flow clears any half-finished sell flow.
        plugin.input().clearPrompt(player.getUniqueId());
        plugin.input().clearSell(player.getUniqueId());
        plugin.input().setBuy(player.getUniqueId(), session);
        later(() -> new BuyMenu(plugin, session).open(player));
    }

    public void confirmBuy(Player player, BuySession session) {
        if (session.maxPricePerItem() <= 0) {
            player.sendMessage(msg("Set a max price first.", NamedTextColor.RED));
            return;
        }
        if (session.amount() <= 0) {
            player.sendMessage(msg("Set a quantity first.", NamedTextColor.RED));
            return;
        }
        if (atOfferCap(player)) {
            return;
        }
        MatchResult result = plugin.engine().placeBuyOffer(player, session.template(),
                session.amount(), session.maxPricePerItem());
        if (!result.ok()) {
            player.sendMessage(msg(result.error(), NamedTextColor.RED));
            return;
        }
        plugin.input().clearBuy(player.getUniqueId());

        String item = session.template().getType().name();
        if (result.filled() > 0 && result.resting() > 0) {
            player.sendMessage(msg("Bought " + result.filled() + "x " + item + " for "
                    + plugin.economy().format(result.coins()) + " (added to your inventory). "
                    + result.resting() + " still wanted at your price.", NamedTextColor.GREEN));
        } else if (result.filled() > 0) {
            player.sendMessage(msg("Bought " + result.filled() + "x " + item + " for "
                    + plugin.economy().format(result.coins()) + " — added to your inventory.",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(msg("Buy offer placed: " + result.resting() + "x " + item
                    + " wanted at up to " + plugin.economy().format(session.maxPricePerItem())
                    + " each. You'll receive them automatically as sellers appear.", NamedTextColor.YELLOW));
        }
        openMarket(player);
    }

    // ------------------------------------------------------------ sell window

    public void openSell(Player player, ItemStack source) {
        ItemStack template = source.clone();
        template.setAmount(1);
        int available = Items.count(player, template);
        if (available <= 0) {
            player.sendMessage(msg("You need the item in your inventory to sell it.", NamedTextColor.RED));
            return;
        }
        double defaultPrice = 0;
        MarketSummary summary = summary(template.getType().name());
        if (summary != null && summary.hasBid()) {
            defaultPrice = summary.highestBid();
        } else if (summary != null && summary.hasAsk()) {
            defaultPrice = summary.lowestAsk();
        }
        int startAmount = Math.max(1, Math.min(source.getAmount(), available));
        SellSession session = new SellSession(template, startAmount, defaultPrice);
        // Entering the sell flow clears any half-finished buy flow.
        plugin.input().clearPrompt(player.getUniqueId());
        plugin.input().clearBuy(player.getUniqueId());
        plugin.input().setSell(player.getUniqueId(), session);
        later(() -> new SellMenu(plugin, session).open(player));
    }

    public void confirmSell(Player player, SellSession session) {
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
        if (atOfferCap(player)) {
            return;
        }
        MatchResult result = plugin.engine().placeSellOffer(player, session.template(), amount,
                session.pricePerItem());
        if (!result.ok()) {
            player.sendMessage(msg(result.error(), NamedTextColor.RED));
            return;
        }
        plugin.input().clearSell(player.getUniqueId());

        String item = session.template().getType().name();
        if (result.filled() > 0 && result.resting() > 0) {
            player.sendMessage(msg("Sold " + result.filled() + "x " + item + " for "
                    + plugin.economy().format(result.coins()) + " (paid to your balance). "
                    + result.resting() + " still listed.", NamedTextColor.GREEN));
        } else if (result.filled() > 0) {
            player.sendMessage(msg("Sold " + result.filled() + "x " + item + " for "
                    + plugin.economy().format(result.coins()) + " — paid to your balance.",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(msg("Sell offer placed: " + result.resting() + "x " + item
                    + " at " + plugin.economy().format(session.pricePerItem()) + " each.",
                    NamedTextColor.YELLOW));
        }
        openMarket(player);
    }

    // -------------------------------------------------------- cancel / collect

    public void cancelOffer(Player player, long offerId) {
        try {
            Offer offer = plugin.database().offerById(offerId);
            if (offer == null) {
                openMyOffers(player);
                return;
            }
            if (!offer.ownerUuid().equals(player.getUniqueId())) {
                player.sendMessage(msg("That isn't your offer.", NamedTextColor.RED));
                return;
            }
            plugin.engine().cancel(player, offer);
            player.sendMessage(msg("Offer cancelled and the remainder returned.", NamedTextColor.GREEN));
            openMyOffers(player);
        } catch (Exception e) {
            error(player, e);
        }
    }

    public void collectEntry(Player player, long entryId) {
        try {
            CollectionEntry entry = find(plugin.database().collectionByOwner(player.getUniqueId()), entryId);
            if (entry != null) {
                deliver(player, entry);
                plugin.database().deleteCollection(entry.id());
            }
            openCollection(player);
        } catch (Exception e) {
            error(player, e);
        }
    }

    public void collectAll(Player player) {
        try {
            List<CollectionEntry> entries = plugin.database().collectionByOwner(player.getUniqueId());
            double coins = 0;
            int items = 0;
            for (CollectionEntry entry : entries) {
                if (entry.isCoins()) {
                    coins += entry.coins();
                } else {
                    items += entry.quantity();
                }
                deliver(player, entry);
                plugin.database().deleteCollection(entry.id());
            }
            if (!entries.isEmpty()) {
                player.sendMessage(msg("Collected " + items + " items and "
                        + plugin.economy().format(coins) + ".", NamedTextColor.GREEN));
            }
            openCollection(player);
        } catch (Exception e) {
            error(player, e);
        }
    }

    // ------------------------------------------------------------------ utils

    private void clearSessions(UUID player) {
        plugin.input().clearPrompt(player);
        plugin.input().clearBuy(player);
        plugin.input().clearSell(player);
    }

    private void deliver(Player player, CollectionEntry entry) {
        if (entry.isCoins()) {
            plugin.economy().deposit(player, entry.coins());
        } else {
            Items.give(player, entry.item(), entry.quantity());
        }
    }

    private boolean atOfferCap(Player player) {
        try {
            if (plugin.database().offersByOwner(player.getUniqueId()).size() >= plugin.maxListingsPerPlayer()) {
                player.sendMessage(msg("You've reached the max of " + plugin.maxListingsPerPlayer()
                        + " active offers.", NamedTextColor.RED));
                return true;
            }
        } catch (Exception e) {
            error(player, e);
            return true;
        }
        return false;
    }

    private MarketSummary summary(String label) {
        try {
            return plugin.database().marketSummary(label);
        } catch (Exception e) {
            return null;
        }
    }

    private static CollectionEntry find(List<CollectionEntry> entries, long id) {
        for (CollectionEntry entry : entries) {
            if (entry.id() == id) {
                return entry;
            }
        }
        return null;
    }

    private void error(Player player, Exception e) {
        player.sendMessage(msg("Something went wrong: " + e.getMessage(), NamedTextColor.RED));
    }

    private static Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
