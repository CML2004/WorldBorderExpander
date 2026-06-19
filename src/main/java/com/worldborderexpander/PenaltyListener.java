package com.worldborderexpander;

import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PenaltyListener implements Listener {

    private final WorldBorderExpander plugin;

    public PenaltyListener(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    // ── Death Penalties ────────────────────────────────────────────────

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        var cause = player.getLastDamageCause();
        if (cause == null) return;

        BorderConfig cfg = plugin.getBorderConfig();

        // Map damage cause to penalty key
        String penaltyKey = switch (cause.getCause()) {
            case FALL -> "death.fall";
            case DROWNING -> "death.drowning";
            case LAVA -> "death.lava";
            case FIRE, FIRE_TICK -> "death.fire";
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE -> player.getKiller() != null ? "death.pvp" : "death.mob";
            case VOID -> "death.void";
            case MAGIC -> "death.magic";
            case WITHER -> "death.wither";
            case LIGHTNING -> "death.lightning";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "death.explosion";
            case STARVATION -> "death.starvation";
            default -> "death.other";
        };

        applyPenalty(penaltyKey, player.getName());
    }

    // ── World Change Penalty ───────────────────────────────────────────

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World newWorld = player.getWorld();

        // Penalize leaving the overworld
        if (newWorld.getEnvironment() != World.Environment.NORMAL) {
            applyPenalty("leave-overworld", player.getName());
        }
    }

    // ── Shared penalty logic ───────────────────────────────────────────

    private void applyPenalty(String key, String playerName) {
        BorderConfig cfg = plugin.getBorderConfig();
        if (!cfg.isPenaltyEnabled(key)) return;

        World world = plugin.getServer().getWorld(cfg.getWorldName());
        if (world == null) return;

        WorldBorder border = world.getWorldBorder();
        double current = border.getSize();
        double amount = cfg.getPenaltyAmount(key);
        double newSize = plugin.shrinkBorderSafely(world, amount);

        if (newSize >= current) return;

        String msg = cfg.getPenaltyMessage(key)
            .replace("{player}", playerName)
            .replace("{amount}", String.format("%.0f", amount))
            .replace("{size}", String.format("%.0f", newSize));

        plugin.getServer().broadcastMessage(msg);
        plugin.getLogger().info("[Penalty:" + key + "] " + playerName
            + " triggered penalty — border shrunk by " + amount + " to " + newSize);
    }
}
