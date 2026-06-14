package com.github.petterj345.grandexchange.storage;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * A single marketplace listing. {@link #item()} is a single representative item (amount 1);
 * {@link #quantity()} is how many are still available.
 */
public final class Listing {

    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private int quantity;
    private final double pricePerItem;
    private final long createdAt;

    public Listing(long id, UUID sellerUuid, String sellerName, ItemStack item,
                   int quantity, double pricePerItem, long createdAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public UUID sellerUuid() {
        return sellerUuid;
    }

    public String sellerName() {
        return sellerName;
    }

    public ItemStack item() {
        return item;
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

    public long createdAt() {
        return createdAt;
    }

    public String label() {
        return item.getType().name();
    }
}
