package com.github.petterj345.grandexchange.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Small helpers for building GUI items without italic formatting noise. */
public final class Gui {

    private Gui() {
    }

    /** A non-italic colored text line, for names and lore. */
    public static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** A button item with a yellow name and optional lore lines. */
    public static ItemStack button(Material material, String name, List<Component> lore) {
        return named(material, name, NamedTextColor.YELLOW, lore);
    }

    public static ItemStack named(Material material, String name, NamedTextColor nameColor, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line(name, nameColor));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Applies a name + lore to a copy of an existing item (e.g. a listing icon). */
    public static ItemStack decorate(ItemStack base, List<Component> lore) {
        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (lore != null) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
