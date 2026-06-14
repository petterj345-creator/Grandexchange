package com.github.petterj345.grandexchange.engine;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.storage.Database;
import com.github.petterj345.grandexchange.storage.Offer;
import com.github.petterj345.grandexchange.storage.OfferSide;
import com.github.petterj345.grandexchange.util.Items;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Places offers and matches them against the resting book.
 *
 * <p>Money and items are escrowed up front: a buyer's coins are withdrawn when the buy
 * offer is placed; a seller's items are removed when the sell offer is placed. Matching
 * never touches a live balance or inventory again — it only moves the already-escrowed
 * value into the counterparties' collection boxes. Every trade executes at the
 * <b>seller's asking price</b>; a buyer who offered more is refunded the difference.
 */
public final class MatchingEngine {

    private final Grandexchange plugin;

    public MatchingEngine(Grandexchange plugin) {
        this.plugin = plugin;
    }

    private Database db() {
        return plugin.database();
    }

    /**
     * Places a buy offer for {@code quantity} of {@code template} at up to {@code maxPrice} each.
     * Reserves {@code quantity * maxPrice} coins from the buyer.
     */
    public MatchResult placeBuyOffer(Player buyer, ItemStack template, int quantity, double maxPrice) {
        double reserve = quantity * maxPrice;
        if (!plugin.economy().has(buyer, reserve)) {
            return MatchResult.fail("You need " + plugin.economy().format(reserve) + " to place that offer.");
        }
        if (!plugin.economy().withdraw(buyer, reserve)) {
            return MatchResult.fail("Payment failed.");
        }

        try {
            long now = System.currentTimeMillis();
            String label = template.getType().name();
            Offer offer = db().insertOffer(buyer.getUniqueId(), buyer.getName(), OfferSide.BUY,
                    template, quantity, maxPrice, reserve, now);

            int remaining = quantity;
            double escrow = reserve;
            int filled = 0;
            double spent = 0;

            List<Offer> sells = db().matchableSells(label, maxPrice);
            for (Offer sell : sells) {
                if (remaining <= 0) {
                    break;
                }
                if (sell.ownerUuid().equals(buyer.getUniqueId())) {
                    continue; // don't fill your own buy with your own sell
                }
                int m = Math.min(remaining, sell.quantity());
                double exec = sell.pricePerItem();

                // Resting seller is paid via their collection box.
                db().addCollectionCoins(sell.ownerUuid(), m * exec, now);
                // Active buyer gets the items (and any price-improvement refund) right now.
                Items.give(buyer, template, m);
                double refund = m * (maxPrice - exec);
                if (refund > 0) {
                    plugin.economy().deposit(buyer, refund);
                }

                int sellRemaining = sell.quantity() - m;
                if (sellRemaining <= 0) {
                    db().deleteOffer(sell.id());
                } else {
                    db().updateOffer(sell.id(), sellRemaining, sell.escrowCoins());
                }

                remaining -= m;
                escrow -= m * maxPrice;
                filled += m;
                spent += m * exec;
            }

            if (remaining <= 0) {
                db().deleteOffer(offer.id());
            } else {
                db().updateOffer(offer.id(), remaining, escrow);
            }
            return MatchResult.ok(filled, spent, Math.max(0, remaining));
        } catch (Exception e) {
            // Refund the reserve if anything went wrong after withdrawing.
            plugin.economy().deposit(buyer, reserve);
            return MatchResult.fail("Something went wrong: " + e.getMessage());
        }
    }

    /**
     * Places a sell offer for {@code quantity} of {@code template} at {@code price} each.
     * Removes the items from the seller's inventory as escrow.
     */
    public MatchResult placeSellOffer(Player seller, ItemStack template, int quantity, double price) {
        if (!Items.removeMatching(seller, template, quantity)) {
            return MatchResult.fail("You don't have " + quantity + " of that item.");
        }
        try {
            long now = System.currentTimeMillis();
            String label = template.getType().name();
            Offer offer = db().insertOffer(seller.getUniqueId(), seller.getName(), OfferSide.SELL,
                    template, quantity, price, 0, now);

            int remaining = quantity;
            int filled = 0;
            double earned = 0;

            List<Offer> buys = db().matchableBuys(label, price);
            for (Offer buy : buys) {
                if (remaining <= 0) {
                    break;
                }
                if (buy.ownerUuid().equals(seller.getUniqueId())) {
                    continue; // don't fill your own sell with your own buy
                }
                int m = Math.min(remaining, buy.quantity());
                double exec = price; // the seller's asking price

                // Active seller is paid right now.
                plugin.economy().deposit(seller, m * exec);
                // Resting buyer receives items (and refund) via their collection box.
                db().addCollectionItem(buy.ownerUuid(), template, m, now);
                double refund = m * (buy.pricePerItem() - exec);
                db().addCollectionCoins(buy.ownerUuid(), refund, now);

                int buyRemaining = buy.quantity() - m;
                double buyEscrow = Math.max(0, buy.escrowCoins() - m * buy.pricePerItem());
                if (buyRemaining <= 0) {
                    db().deleteOffer(buy.id());
                } else {
                    db().updateOffer(buy.id(), buyRemaining, buyEscrow);
                }

                remaining -= m;
                filled += m;
                earned += m * exec;
            }

            if (remaining <= 0) {
                db().deleteOffer(offer.id());
            } else {
                db().updateOffer(offer.id(), remaining, 0);
            }
            return MatchResult.ok(filled, earned, Math.max(0, remaining));
        } catch (Exception e) {
            // Hand the items back if persistence failed after escrow.
            Items.give(seller, template, quantity);
            return MatchResult.fail("Something went wrong: " + e.getMessage());
        }
    }

    /**
     * Cancels a resting offer, returning the unspent escrow to the owner immediately:
     * remaining items for a sell, remaining reserved coins for a buy.
     */
    public void cancel(Player owner, Offer offer) throws Exception {
        db().deleteOffer(offer.id());
        if (offer.side() == OfferSide.SELL) {
            Items.give(owner, offer.item(), offer.quantity());
        } else {
            plugin.economy().deposit(owner, offer.escrowCoins());
        }
    }
}
