package com.github.petterj345.grandexchange.service;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.ExchangeMenu;
import com.github.petterj345.grandexchange.storage.Listing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Shared entry points into the exchange UI, used by both the /ge command and the
 * Citizens NPC integration so they behave identically.
 */
public final class ExchangeService {

    private final Grandexchange plugin;

    public ExchangeService(Grandexchange plugin) {
        this.plugin = plugin;
    }

    /** Opens the browse-and-buy GUI for the player. */
    public void openBrowse(Player player) {
        try {
            List<Listing> listings = plugin.database().all();
            if (listings.isEmpty()) {
                player.sendMessage(Component.text(
                        "The Grand Exchange is empty. Use /ge sell to list something!",
                        NamedTextColor.GRAY));
                return;
            }
            new ExchangeMenu(plugin, ExchangeMenu.Mode.BROWSE, listings, 0).open(player);
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to open the exchange: " + e.getMessage(),
                    NamedTextColor.RED));
        }
    }
}
