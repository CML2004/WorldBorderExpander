package com.worldborderexpander;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkManager {

    private final WorldBorderExpander plugin;

    // Last recorded position and the time it was last changed
    private final Map<UUID, Location> lastPositions = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    public AfkManager(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    /** Called every few seconds by AfkListener to update positions. */
    public void updatePosition(Player player) {
        UUID id = player.getUniqueId();
        Location current = player.getLocation().clone();
        Location last = lastPositions.get(id);

        boolean moved = last == null
            || Math.abs(current.getX() - last.getX()) > 0.1
            || Math.abs(current.getZ() - last.getZ()) > 0.1;

        if (moved) {
            lastPositions.put(id, current);
            lastMoveTime.put(id, System.currentTimeMillis());
        }
    }

    public boolean isAfk(Player player) {
        Long lastMove = lastMoveTime.get(player.getUniqueId());
        if (lastMove == null) return false;
        long afkMillis = plugin.getBorderConfig().getAfkMinutes() * 60_000L;
        return System.currentTimeMillis() - lastMove > afkMillis;
    }

    public void remove(Player player) {
        lastPositions.remove(player.getUniqueId());
        lastMoveTime.remove(player.getUniqueId());
    }

    public boolean areAllAfk() {
        var online = plugin.getServer().getOnlinePlayers();
        if (online.isEmpty()) return false;
        for (Player p : online) {
            if (!isAfk(p)) return false;
        }
        return true;
    }
}
