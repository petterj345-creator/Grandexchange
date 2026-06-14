package com.github.petterj345.grandexchange.command;

import com.github.petterj345.grandexchange.Grandexchange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin command layer over {@link com.github.petterj345.grandexchange.service.ExchangeService}.
 * Everything is also reachable from the GUI tabs, so the command is just a convenience.
 */
public final class ExchangeCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("buy", "sell", "offers", "collect", "help");

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
                || args[0].equalsIgnoreCase("buy")
                || args[0].equalsIgnoreCase("market")
                || args[0].equalsIgnoreCase("browse")
                || args[0].equalsIgnoreCase("open")) {
            plugin.exchange().openBuyBrowse(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell" -> plugin.exchange().openSellBrowse(player);
            case "offers", "mine" -> plugin.exchange().openMyOffers(player);
            case "collect", "collection" -> plugin.exchange().openCollection(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(msg("Grand Exchange:", NamedTextColor.GOLD));
        player.sendMessage(msg("/ge buy - browse items for sale (sell orders)", NamedTextColor.YELLOW));
        player.sendMessage(msg("/ge sell - browse buy orders to sell into", NamedTextColor.YELLOW));
        player.sendMessage(msg("/ge offers - view & cancel your buy/sell offers", NamedTextColor.YELLOW));
        player.sendMessage(msg("/ge collect - open your collection box", NamedTextColor.YELLOW));
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
