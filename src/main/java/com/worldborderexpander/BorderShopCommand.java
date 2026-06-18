package com.worldborderexpander;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BorderShopCommand implements CommandExecutor {

    private final WorldBorderExpander plugin;

    public BorderShopCommand(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worldborderexpander.reload")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("§aWorldBorderExpander config reloaded!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("worldborderexpander.use")) {
            player.sendMessage("§cYou don't have permission to use the border shop.");
            return true;
        }

        BorderShopGUI gui = new BorderShopGUI(plugin);
        gui.open(player);
        return true;
    }
}
