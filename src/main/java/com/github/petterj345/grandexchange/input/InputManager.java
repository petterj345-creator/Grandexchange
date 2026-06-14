package com.github.petterj345.grandexchange.input;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks, per player, any pending chat prompt and the in-progress buy/sell session.
 * Accessed from both the async chat thread and the main thread, hence concurrent maps.
 */
public final class InputManager {

    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();
    private final Map<UUID, SellSession> sellSessions = new ConcurrentHashMap<>();
    private final Map<UUID, BuySession> buySessions = new ConcurrentHashMap<>();

    public void setPrompt(UUID player, Prompt prompt) {
        prompts.put(player, prompt);
    }

    public Prompt prompt(UUID player) {
        return prompts.get(player);
    }

    public void clearPrompt(UUID player) {
        prompts.remove(player);
    }

    public void setSell(UUID player, SellSession session) {
        sellSessions.put(player, session);
    }

    public SellSession sell(UUID player) {
        return sellSessions.get(player);
    }

    public void clearSell(UUID player) {
        sellSessions.remove(player);
    }

    public void setBuy(UUID player, BuySession session) {
        buySessions.put(player, session);
    }

    public BuySession buy(UUID player) {
        return buySessions.get(player);
    }

    public void clearBuy(UUID player) {
        buySessions.remove(player);
    }
}
