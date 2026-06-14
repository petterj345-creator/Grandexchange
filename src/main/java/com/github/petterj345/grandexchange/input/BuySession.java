package com.github.petterj345.grandexchange.input;

import org.bukkit.inventory.ItemStack;

/**
 * Mutable state for a player composing a buy offer. {@code template} is a single
 * representative item (amount 1); {@code maxPricePerItem} is the most they'll pay each.
 */
public final class BuySession {

    private final ItemStack template;
    private int amount;
    private double maxPricePerItem;

    public BuySession(ItemStack template, int amount, double maxPricePerItem) {
        this.template = template;
        this.amount = amount;
        this.maxPricePerItem = maxPricePerItem;
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

    public double maxPricePerItem() {
        return maxPricePerItem;
    }

    public void maxPricePerItem(double maxPricePerItem) {
        this.maxPricePerItem = maxPricePerItem;
    }
}
