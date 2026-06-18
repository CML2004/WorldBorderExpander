package com.worldborderexpander;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class BorderConfig {

    private final WorldBorderExpander plugin;

    public BorderConfig(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    public List<ExpansionTier> getTiers() {
        List<ExpansionTier> tiers = new ArrayList<>();
        var tiersSection = cfg().getConfigurationSection("tiers");
        if (tiersSection == null) return tiers;

        for (String key : tiersSection.getKeys(false)) {
            String path = "tiers." + key;
            String name = cfg().getString(path + ".name", key);
            int xpCost = cfg().getInt(path + ".xp-cost", 10);
            double expansionAmount = cfg().getDouble(path + ".expansion-blocks", 100);
            String material = cfg().getString(path + ".icon", "GRASS_BLOCK");
            String description = cfg().getString(path + ".description", "Expand the world border");
            tiers.add(new ExpansionTier(key, name, xpCost, expansionAmount, material, description));
        }

        return tiers;
    }

    public int getGuiRows() {
        return Math.max(1, Math.min(6, cfg().getInt("gui.rows", 3)));
    }

    public String getGuiTitle() {
        return cfg().getString("gui.title", "§8⚙ §6World Border Shop");
    }

    public String getWorldName() {
        return cfg().getString("world", "world");
    }

    public double getMaxBorderSize() {
        return cfg().getDouble("max-border-size", 10000);
    }

    public boolean isConfirmEnabled() {
        return cfg().getBoolean("gui.confirm-purchase", true);
    }
}
