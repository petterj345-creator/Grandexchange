package com.github.petterj345.grandexchange;

import com.github.petterj345.grandexchange.command.ExchangeCommand;
import com.github.petterj345.grandexchange.economy.EconomyHook;
import com.github.petterj345.grandexchange.engine.MatchingEngine;
import com.github.petterj345.grandexchange.input.InputManager;
import com.github.petterj345.grandexchange.listener.ChatListener;
import com.github.petterj345.grandexchange.listener.MenuListener;
import com.github.petterj345.grandexchange.service.ExchangeService;
import com.github.petterj345.grandexchange.storage.Database;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the Grandexchange plugin.
 */
public final class Grandexchange extends JavaPlugin {

    private Database database;
    private EconomyHook economy;
    private InputManager input;
    private MatchingEngine engine;
    private ExchangeService exchange;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economy = new EconomyHook(this);
        if (!economy.setup()) {
            getLogger().severe("Vault economy provider not found. Install Vault + an economy plugin. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            database = new Database(this);
            database.initialize();
        } catch (Exception e) {
            getLogger().severe("Failed to initialise the database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        input = new InputManager();
        engine = new MatchingEngine(this);
        exchange = new ExchangeService(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            try {
                com.github.petterj345.grandexchange.citizens.CitizensHook.enable(this);
                getLogger().info("Citizens integration enabled. Select an NPC and run "
                        + "/trait grandexchange to make it a Grand Exchange clerk.");
            } catch (Throwable t) {
                getLogger().warning("Citizens was found but integration could not be enabled: "
                        + t.getMessage());
            }
        }

        ExchangeCommand command = new ExchangeCommand(this);
        PluginCommand pluginCommand = getCommand("grandexchange");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("Grandexchange enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("Grandexchange disabled.");
    }

    public Database database() {
        return database;
    }

    public EconomyHook economy() {
        return economy;
    }

    public InputManager input() {
        return input;
    }

    public MatchingEngine engine() {
        return engine;
    }

    public ExchangeService exchange() {
        return exchange;
    }

    public double taxPercent() {
        return getConfig().getDouble("tax-percent", 0.0);
    }

    public int maxListingsPerPlayer() {
        return getConfig().getInt("max-listings-per-player", 20);
    }
}
