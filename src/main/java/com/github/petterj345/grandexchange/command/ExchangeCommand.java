package com.github.petterj345.grandexchange.command;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.ExchangeMenu;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.Listing;
import com.github.petterj345.grandexchange.storage.MarketStats;
import com.github.petterj345.grandexchange.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ExchangeCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("browse", "sell", "mine", "help");

    private final Grandexchange plugin;

    public ExchangeCommand(Grandexchange plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use the Grand Exchange.");
            return true;
        }
        if (args.length == 0
                || args[0].equalsIgnoreCase("browse")
                || args[0].equalsIgnoreCase("open")) {
            openBrowse(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell" -> openSell(player);
            case "mine" -> openMine(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void openBrowse(Player player) {
        try {
            List<Listing> listings = plugin.database().all();
            if (listings.isEmpty()) {
                player.sendMessage(msg("The Grand Exchange is empty. Use /ge sell to list something!",
                        NamedTextColor.GRAY));
                return;
            }
            new ExchangeMenu(plugin, ExchangeMenu.Mode.BROWSE, listings, 0).open(player);
        } catch (Exception e) {
            player.sendMessage(msg("Failed to open the exchange: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void openMine(Player player) {
        try {
            List<Listing> listings = plugin.database().bySeller(player.getUniqueId());
            if (listings.isEmpty()) {
                player.sendMessage(msg("You have no active listings.", NamedTextColor.GRAY));
                return;
            }
            new ExchangeMenu(plugin, ExchangeMenu.Mode.MINE, listings, 0).open(player);
        } catch (Exception e) {
            player.sendMessage(msg("Failed to open your listings: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void openSell(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType().isAir()) {
            player.sendMessage(msg("Hold the item you want to sell, then run /ge sell.", NamedTextColor.RED));
            return;
        }
        ItemStack template = inHand.clone();
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
        SellSession session = new SellSession(template, Math.max(1, Math.min(inHand.getAmount(), available)),
                defaultPrice);
        plugin.input().setSell(player.getUniqueId(), session);
        new SellMenu(plugin, session).open(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(msg("Grand Exchange:", NamedTextColor.GOLD));
        player.sendMessage(msg("/ge - browse listings and buy", NamedTextColor.YELLOW));
        player.sendMessage(msg("/ge sell - sell the item in your hand", NamedTextColor.YELLOW));
        player.sendMessage(msg("/ge mine - view & cancel your listings", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) {
                    out.add(sub);
                }
            }
        }
        return out;
    }

    private static Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
