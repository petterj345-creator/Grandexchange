package com.github.petterj345.grandexchange.storage;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * One pending item-bundle or coin payout waiting in a player's collection box.
 * Exactly one of (item, coins) is meaningful: if {@link #item()} is null it's coins.
 */
public final class CollectionEntry {

    private final long id;
    private final UUID ownerUuid;
    private final ItemStack item;
    private final int quantity;
    private final double coins;
    private final long createdAt;

    public CollectionEntry(long id, UUID ownerUuid, ItemStack item, int quantity,
                           double coins, long createdAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.item = item;
        this.quantity = quantity;
        this.coins = coins;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public ItemStack item() {
        return item;
    }

    public int quantity() {
        return quantity;
    }

    public double coins() {
        return coins;
    }

    public long createdAt() {
        return createdAt;
    }

    public boolean isCoins() {
        return item == null;
    }
}
