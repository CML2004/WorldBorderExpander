package com.worldborderexpander;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;

public class BorderConfig {

    private final WorldBorderExpander plugin;

    public BorderConfig(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() { return plugin.getConfig(); }

    public String getWorldName() { return cfg().getString("world", "world"); }
    public double getStartingSize() { return cfg().getDouble("starting-size", 100); }
    public double getMaxBorderSize() { return cfg().getDouble("max-border-size", 10000); }

    public boolean isAutoGrowthEnabled() { return cfg().getBoolean("auto-growth.enabled", true); }
    public double getGrowthAmount() { return cfg().getDouble("auto-growth.blocks-per-day", 1); }
    public boolean isPauseWhenEmpty() { return cfg().getBoolean("auto-growth.pause-when-empty", true); }
    public boolean isPauseWhenAllAfk() { return cfg().getBoolean("auto-growth.pause-when-all-afk", true); }

    public int getAfkMinutes() { return cfg().getInt("afk.minutes-until-afk", 5); }

    public String getGuiTitle() { return cfg().getString("gui.title", "§8⚙ §6World Border Shop"); }
    public int getGuiRows() { return Math.max(1, Math.min(6, cfg().getInt("gui.rows", 3))); }
    public boolean isConfirmEnabled() { return cfg().getBoolean("gui.confirm-purchase", true); }

    public List<ExpansionTier> getTiers() {
        List<ExpansionTier> tiers = new ArrayList<>();
        var section = cfg().getConfigurationSection("tiers");
        if (section == null) return tiers;
        for (String key : section.getKeys(false)) {
            String p = "tiers." + key;
            tiers.add(new ExpansionTier(
                key,
                cfg().getString(p + ".name", key),
                cfg().getInt(p + ".xp-cost", 10),
                cfg().getDouble(p + ".expansion-blocks", 10),
                cfg().getString(p + ".icon", "GRASS_BLOCK"),
                cfg().getString(p + ".description", "Expand the world border")
            ));
        }
        return tiers;
    }

    public boolean isPenaltyEnabled(String key) {
        return cfg().getBoolean("penalties." + key + ".enabled", true);
    }
    public double getPenaltyAmount(String key) {
        return cfg().getDouble("penalties." + key + ".blocks", 5);
    }
    public String getPenaltyMessage(String key) {
        return cfg().getString("penalties." + key + ".message", "§cThe world border shrank by {amount} blocks!");
    }

    public String getBroadcastMessage() {
        return cfg().getString("broadcast-message",
            "§6[Border] §e{player} §7expanded the world border to §f{size} §7blocks!");
    }

    public String getString(String path, String def) {
        return cfg().getString(path, def);
    }

    public void set(String path, Object value) {
        cfg().set(path, value);
        plugin.saveConfig();
    }
}
