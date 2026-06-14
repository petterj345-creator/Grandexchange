package com.github.petterj345.grandexchange;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main entry point for the Grandexchange plugin.
 */
public final class Grandexchange extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Grandexchange enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Grandexchange disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("grandexchange")) {
            sender.sendMessage("Grandexchange v" + getPluginMeta().getVersion() + " is running.");
            return true;
        }
        return false;
    }
}
