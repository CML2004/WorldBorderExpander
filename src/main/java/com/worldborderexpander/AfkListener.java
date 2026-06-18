package com.worldborderexpander;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AfkListener implements Listener {

    private final WorldBorderExpander plugin;

    public AfkListener(WorldBorderExpander plugin) {
        this.plugin = plugin;
        startPositionChecker();
    }

    private void startPositionChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    plugin.getAfkManager().updatePosition(p);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // check every 2 seconds
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getAfkManager().updatePosition(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getAfkManager().remove(event.getPlayer());
    }
}
