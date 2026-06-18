package com.worldborderexpander;

import org.bukkit.plugin.java.JavaPlugin;

public class WorldBorderExpander extends JavaPlugin {

    private static WorldBorderExpander instance;
    private BorderConfig borderConfig;
    private AfkManager afkManager;
    private BorderGrowthTask growthTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        borderConfig = new BorderConfig(this);
        afkManager = new AfkManager(this);

        // Initialize border to starting size if it's at default (60M)
        initBorder();

        // Register events
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PenaltyListener(this), this);
        getServer().getPluginManager().registerEvents(new AfkListener(this), this);

        // Register commands
        BorderCommand cmd = new BorderCommand(this);
        getCommand("bordershop").setExecutor(cmd);
        getCommand("border").setExecutor(cmd);

        // Start growth task
        growthTask = new BorderGrowthTask(this);
        growthTask.start();

        getLogger().info("WorldBorderExpander enabled!");
    }

    @Override
    public void onDisable() {
        if (growthTask != null) growthTask.stop();
        getLogger().info("WorldBorderExpander disabled.");
    }

    private void initBorder() {
        var world = getServer().getWorld(borderConfig.getWorldName());
        if (world == null) return;
        var border = world.getWorldBorder();
        // Only set if it looks like it hasn't been configured (default is 59,999,968)
        if (border.getSize() > 1_000_000) {
            double startSize = borderConfig.getStartingSize();
            border.setSize(startSize);
            getLogger().info("World border initialized to " + startSize + " blocks.");
        }
    }

    public static WorldBorderExpander getInstance() { return instance; }
    public BorderConfig getBorderConfig() { return borderConfig; }
    public AfkManager getAfkManager() { return afkManager; }
}
