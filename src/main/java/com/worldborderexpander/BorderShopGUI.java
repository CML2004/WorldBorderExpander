package com.worldborderexpander;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BorderShopGUI {

    public static final String GUI_TITLE_PREFIX = "§8⚙ §6World Border Shop";
    public static final String CONFIRM_TITLE_PREFIX = "§8Confirm Purchase: ";

    private final WorldBorderExpander plugin;

    public BorderShopGUI(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        BorderConfig cfg = plugin.getBorderConfig();
        List<ExpansionTier> tiers = cfg.getTiers();

        int rows = cfg.getGuiRows();
        int size = rows * 9;
        String title = cfg.getGuiTitle();

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill borders with glass panes
        ItemStack pane = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);
        for (int i = 0; i < rows; i++) {
            inv.setItem(i * 9, pane);
            inv.setItem(i * 9 + 8, pane);
        }

        // Info item: current border size
        String worldName = cfg.getWorldName();
        var world = Bukkit.getWorld(worldName);
        double currentSize = world != null ? world.getWorldBorder().getSize() : 0;
        double maxSize = cfg.getMaxBorderSize();

        ItemStack infoItem = buildItem(
                Material.COMPASS,
                "§e§lCurrent Border",
                List.of(
                        "§7Size: §f" + String.format("%.0f", currentSize) + " blocks",
                        "§7Max Size: §f" + String.format("%.0f", maxSize) + " blocks",
                        "",
                        "§7Your XP Level: §a" + player.getLevel()
                )
        );
        inv.setItem(4, infoItem);

        // Place tier items — start at slot 9+1 = 10 for interior
        int startSlot = 10;
        for (int i = 0; i < tiers.size() && i < (rows - 2) * 7; i++) {
            ExpansionTier tier = tiers.get(i);
            int row = i / 7;
            int col = i % 7;
            int slot = startSlot + row * 9 + col;

            boolean canAfford = player.getLevel() >= tier.getXpCost();
            boolean wouldExceedMax = currentSize + tier.getExpansionBlocks() > maxSize;

            Material mat;
            try {
                mat = Material.valueOf(tier.getIconMaterial().toUpperCase());
            } catch (IllegalArgumentException e) {
                mat = Material.GRASS_BLOCK;
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7" + tier.getDescription());
            lore.add("");
            lore.add("§7Expands border by: §b+" + String.format("%.0f", tier.getExpansionBlocks()) + " blocks");
            lore.add("§7Cost: §e" + tier.getXpCost() + " XP Levels");
            lore.add("");
            if (wouldExceedMax) {
                lore.add("§c✗ Would exceed max border size!");
            } else if (canAfford) {
                lore.add("§a✔ Click to purchase!");
            } else {
                lore.add("§c✗ Not enough XP levels!");
                lore.add("§7Need §e" + tier.getXpCost() + "§7, have §e" + player.getLevel());
            }

            String displayName = (canAfford && !wouldExceedMax ? "§a" : "§c") + tier.getDisplayName();
            ItemStack item = buildItem(mat, displayName, lore);

            // Gray out if can't afford
            if (!canAfford || wouldExceedMax) {
                item = applyGrayOut(item);
            }

            inv.setItem(slot, item);
        }

        player.openInventory(inv);
    }

    public void openConfirm(Player player, ExpansionTier tier) {
        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_TITLE_PREFIX + tier.getDisplayName());

        ItemStack pane = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // Purchase info in center
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Expand border by: §b+" + String.format("%.0f", tier.getExpansionBlocks()) + " blocks");
        infoLore.add("§7Cost: §e" + tier.getXpCost() + " XP Levels");
        infoLore.add("");
        infoLore.add("§7Your XP Level: §a" + player.getLevel());
        ItemStack infoItem = buildItem(Material.PAPER, "§e§lPurchase Details", infoLore);
        inv.setItem(13, infoItem);

        // Confirm (green wool, slot 11)
        ItemStack confirm = buildItem(
                Material.LIME_WOOL,
                "§a§l✔ Confirm",
                List.of("§7Click to expand the world border!")
        );
        inv.setItem(11, confirm);

        // Cancel (red wool, slot 15)
        ItemStack cancel = buildItem(
                Material.RED_WOOL,
                "§c§l✗ Cancel",
                List.of("§7Click to go back.")
        );
        inv.setItem(15, cancel);

        player.openInventory(inv);
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack applyGrayOut(ItemStack item) {
        // Replace with light gray stained glass version if possible,
        // otherwise just return as-is (material change would lose context)
        // We keep the original item but dim via name color (done at name building)
        return item;
    }
}
