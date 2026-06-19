package com.worldborderexpander;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        plugin.getAfkManager().updatePosition(event.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        plugin.getAfkManager().updatePosition(event.getPlayer());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        plugin.getAfkManager().updatePosition(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        plugin.getAfkManager().updatePosition(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            plugin.getAfkManager().updatePosition(player);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getAfkManager().updatePosition(event.getPlayer()));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        plugin.getAfkManager().updatePosition(event.getPlayer());
    }
}
