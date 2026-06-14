package com.github.petterj345.grandexchange.storage;

import org.bukkit.inventory.ItemStack;

/**
 * Aggregated order-book state for one item: the best (lowest) sell ask and total quantity
 * available, and the best (highest) buy offer and total quantity wanted.
 */
public record MarketSummary(String label, ItemStack item,
                            double lowestAsk, int askQuantity,
                            double highestBid, int bidQuantity) {

    public boolean hasAsk() {
        return askQuantity > 0;
    }

    public boolean hasBid() {
        return bidQuantity > 0;
    }
}
