package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.input.BuySession;
import com.github.petterj345.grandexchange.storage.MarketSummary;
import com.github.petterj345.grandexchange.util.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Compose a buy offer: pick quantity (buttons or chat) and your max price (chat),
 * with the current market prices shown for reference.
 */
public final class BuyMenu implements InventoryHolder {

    public static final int SLOT_ITEM = 13;
    public static final int SLOT_DEC_10 = 10;
    public static final int SLOT_DEC_1 = 11;
    public static final int SLOT_INC_1 = 15;
    public static final int SLOT_INC_10 = 16;
    public static final int SLOT_INC_64 = 9;
    public static final int SLOT_TYPE_AMOUNT = 17;
    public static final int SLOT_SET_PRICE = 4;
    public static final int SLOT_USE_MARKET = 5;
    public static final int SLOT_CONFIRM = 22;
    public static final int SLOT_CANCEL = 26;

    private final Grandexchange plugin;
    private final BuySession session;
    private Inventory inventory;

    public BuyMenu(Grandexchange plugin, BuySession session) {
        this.plugin = plugin;
        this.session = session;
    }

    public BuySession session() {
        return session;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        build(player);
        player.openInventory(inventory);
    }

    private void build(Player player) {
        inventory = Bukkit.createInventory(this, 27, Component.text("Buy offer"));
        MarketSummary summary = summary();

        inventory.setItem(SLOT_ITEM, itemIcon(player, summary));
        inventory.setItem(SLOT_DEC_10, Gui.button(Material.RED_STAINED_GLASS_PANE, "-10", null));
        inventory.setItem(SLOT_DEC_1, Gui.button(Material.PINK_STAINED_GLASS_PANE, "-1", null));
        inventory.setItem(SLOT_INC_1, Gui.button(Material.LIME_STAINED_GLASS_PANE, "+1", null));
        inventory.setItem(SLOT_INC_10, Gui.button(Material.GREEN_STAINED_GLASS_PANE, "+10", null));
        inventory.setItem(SLOT_INC_64, Gui.button(Material.HOPPER, "+64", null));
        inventory.setItem(SLOT_TYPE_AMOUNT, Gui.button(Material.OAK_SIGN, "Type amount in chat", null));
        inventory.setItem(SLOT_SET_PRICE, priceButton());
        inventory.setItem(SLOT_USE_MARKET, marketButton(summary));
        inventory.setItem(SLOT_CONFIRM, Gui.button(Material.EMERALD_BLOCK, "Place buy offer", null));
        inventory.setItem(SLOT_CANCEL, Gui.button(Material.BARRIER, "Cancel", null));
    }

    private MarketSummary summary() {
        try {
            return plugin.database().marketSummary(session.label());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack itemIcon(Player player, MarketSummary summary) {
        ItemStack icon = session.template().clone();
        icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), session.amount())));

        List<Component> lore = new ArrayList<>();
        lore.add(Gui.line("Quantity: " + session.amount(), NamedTextColor.WHITE));
        if (session.maxPricePerItem() > 0) {
            lore.add(Gui.line("Max price each: " + plugin.economy().format(session.maxPricePerItem()),
                    NamedTextColor.GOLD));
            lore.add(Gui.line("Reserved total: "
                    + plugin.economy().format(session.maxPricePerItem() * session.amount()), NamedTextColor.GOLD));
        } else {
            lore.add(Gui.line("Max price each: not set", NamedTextColor.RED));
        }
        lore.add(Component.empty());
        if (summary != null && summary.hasAsk()) {
            lore.add(Gui.line("Lowest sell price: " + plugin.economy().format(summary.lowestAsk()),
                    NamedTextColor.AQUA));
            lore.add(Gui.line(summary.askQuantity() + " available now", NamedTextColor.DARK_AQUA));
        } else {
            lore.add(Gui.line("No sellers yet — your offer will rest", NamedTextColor.DARK_AQUA));
        }
        lore.add(Component.empty());
        lore.add(Gui.line("Your balance: " + plugin.economy().format(plugin.economy().balanceOf(player)),
                NamedTextColor.GRAY));
        return Gui.decorate(icon, lore);
    }

    private ItemStack priceButton() {
        List<Component> lore = new ArrayList<>();
        lore.add(session.maxPricePerItem() > 0
                ? Gui.line("Current: " + plugin.economy().format(session.maxPricePerItem()) + " each",
                NamedTextColor.GRAY)
                : Gui.line("Current: not set", NamedTextColor.GRAY));
        return Gui.button(Material.GOLD_INGOT, "Set max price (type in chat)", lore);
    }

    private ItemStack marketButton(MarketSummary summary) {
        List<Component> lore = new ArrayList<>();
        lore.add(summary != null && summary.hasAsk()
                ? Gui.line("Set to lowest ask: " + plugin.economy().format(summary.lowestAsk()), NamedTextColor.GRAY)
                : Gui.line("No market data yet", NamedTextColor.GRAY));
        return Gui.button(Material.GOLD_NUGGET, "Match market price", lore);
    }
}
