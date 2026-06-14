package com.github.petterj345.grandexchange.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Inventory helpers for counting, removing and granting items while respecting stack sizes.
 */
public final class Items {

    private Items() {
    }

    /** Counts how many items matching {@code match} (ignoring amount) the player holds. */
    public static int count(Player player, ItemStack match) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.isSimilar(match)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Removes exactly {@code amount} items matching {@code match} from the player's inventory.
     * Returns false (removing nothing) if the player does not have that many.
     */
    public static boolean removeMatching(Player player, ItemStack match, int amount) {
        if (count(player, match) < amount) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !stack.isSimilar(match)) {
                continue;
            }
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    /**
     * Gives the player {@code amount} copies of {@code single}, splitting into stacks.
     * Anything that does not fit is dropped at the player's feet.
     */
    public static void give(Player player, ItemStack single, int amount) {
        int max = Math.max(1, single.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int give = Math.min(max, remaining);
            ItemStack stack = single.clone();
            stack.setAmount(give);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                Location loc = player.getLocation();
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(loc, drop);
                }
            }
            remaining -= give;
        }
    }
}
