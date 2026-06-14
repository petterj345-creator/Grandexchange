package com.github.petterj345.grandexchange.listener;

import com.github.petterj345.grandexchange.Grandexchange;
import com.github.petterj345.grandexchange.gui.BuyMenu;
import com.github.petterj345.grandexchange.gui.CollectionMenu;
import com.github.petterj345.grandexchange.gui.ItemDetailMenu;
import com.github.petterj345.grandexchange.gui.MarketMenu;
import com.github.petterj345.grandexchange.gui.MyOffersMenu;
import com.github.petterj345.grandexchange.gui.Nav;
import com.github.petterj345.grandexchange.gui.SellMenu;
import com.github.petterj345.grandexchange.gui.SellSelectMenu;
import com.github.petterj345.grandexchange.input.BuySession;
import com.github.petterj345.grandexchange.input.Prompt;
import com.github.petterj345.grandexchange.input.PromptType;
import com.github.petterj345.grandexchange.input.SellSession;
import com.github.petterj345.grandexchange.storage.MarketSummary;
import com.github.petterj345.grandexchange.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Routes clicks in every exchange menu. All menus are read-only, so clicks are cancelled. */
public final class MenuListener implements Listener {

    private final Grandexchange plugin;

    public MenuListener(Grandexchange plugin) {
        this.plugin = plugin;
    }

