package com.github.petterj345.grandexchange.storage;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * A resting order. {@link #item()} is a single representative item (amount 1).
 *
 * <p>For a SELL offer the items are escrowed (taken from the seller's inventory) and
 * {@link #escrowCoins()} is 0. For a BUY offer coins are escrowed: {@code escrowCoins}
 * holds {@code quantity * pricePerItem} for the still-unfilled units.
 */
public final class Offer {

    private final long id;
    private final UUID ownerUuid;
    private final String ownerName;
    private final OfferSide side;
    private final ItemStack item;
    private int quantity;
    private final double pricePerItem;
    private double escrowCoins;
    private final long createdAt;

    public Offer(long id, UUID ownerUuid, String ownerName, OfferSide side, ItemStack item,
                 int quantity, double pricePerItem, double escrowCoins, long createdAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.side = side;
        this.item = item;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
        this.escrowCoins = escrowCoins;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public OfferSide side() {
        return side;
    }

    public ItemStack item() {
        return item;
    }

    public String label() {
        return item.getType().name();
    }

    public int quantity() {
        return quantity;
    }

    public void quantity(int quantity) {
        this.quantity = quantity;
    }

    public double pricePerItem() {
        return pricePerItem;
    }

    public double escrowCoins() {
        return escrowCoins;
    }

    public void escrowCoins(double escrowCoins) {
        this.escrowCoins = escrowCoins;
    }

    public long createdAt() {
        return createdAt;
    }
}
