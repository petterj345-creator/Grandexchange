package com.github.petterj345.grandexchange.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Inventory helpers for counting, removing and granting items while respecting stack sizes.
 */
public final class Items {

    /**
     * Default tradeable resources, used when the config has no {@code resources} key at all
     * (e.g. an upgraded server whose config predates the setting). Kept in sync with the
     * {@code resources:} block shipped in config.yml.
     */
    public static final List<String> DEFAULT_RESOURCE_NAMES = List.of(
            // ores, ingots, gems and smelting products
            "COAL", "CHARCOAL", "RAW_IRON", "RAW_GOLD", "RAW_COPPER", "IRON_INGOT", "GOLD_INGOT",
            "COPPER_INGOT", "NETHERITE_INGOT", "NETHERITE_SCRAP", "IRON_NUGGET", "GOLD_NUGGET",
            "DIAMOND", "EMERALD", "LAPIS_LAZULI", "REDSTONE", "QUARTZ", "AMETHYST_SHARD",
            "GLOWSTONE_DUST", "FLINT", "CLAY_BALL", "BRICK", "NETHER_BRICK",
            // ores
            "COAL_ORE", "DEEPSLATE_COAL_ORE", "IRON_ORE", "DEEPSLATE_IRON_ORE", "COPPER_ORE",
            "DEEPSLATE_COPPER_ORE", "GOLD_ORE", "DEEPSLATE_GOLD_ORE", "NETHER_GOLD_ORE", "REDSTONE_ORE",
            "DEEPSLATE_REDSTONE_ORE", "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE", "DIAMOND_ORE",
            "DEEPSLATE_DIAMOND_ORE", "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE", "NETHER_QUARTZ_ORE",
            "ANCIENT_DEBRIS",
            // logs and wood
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG",
            "MANGROVE_LOG", "CHERRY_LOG", "PALE_OAK_LOG", "CRIMSON_STEM", "WARPED_STEM", "BAMBOO",
            "STICK",
            // planks
            "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS", "ACACIA_PLANKS",
            "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS", "PALE_OAK_PLANKS", "CRIMSON_PLANKS",
            "WARPED_PLANKS", "BAMBOO_PLANKS",
            // crops, farming and food
            "WHEAT", "WHEAT_SEEDS", "CARROT", "POTATO", "BEETROOT", "BEETROOT_SEEDS", "MELON_SLICE",
            "MELON_SEEDS", "PUMPKIN_SEEDS", "SUGAR_CANE", "SUGAR", "APPLE", "SWEET_BERRIES",
            "GLOW_BERRIES", "COCOA_BEANS", "NETHER_WART", "KELP", "DRIED_KELP", "HONEYCOMB", "EGG",
            "BREAD",
            // meat and fish
            "BEEF", "PORKCHOP", "CHICKEN", "MUTTON", "RABBIT", "COD", "SALMON", "TROPICAL_FISH",
            "PUFFERFISH",
            // mob and gathering drops
            "LEATHER", "FEATHER", "STRING", "BONE", "BONE_MEAL", "GUNPOWDER", "SLIME_BALL",
            "ENDER_PEARL", "BLAZE_ROD", "BLAZE_POWDER", "GHAST_TEAR", "SPIDER_EYE", "ROTTEN_FLESH",
            "MAGMA_CREAM", "PHANTOM_MEMBRANE", "RABBIT_HIDE", "RABBIT_FOOT", "INK_SAC", "GLOW_INK_SAC",
            "NAUTILUS_SHELL", "PRISMARINE_SHARD", "PRISMARINE_CRYSTALS", "SHULKER_SHELL", "OBSIDIAN");

    private Items() {
    }

    /**
     * Resolves a list of material names (from config) into a set of {@link Material}s. Names are
     * matched case-insensitively via {@link Material#matchMaterial(String)}; any that don't
     * resolve are logged and skipped so one typo doesn't break the whole list.
     */
    public static Set<Material> parseResources(List<String> names, Logger logger) {
        Set<Material> resources = EnumSet.noneOf(Material.class);
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Material material = Material.matchMaterial(name.trim());
            if (material == null) {
                logger.warning("Unknown resource material in config: '" + name + "' — skipping.");
            } else {
                resources.add(material);
            }
        }
        return resources;
    }

    /** Whether an item's type is in the allowed resource set. */
    public static boolean isResource(ItemStack stack, Set<Material> allowed) {
        return stack != null && allowed.contains(stack.getType());
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
