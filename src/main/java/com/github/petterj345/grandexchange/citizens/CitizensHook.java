package com.github.petterj345.grandexchange.citizens;

import com.github.petterj345.grandexchange.Grandexchange;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;

/**
 * Isolates all direct references to the Citizens API. This class is only loaded when
 * Citizens is present, so the plugin runs fine on servers without Citizens.
 */
public final class CitizensHook {

    private CitizensHook() {
    }

    public static void enable(Grandexchange plugin) {
        CitizensAPI.getTraitFactory().registerTrait(
                TraitInfo.create(GrandExchangeTrait.class).withName("grandexchange"));
        plugin.getServer().getPluginManager().registerEvents(new NpcListener(plugin), plugin);
    }
}
