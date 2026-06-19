package com.worldborderexpander;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldBorderExpander extends JavaPlugin {

    private static WorldBorderExpander instance;
    private BorderConfig borderConfig;
    private AfkManager afkManager;
    private BorderGrowthTask growthTask;
    private ChallengeManager challengeManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        borderConfig = new BorderConfig(this);
        afkManager = new AfkManager(this);
        challengeManager = new ChallengeManager(this);

        // Initialize border to starting size if it's at default (60M)
        initBorder();

        // Register events
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PenaltyListener(this), this);
        getServer().getPluginManager().registerEvents(new AfkListener(this), this);
        getServer().getPluginManager().registerEvents(challengeManager, this);

        // Register commands
        BorderCommand cmd = new BorderCommand(this);
        getCommand("bordershop").setExecutor(cmd);
        getCommand("border").setExecutor(cmd);
        getCommand("borderchallenge").setExecutor(cmd);

        // Start growth and event tasks
        growthTask = new BorderGrowthTask(this);
        growthTask.start();
        challengeManager.start();

        getLogger().info("WorldBorderExpander enabled!");
    }

    @Override
    public void onDisable() {
        if (growthTask != null) growthTask.stop();
        if (challengeManager != null) challengeManager.stop();
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
    public ChallengeManager getChallengeManager() { return challengeManager; }

    public double shrinkBorderSafely(World world, double shrinkAmount) {
        BorderConfig cfg = getBorderConfig();
        WorldBorder border = world.getWorldBorder();
        double currentSize = border.getSize();
        double targetSize = Math.max(cfg.getStartingSize(), currentSize - shrinkAmount);
        if (targetSize >= currentSize) return currentSize;

        boolean safeShrink = getConfig().getBoolean("safe-shrink.enabled", true);
        int buffer = getConfig().getInt("safe-shrink.edge-buffer-blocks", 3);
        if (safeShrink) {
            movePlayersInsideBorder(world, targetSize, buffer);
        }

        border.setSize(targetSize);
        return targetSize;
    }

    private void movePlayersInsideBorder(World world, double newSize, int edgeBuffer) {
        double radius = Math.max(1.0, newSize / 2.0 - edgeBuffer);
        var center = world.getWorldBorder().getCenter();
        boolean moved = false;

        for (var player : world.getPlayers()) {
            var location = player.getLocation();
            double dx = location.getX() - center.getX();
            double dz = location.getZ() - center.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > radius) {
                double angle = Math.atan2(dz, dx);
                double targetX = center.getX() + Math.cos(angle) * radius;
                double targetZ = center.getZ() + Math.sin(angle) * radius;
                int targetY = world.getHighestBlockYAt((int) Math.floor(targetX), (int) Math.floor(targetZ)) + 1;
                Location target = new Location(world, targetX, targetY, targetZ, location.getYaw(), location.getPitch());
                player.teleport(target);
                moved = true;
            }
        }

        if (moved) {
            getServer().broadcastMessage("§eThe border contracted, so you were moved safely inside.");
        }
    }
}
