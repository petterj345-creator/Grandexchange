package com.github.petterj345.grandexchange.gui;

import com.github.petterj345.grandexchange.Grandexchange;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Prompts the player to click an item in their own inventory to start selling it.
 * The actual selection is handled by the click listener (a click in the bottom inventory).
 */
public final class SellSelectMenu implements InventoryHolder {

    public static final int SLOT_BACK = 0;
    public static final int SLOT_INFO = 4;

    private final Grandexchange plugin;
    private Inventory inventory;

    public SellSelectMenu(Grandexchange plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 9, Component.text("Resources — sell order"));
        inventory.setItem(SLOT_BACK, button(Material.ARROW, "Back to browse", null));
        inventory.setItem(SLOT_INFO, button(Material.PAPER, "Click a resource to sell",
                "Click any resource in your inventory below to list it for sale. Only resources can be traded."));
        player.openInventory(inventory);
    }

    private ItemStack button(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line(name, NamedTextColor.YELLOW));
            if (loreLine != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(line(loreLine, NamedTextColor.GRAY));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
