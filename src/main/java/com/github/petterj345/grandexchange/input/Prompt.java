package com.github.petterj345.grandexchange.input;

/**
 * A pending chat prompt. {@code listingId} is only meaningful for {@link PromptType#BUY_QUANTITY}.
 */
public record Prompt(PromptType type, long listingId) {

    public static Prompt buy(long listingId) {
        return new Prompt(PromptType.BUY_QUANTITY, listingId);
    }

    public static Prompt sellQuantity() {
        return new Prompt(PromptType.SELL_QUANTITY, 0);
    }

    public static Prompt sellPrice() {
        return new Prompt(PromptType.SELL_PRICE, 0);
    }
}
