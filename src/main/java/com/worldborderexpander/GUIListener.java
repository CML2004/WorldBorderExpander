package com.worldborderexpander;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final WorldBorderExpander plugin;
    private final Map<UUID, ExpansionTier> pendingConfirmations = new HashMap<>();

    public GUIListener(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        BorderConfig cfg = plugin.getBorderConfig();

        if (title.equals(cfg.getGuiTitle())) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
            String displayName = clicked.getItemMeta().getDisplayName();
            for (ExpansionTier tier : cfg.getTiers()) {
                if (displayName.contains(tier.getDisplayName())) {
                    handleTierClick(player, tier);
                    return;
                }
            }
        }

        if (title.startsWith(BorderShopGUI.CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Confirm")) {
                ExpansionTier tier = pendingConfirmations.remove(player.getUniqueId());
                if (tier != null) executePurchase(player, tier);
                player.closeInventory();
            } else if (name.contains("Cancel")) {
                pendingConfirmations.remove(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> new BorderShopGUI(plugin).open(player), 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().getTitle().startsWith(BorderShopGUI.CONFIRM_TITLE_PREFIX)) {
            pendingConfirmations.remove(player.getUniqueId());
        }
    }

    private void handleTierClick(Player player, ExpansionTier tier) {
        BorderConfig cfg = plugin.getBorderConfig();
        if (!player.hasPermission("wbe.use")) {
            player.sendMessage("§cYou don't have permission to use the border shop.");
            return;
        }
        if (player.getLevel() < tier.getXpCost()) {
            player.sendMessage("§cNot enough XP levels! Need §e" + tier.getXpCost()
                + "§c, have §e" + player.getLevel() + "§c.");
            return;
        }
        World world = Bukkit.getWorld(cfg.getWorldName());
        if (world == null) { player.sendMessage("§cError: world not found."); return; }
        if (world.getWorldBorder().getSize() + tier.getExpansionBlocks() > cfg.getMaxBorderSize()) {
            player.sendMessage("§cThe border is at or near maximum size!");
            return;
        }
        if (cfg.isConfirmEnabled()) {
            pendingConfirmations.put(player.getUniqueId(), tier);
            Bukkit.getScheduler().runTaskLater(plugin, () -> new BorderShopGUI(plugin).openConfirm(player, tier), 1L);
        } else {
            executePurchase(player, tier);
            player.closeInventory();
        }
    }

    private void executePurchase(Player player, ExpansionTier tier) {
        BorderConfig cfg = plugin.getBorderConfig();
        if (player.getLevel() < tier.getXpCost()) {
            player.sendMessage("§cPurchase failed: not enough XP levels.");
            return;
        }
        World world = Bukkit.getWorld(cfg.getWorldName());
        if (world == null) { player.sendMessage("§cError: world not found."); return; }
        WorldBorder border = world.getWorldBorder();
        double newSize = border.getSize() + tier.getExpansionBlocks();
        if (newSize > cfg.getMaxBorderSize()) {
            player.sendMessage("§cPurchase failed: would exceed max size.");
            return;
        }
        player.setLevel(player.getLevel() - tier.getXpCost());
        border.setSize(newSize);
        player.sendMessage("§a§l✔ Border Expanded!");
        player.sendMessage("§7Grew by §b+" + String.format("%.0f", tier.getExpansionBlocks())
            + " §7blocks to §f" + String.format("%.0f", newSize) + "§7.");
        player.sendMessage("§7Remaining XP: §e" + player.getLevel());
        String broadcast = cfg.getBroadcastMessage();
        if (!broadcast.isEmpty()) {
            Bukkit.broadcastMessage(broadcast
                .replace("{player}", player.getName())
                .replace("{size}", String.format("%.0f", newSize))
                .replace("{expansion}", String.format("%.0f", tier.getExpansionBlocks()))
                .replace("{tier}", tier.getDisplayName()));
        }
        plugin.getLogger().info(player.getName() + " purchased '" + tier.getDisplayName()
            + "' — border now " + newSize);
    }
}
