package com.github.petterj345.grandexchange.engine;

/**
 * Outcome of placing an offer.
 *
 * @param ok      whether the offer was accepted
 * @param filled  units matched immediately
 * @param coins   coins moved: for a buy this is the amount spent on fills; for a sell, earned
 * @param resting units left resting on the book after immediate matching
 * @param error   human-readable reason when {@code ok} is false
 */
public record MatchResult(boolean ok, int filled, double coins, int resting, String error) {

    public static MatchResult fail(String error) {
        return new MatchResult(false, 0, 0, 0, error);
    }

    public static MatchResult ok(int filled, double coins, int resting) {
        return new MatchResult(true, filled, coins, resting, null);
    }
}
