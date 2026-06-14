package com.github.petterj345.grandexchange.listener;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.BuyMenu;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.input.BuySession;
import com.github.petterj345.grandexchange.input.Prompt;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.util.Items;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Captures a player's next chat line when they're answering a quantity/price prompt
 * for a buy or sell offer, then updates the session and reopens the menu (main thread).
 */
public final class ChatListener implements Listener {

    private final Grandexchange plugin;

    public ChatListener(Grandexchange plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = plugin.input().prompt(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        plugin.input().clearPrompt(player.getUniqueId());

        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> handle(player, prompt, message));
    }

    private void handle(Player player, Prompt prompt, String message) {
        switch (prompt.type()) {
            case BUY_QUANTITY -> buyQuantity(player, message);
            case BUY_PRICE -> buyPrice(player, message);
            case SELL_QUANTITY -> sellQuantity(player, message);
            case SELL_PRICE -> sellPrice(player, message);
        }
    }

    // -------------------------------------------------------------------- buy

    private void buyQuantity(Player player, String message) {
        BuySession session = plugin.input().buy(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!message.equalsIgnoreCase("cancel")) {
            Integer amount = parseInt(player, message);
            if (amount != null) {
                session.amount(Math.max(1, amount));
            }
        }
        new BuyMenu(plugin, session).open(player);
    }

    private void buyPrice(Player player, String message) {
        BuySession session = plugin.input().buy(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!message.equalsIgnoreCase("cancel")) {
            Double price = parsePrice(player, message);
            if (price != null) {
                session.maxPricePerItem(price);
            }
        }
        new BuyMenu(plugin, session).open(player);
    }

    // ------------------------------------------------------------------- sell

    private void sellQuantity(Player player, String message) {
        SellSession session = plugin.input().sell(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!message.equalsIgnoreCase("cancel")) {
            Integer amount = parseInt(player, message);
            if (amount != null) {
                int available = Items.count(player, session.template());
                int wanted = Math.max(1, amount);
                if (available > 0 && wanted > available) {
                    wanted = available;
                    player.sendMessage(msg("You only have " + available + " — using that.", NamedTextColor.GRAY));
                }
                session.amount(wanted);
            }
        }
        new SellMenu(plugin, session).open(player);
    }

    private void sellPrice(Player player, String message) {
        SellSession session = plugin.input().sell(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!message.equalsIgnoreCase("cancel")) {
            Double price = parsePrice(player, message);
            if (price != null) {
                session.pricePerItem(price);
            }
        }
        new SellMenu(plugin, session).open(player);
    }

    // ------------------------------------------------------------------ parse

    private Integer parseInt(Player player, String message) {
        try {
            return Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage(msg("That wasn't a whole number.", NamedTextColor.RED));
            return null;
        }
    }

    private Double parsePrice(Player player, String message) {
        try {
            double price = Double.parseDouble(message);
            if (price <= 0) {
                player.sendMessage(msg("Price must be positive.", NamedTextColor.RED));
                return null;
            }
            return price;
        } catch (NumberFormatException e) {
            player.sendMessage(msg("That wasn't a valid number.", NamedTextColor.RED));
            return null;
        }
    }

    private static Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
