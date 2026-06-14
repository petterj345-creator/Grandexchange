package com.github.petterj345.grandexchange.command;

import com.github.petterj345.grandexchange.Grandexchange;
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

/**
 * Thin command layer over {@link com.github.petterj345.grandexchange.service.ExchangeService}.
 * Everything is also reachable from the GUI tabs, so the command is just a convenience.
 */
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
            plugin.exchange().openBrowse(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell" -> openSell(player);
            case "mine" -> plugin.exchange().openMine(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void openSell(Player player) {
        // If they're holding something, sell that directly; otherwise let them pick in the GUI.
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType().isAir()) {
            plugin.exchange().openSellSelect(player);
        } else {
            plugin.exchange().openSell(player, inHand);
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(msg("Grand Exchange:", NamedTextColor.GOLD));
        player.sendMessage(msg("/ge - open the exchange (browse, sell and manage from the menu)",
                NamedTextColor.YELLOW));
        player.sendMessage(msg("/ge sell - start selling the item in your hand", NamedTextColor.YELLOW));
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
