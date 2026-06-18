package com.worldborderexpander;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BorderGrowthTask {

    private final WorldBorderExpander plugin;
    private BukkitTask task;

    // One Minecraft day = 24000 ticks = 20 minutes real time
    private static final long MC_DAY_TICKS = 24000L;

    public BorderGrowthTask(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Check every tick whether a day has rolled over
        task = new BukkitRunnable() {
            private long lastDayCount = -1;

            @Override
            public void run() {
                BorderConfig cfg = plugin.getBorderConfig();
                if (!cfg.isAutoGrowthEnabled()) return;

                World world = plugin.getServer().getWorld(cfg.getWorldName());
                if (world == null) return;

                long dayCount = world.getFullTime() / MC_DAY_TICKS;

                if (lastDayCount == -1) {
                    lastDayCount = dayCount;
                    return;
                }

                if (dayCount <= lastDayCount) return;
                lastDayCount = dayCount;

                // A new day has started — check if we should grow
                boolean empty = world.getPlayers().isEmpty();
                boolean allAfk = plugin.getAfkManager().areAllAfk();

                if (cfg.isPauseWhenEmpty() && empty) {
                    plugin.getLogger().info("[AutoGrowth] Skipped — no players online.");
                    return;
                }
                if (cfg.isPauseWhenAllAfk() && allAfk) {
                    plugin.getLogger().info("[AutoGrowth] Skipped — all players are AFK.");
                    return;
                }

                double current = world.getWorldBorder().getSize();
                double growth = cfg.getGrowthAmount();
                double newSize = Math.min(current + growth, cfg.getMaxBorderSize());

                if (newSize <= current) return;

                world.getWorldBorder().setSize(newSize);
                plugin.getLogger().info("[AutoGrowth] Border grew by " + growth + " to " + newSize);

                String msg = cfg.getString("auto-growth.announce-message",
                    "§6[Border] §7A new day dawns! The border grew by §b+" + growth + " §7blocks to §f" + String.format("%.0f", newSize) + "§7.");
                if (msg != null && !msg.isEmpty()) {
                    plugin.getServer().broadcastMessage(
                        msg.replace("{growth}", String.format("%.0f", growth))
                           .replace("{size}", String.format("%.0f", newSize))
                    );
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // check every second (20 ticks)
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
