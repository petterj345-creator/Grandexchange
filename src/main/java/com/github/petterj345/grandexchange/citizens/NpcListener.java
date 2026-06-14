package com.github.petterj345.grandexchange.citizens;

import com.github.petterj345.grandexchange.Grandexchange;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Opens the Grand Exchange when a player right-clicks an NPC that has the
 * {@link GrandExchangeTrait}. {@link NPCRightClickEvent} fires on the main thread,
 * so opening the inventory here is safe.
 */
public final class NpcListener implements Listener {

    private final Grandexchange plugin;

    public NpcListener(Grandexchange plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        if (!event.getNPC().hasTrait(GrandExchangeTrait.class)) {
            return;
        }
        plugin.exchange().openBrowse(event.getClicker());
    }
}
