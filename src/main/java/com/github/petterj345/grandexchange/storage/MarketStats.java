package com.github.petterj345.grandexchange.storage;

/**
 * Aggregated pricing across all active listings of one item type:
 * how many listings exist, the average price each, and the lowest price each.
 */
public record MarketStats(int count, double average, double min) {

    public static final MarketStats EMPTY = new MarketStats(0, 0, 0);

    public boolean hasData() {
        return count > 0;
    }
}