    private static boolean ours(InventoryHolder holder) {
        return holder instanceof MarketMenu || holder instanceof ItemDetailMenu
                || holder instanceof BuyMenu || holder instanceof SellMenu
                || holder instanceof SellSelectMenu || holder instanceof MyOffersMenu
                || holder instanceof CollectionMenu;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (ours(event.getView().getTopInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!ours(holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // The sell-picker reacts to clicks in the player's own (bottom) inventory.
        if (holder instanceof SellSelectMenu) {
            handleSelectClick(player, event, top);
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }
        int slot = event.getSlot();
        if (holder instanceof MarketMenu menu) {
            handleMarketClick(player, menu, slot);
        } else if (holder instanceof MyOffersMenu menu) {
            handleMyOffersClick(player, menu, slot);
        } else if (holder instanceof CollectionMenu menu) {
            handleCollectionClick(player, menu, slot);
        } else if (holder instanceof ItemDetailMenu menu) {
            handleDetailClick(player, menu, slot);
        } else if (holder instanceof BuyMenu menu) {
            handleBuyClick(player, menu, slot);
        } else if (holder instanceof SellMenu menu) {
            handleSellClick(player, menu, slot);
        }
    }

    // ---------------------------------------------------------------- market

    private void handleMarketClick(Player player, MarketMenu menu, int slot) {
        if (slot == Nav.PREV) {
            menu.reopen(player, menu.page() - 1);
            return;
        }
        if (slot == Nav.NEXT) {
            menu.reopen(player, menu.page() + 1);
            return;
        }
        if (Nav.isTab(slot)) {
            handleTab(player, slot);
            return;
        }
        MarketSummary summary = menu.summaryAt(slot);
        if (summary != null) {
            plugin.exchange().openItemDetail(player, summary.item());
        }
    }

    private void handleMyOffersClick(Player player, MyOffersMenu menu, int slot) {
        if (slot == Nav.PREV) {
            menu.reopen(player, menu.page() - 1);
            return;
        }
        if (slot == Nav.NEXT) {
            menu.reopen(player, menu.page() + 1);
            return;
        }
        if (Nav.isTab(slot)) {
            handleTab(player, slot);
            return;
        }
        Long offerId = menu.offerAt(slot);
        if (offerId != null) {
            plugin.exchange().cancelOffer(player, offerId);
        }
    }

    private void handleCollectionClick(Player player, CollectionMenu menu, int slot) {
        if (slot == CollectionMenu.SLOT_COLLECT_ALL) {
            plugin.exchange().collectAll(player);
            return;
        }
        if (slot == Nav.PREV) {
            menu.reopen(player, menu.page() - 1);
            return;
        }
        if (slot == Nav.NEXT) {
            menu.reopen(player, menu.page() + 1);
            return;
        }
        if (Nav.isTab(slot)) {
            handleTab(player, slot);
            return;
        }
        Long entryId = menu.entryAt(slot);
        if (entryId != null) {
            plugin.exchange().collectEntry(player, entryId);
        }
    }

    private void handleTab(Player player, int slot) {
        switch (slot) {
            case Nav.MARKET -> plugin.exchange().openMarket(player);
            case Nav.SELL -> plugin.exchange().openSellSelect(player);
            case Nav.MY_OFFERS -> plugin.exchange().openMyOffers(player);
            case Nav.COLLECTION -> plugin.exchange().openCollection(player);
            default -> {
                // not a tab
            }
        }
    }

    // ------------------------------------------------------------ item detail

    private void handleDetailClick(Player player, ItemDetailMenu menu, int slot) {
        switch (slot) {
            case ItemDetailMenu.SLOT_BUY -> plugin.exchange().openBuy(player, menu.template());
            case ItemDetailMenu.SLOT_SELL -> plugin.exchange().openSell(player, menu.template());
            case ItemDetailMenu.SLOT_BACK -> plugin.exchange().openMarket(player);
            default -> {
                // decorative
            }
        }
    }

    // -------------------------------------------------------------- sell pick

    private void handleSelectClick(Player player, InventoryClickEvent event, Inventory top) {
        if (event.getClickedInventory() == top) {
            if (event.getSlot() == SellSelectMenu.SLOT_BACK) {
                plugin.exchange().openMarket(player);
            }
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        plugin.exchange().openSell(player, clicked);
    }

    // -------------------------------------------------------------------- buy

    private void handleBuyClick(Player player, BuyMenu menu, int slot) {
        BuySession session = menu.session();
        switch (slot) {
            case BuyMenu.SLOT_DEC_10 -> {
                session.amount(Math.max(1, session.amount() - 10));
                menu.open(player);
            }
            case BuyMenu.SLOT_DEC_1 -> {
                session.amount(Math.max(1, session.amount() - 1));
                menu.open(player);
            }
            case BuyMenu.SLOT_INC_1 -> {
                session.amount(session.amount() + 1);
                menu.open(player);
            }
            case BuyMenu.SLOT_INC_10 -> {
                session.amount(session.amount() + 10);
                menu.open(player);
            }
            case BuyMenu.SLOT_INC_64 -> {
                session.amount(session.amount() + 64);
                menu.open(player);
            }
            case BuyMenu.SLOT_TYPE_AMOUNT -> prompt(player, PromptType.BUY_QUANTITY,
                    "Type how many you want to buy (or 'cancel').");
            case BuyMenu.SLOT_SET_PRICE -> prompt(player, PromptType.BUY_PRICE,
                    "Type your max price per item (or 'cancel').");
            case BuyMenu.SLOT_USE_MARKET -> {
                MarketSummary summary = summary(session.label());
                if (summary != null && summary.hasAsk()) {
                    session.maxPricePerItem(summary.lowestAsk());
                } else {
                    player.sendMessage(msg("No sellers yet — set a price manually.", NamedTextColor.GRAY));
                }
                menu.open(player);
            }
            case BuyMenu.SLOT_CONFIRM -> plugin.exchange().confirmBuy(player, session);
            case BuyMenu.SLOT_CANCEL -> {
                plugin.input().clearBuy(player.getUniqueId());
                plugin.exchange().openMarket(player);
            }
            default -> {
                // decorative
            }
        }
    }

    // ------------------------------------------------------------------- sell

    private void handleSellClick(Player player, SellMenu menu, int slot) {
        SellSession session = menu.session();
        switch (slot) {
            case SellMenu.SLOT_DEC_10 -> {
                session.amount(Math.max(1, session.amount() - 10));
                menu.open(player);
            }
            case SellMenu.SLOT_DEC_1 -> {
                session.amount(Math.max(1, session.amount() - 1));
                menu.open(player);
            }
            case SellMenu.SLOT_INC_1 -> {
                session.amount(session.amount() + 1);
                menu.open(player);
            }
            case SellMenu.SLOT_INC_10 -> {
                session.amount(session.amount() + 10);
                menu.open(player);
            }
            case SellMenu.SLOT_ALL -> {
                session.amount(Math.max(1, Items.count(player, session.template())));
                menu.open(player);
            }
            case SellMenu.SLOT_TYPE_AMOUNT -> prompt(player, PromptType.SELL_QUANTITY,
                    "Type how many you want to sell (or 'cancel').");
            case SellMenu.SLOT_SET_PRICE -> prompt(player, PromptType.SELL_PRICE,
                    "Type your price per item (or 'cancel').");
            case SellMenu.SLOT_USE_MARKET -> {
                MarketSummary summary = summary(session.label());
                if (summary != null && summary.hasBid()) {
                    session.pricePerItem(summary.highestBid());
                } else if (summary != null && summary.hasAsk()) {
                    session.pricePerItem(summary.lowestAsk());
                } else {
                    player.sendMessage(msg("No market data yet — set a price manually.", NamedTextColor.GRAY));
                }
                menu.open(player);
            }
            case SellMenu.SLOT_CONFIRM -> plugin.exchange().confirmSell(player, session);
            case SellMenu.SLOT_CANCEL -> {
                plugin.input().clearSell(player.getUniqueId());
                plugin.exchange().openMarket(player);
            }
            default -> {
                // decorative
            }
        }
    }

    private void prompt(Player player, PromptType type, String message) {
        plugin.input().setPrompt(player.getUniqueId(), Prompt.of(type));
        player.closeInventory();
        player.sendMessage(msg(message, NamedTextColor.YELLOW));
    }

    private MarketSummary summary(String label) {
        try {
            return plugin.database().marketSummary(label);
        } catch (Exception e) {
            return null;
        }
    }

    private static Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
