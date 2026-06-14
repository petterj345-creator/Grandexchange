package com.github.petterj345.grandexchange.service;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.ExchangeMenu;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.gui.SellSelectMenu;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.Listing;
import com.github.petterj345.grandexchange.storage.MarketStats;
import com.github.petterj345.grandexchange.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Shared entry points into the exchange UI, used by the /ge command, the GUI nav tabs,
 * and the Citizens NPC integration so they all behave identically. Every screen is
 * reachable from every other screen, so the GUI alone can do everything.
 */
public final class ExchangeService {

    private final Grandexchange plugin;

    public ExchangeService(Grandexchange plugin) {
        this.plugin = plugin;
    }

    /** Opens the browse-and-buy GUI. Opens even when empty so the Sell tab stays reachable. */
    public void openBrowse(Player player) {
        try {
            List<Listing> listings = plugin.database().all();
            new ExchangeMenu(plugin, ExchangeMenu.Mode.BROWSE, listings, 0).open(player);
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to open the exchange: " + e.getMessage(),
                    NamedTextColor.RED));
        }
    }

    /** Opens the player's own listings (to cancel/reclaim). Opens even when empty. */
    public void openMine(Player player) {
        try {
            List<Listing> listings = plugin.database().bySeller(player.getUniqueId());
            new ExchangeMenu(plugin, ExchangeMenu.Mode.MINE, listings, 0).open(player);
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to open your listings: " + e.getMessage(),
                    NamedTextColor.RED));
        }
    }

    /** Opens the "click an item in your inventory to sell" picker. */
    public void openSellSelect(Player player) {
        new SellSelectMenu(plugin).open(player);
    }

    /** Starts the sell flow for the given item (a copy is used as the listing template). */
    public void openSell(Player player, ItemStack source) {
        ItemStack template = source.clone();
        template.setAmount(1);

        double defaultPrice = 0;
        try {
            MarketStats stats = plugin.database().marketStats(template.getType().name());
            if (stats.hasData()) {
                defaultPrice = stats.average();
            }
        } catch (Exception ignored) {
            // fall back to an unset price
        }

        int available = Items.count(player, template);
        int startAmount = Math.max(1, Math.min(source.getAmount(), Math.max(1, available)));
        SellSession session = new SellSession(template, startAmount, defaultPrice);
        plugin.input().setSell(player.getUniqueId(), session);
        new SellMenu(plugin, session).open(player);
    }
}
