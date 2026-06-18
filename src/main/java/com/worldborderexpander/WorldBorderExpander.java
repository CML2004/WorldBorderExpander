package com.worldborderexpander;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class WorldBorderExpander extends JavaPlugin {

    private static WorldBorderExpander instance;
    private BorderConfig borderConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        borderConfig = new BorderConfig(this);

        getCommand("bordershop").setExecutor(new BorderShopCommand(this));

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("WorldBorderExpander enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("WorldBorderExpander disabled.");
    }

    public static WorldBorderExpander getInstance() {
        return instance;
    }

    public BorderConfig getBorderConfig() {
        return borderConfig;
    }
}
