package com.github.petterj345.grandexchange.citizens;

import net.citizensnpcs.api.trait.Trait;

/**
 * Marker trait. An NPC carrying this trait opens the Grand Exchange when right-clicked.
 * Added in-game with: /npc select  then  /trait grandexchange
 *
 * Citizens persists the trait with the NPC, so it survives restarts with no extra config.
 */
public final class GrandExchangeTrait extends Trait {

    public GrandExchangeTrait() {
        super("grandexchange");
    }
}
