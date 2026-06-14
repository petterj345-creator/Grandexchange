package com.github.petterj345.grandexchange.input;

import org.bukkit.inventory.ItemStack;

/**
 * Mutable state for a player composing a sell listing in the sell GUI.
 * {@code template} is a single representative item (amount 1).
 */
public final class SellSession {

    private final ItemStack template;
    private int amount;
    private double pricePerItem;

    public SellSession(ItemStack template, int amount, double pricePerItem) {
        this.template = template;
        this.amount = amount;
        this.pricePerItem = pricePerItem;
    }

    public ItemStack template() {
        return template;
    }

    public String label() {
        return template.getType().name();
    }

    public int amount() {
        return amount;
    }

    public void amount(int amount) {
        this.amount = amount;
    }

    public double pricePerItem() {
        return pricePerItem;
    }

    public void pricePerItem(double pricePerItem) {
        this.pricePerItem = pricePerItem;
    }
}
