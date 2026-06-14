package com.github.petterj345.grandexchange.listener;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.input.Prompt;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.Listing;
import com.github.petterj345.grandexchange.util.Items;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Captures a player's next chat line when they're answering a prompt
 * (buy quantity, sell quantity, or sell price) and runs the action on the main thread.
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
            case BUY_QUANTITY -> handleBuy(player, prompt.listingId(), message);
            case SELL_QUANTITY -> handleSellQuantity(player, message);
            case SELL_PRICE -> handleSellPrice(player, message);
        }
    }

    private void handleBuy(Player player, long listingId, String message) {
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(msg("Purchase cancelled.", NamedTextColor.GRAY));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage(msg("That wasn't a whole number. Purchase cancelled.", NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(msg("Amount must be positive. Purchase cancelled.", NamedTextColor.RED));
            return;
        }
        try {
            Listing listing = plugin.database().byId(listingId);
            if (listing == null || listing.quantity() <= 0) {
                player.sendMessage(msg("That listing is no longer available.", NamedTextColor.RED));
                return;
            }
            if (amount > listing.quantity()) {
                player.sendMessage(msg("Only " + listing.quantity() + " available. Purchase cancelled.",
                        NamedTextColor.RED));
                return;
            }
            double total = amount * listing.pricePerItem();
            if (!plugin.economy().has(player, total)) {
                player.sendMessage(msg("You can't afford that — it costs "
                        + plugin.economy().format(total) + ".", NamedTextColor.RED));
                return;
            }

            OfflinePlayer seller = plugin.getServer().getOfflinePlayer(listing.sellerUuid());
            double tax = total * (plugin.taxPercent() / 100.0);
            double sellerGets = total - tax;

            if (!plugin.economy().withdraw(player, total)) {
                player.sendMessage(msg("Payment failed. Purchase cancelled.", NamedTextColor.RED));
                return;
            }
            plugin.economy().deposit(seller, sellerGets);

            int newQuantity = listing.quantity() - amount;
            if (newQuantity <= 0) {
                plugin.database().delete(listing.id());
            } else {
                plugin.database().updateQuantity(listing.id(), newQuantity);
            }

            Items.give(player, listing.item(), amount);
            String itemName = listing.item().getType().name();
            player.sendMessage(msg("Bought " + amount + "x " + itemName + " for "
                    + plugin.economy().format(total) + ".", NamedTextColor.GREEN));

            Player onlineSeller = seller.getPlayer();
            if (onlineSeller != null) {
                onlineSeller.sendMessage(msg(player.getName() + " bought " + amount + "x " + itemName
                        + " for " + plugin.economy().format(sellerGets) + ".", NamedTextColor.GREEN));
            }

            // Drop the player back into the browse hub to keep shopping.
            plugin.exchange().openBrowse(player);
        } catch (Exception e) {
            player.sendMessage(msg("Something went wrong: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleSellQuantity(Player player, String message) {
        SellSession session = plugin.input().sell(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            new SellMenu(plugin, session).open(player);
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage(msg("That wasn't a whole number.", NamedTextColor.RED));
            new SellMenu(plugin, session).open(player);
            return;
        }
        int available = Items.count(player, session.template());
        if (amount < 1) {
            amount = 1;
        }
        if (available > 0 && amount > available) {
            amount = available;
            player.sendMessage(msg("You only have " + available + " — using that.", NamedTextColor.GRAY));
        }
        session.amount(amount);
        new SellMenu(plugin, session).open(player);
    }

    private void handleSellPrice(Player player, String message) {
        SellSession session = plugin.input().sell(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            new SellMenu(plugin, session).open(player);
            return;
        }
        double price;
        try {
            price = Double.parseDouble(message);
        } catch (NumberFormatException e) {
            player.sendMessage(msg("That wasn't a valid number.", NamedTextColor.RED));
            new SellMenu(plugin, session).open(player);
            return;
        }
        if (price <= 0) {
            player.sendMessage(msg("Price must be positive.", NamedTextColor.RED));
            new SellMenu(plugin, session).open(player);
            return;
        }
        session.pricePerItem(price);
        new SellMenu(plugin, session).open(player);
    }

    private static Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
