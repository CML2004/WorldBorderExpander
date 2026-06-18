package com.worldborderexpander;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final WorldBorderExpander plugin;

    // Track which tier a player is confirming
    private final Map<UUID, ExpansionTier> pendingConfirmations = new HashMap<>();

    public GUIListener(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        BorderConfig cfg = plugin.getBorderConfig();

        // ── Main shop GUI ──────────────────────────────────────────────
        if (title.equals(cfg.getGuiTitle())) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

            String displayName = clicked.getItemMeta().getDisplayName();

            // Find which tier was clicked by matching display name
            List<ExpansionTier> tiers = cfg.getTiers();
            for (ExpansionTier tier : tiers) {
                String tierName = tier.getDisplayName();
                // Strip color from clicked name to compare
                if (displayName.contains(tierName)) {
                    handleTierClick(player, tier);
                    return;
                }
            }
        }

        // ── Confirm purchase GUI ────────────────────────────────────────
        if (title.startsWith(BorderShopGUI.CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

            String name = clicked.getItemMeta().getDisplayName();

            if (name.contains("Confirm")) {
                ExpansionTier tier = pendingConfirmations.get(player.getUniqueId());
                if (tier != null) {
                    executePurchase(player, tier);
                    pendingConfirmations.remove(player.getUniqueId());
                }
                player.closeInventory();

            } else if (name.contains("Cancel")) {
                pendingConfirmations.remove(player.getUniqueId());
                player.closeInventory();
                // Reopen main shop after a tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new BorderShopGUI(plugin).open(player);
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        // Clean up if player closes confirm GUI without choosing
        if (title.startsWith(BorderShopGUI.CONFIRM_TITLE_PREFIX)) {
            pendingConfirmations.remove(player.getUniqueId());
        }
    }

    private void handleTierClick(Player player, ExpansionTier tier) {
        BorderConfig cfg = plugin.getBorderConfig();

        if (!player.hasPermission("worldborderexpander.use")) {
            player.sendMessage("§cYou don't have permission to use the border shop.");
            player.closeInventory();
            return;
        }

        if (player.getLevel() < tier.getXpCost()) {
            player.sendMessage("§cYou don't have enough XP levels! Need §e" + tier.getXpCost()
                    + " §clevels, but you only have §e" + player.getLevel() + "§c.");
            return;
        }

        World world = Bukkit.getWorld(cfg.getWorldName());
        if (world == null) {
            player.sendMessage("§cError: Target world not found. Contact an admin.");
            return;
        }

        double current = world.getWorldBorder().getSize();
        if (current + tier.getExpansionBlocks() > cfg.getMaxBorderSize()) {
            player.sendMessage("§cThe border is already at or near the maximum size!");
            return;
        }

        if (cfg.isConfirmEnabled()) {
            pendingConfirmations.put(player.getUniqueId(), tier);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new BorderShopGUI(plugin).openConfirm(player, tier);
            }, 1L);
        } else {
            executePurchase(player, tier);
            player.closeInventory();
        }
    }

    private void executePurchase(Player player, ExpansionTier tier) {
        BorderConfig cfg = plugin.getBorderConfig();

        // Re-validate everything at time of purchase
        if (player.getLevel() < tier.getXpCost()) {
            player.sendMessage("§cPurchase failed: not enough XP levels.");
            return;
        }

        World world = Bukkit.getWorld(cfg.getWorldName());
        if (world == null) {
            player.sendMessage("§cError: Target world not found.");
            return;
        }

        WorldBorder border = world.getWorldBorder();
        double current = border.getSize();
        double newSize = current + tier.getExpansionBlocks();

        if (newSize > cfg.getMaxBorderSize()) {
            player.sendMessage("§cPurchase failed: would exceed max border size.");
            return;
        }

        // Deduct XP levels
        player.setLevel(player.getLevel() - tier.getXpCost());

        // Expand border with a smooth animation (over 3 seconds)
        border.setSize(newSize, 3L);

        player.sendMessage("§a§l✔ Border Expanded!");
        player.sendMessage("§7The world border grew by §b+" + String.format("%.0f", tier.getExpansionBlocks())
                + " blocks§7 to §f" + String.format("%.0f", newSize) + " blocks§7.");
        player.sendMessage("§7Remaining XP levels: §e" + player.getLevel());

        // Broadcast to all players
        String broadcast = plugin.getConfig().getString(
                "broadcast-message",
                "§6[Border] §e{player} §7expanded the world border to §f{size} §7blocks!"
        );
        if (!broadcast.isEmpty()) {
            String msg = broadcast
                    .replace("{player}", player.getName())
                    .replace("{size}", String.format("%.0f", newSize))
                    .replace("{expansion}", String.format("%.0f", tier.getExpansionBlocks()))
                    .replace("{tier}", tier.getDisplayName());
            Bukkit.broadcastMessage(msg);
        }

        plugin.getLogger().info(player.getName() + " purchased '" + tier.getDisplayName()
                + "' — border expanded to " + newSize);
    }
}
