package com.worldborderexpander;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BorderCommand implements CommandExecutor {

    private final WorldBorderExpander plugin;

    public BorderCommand(WorldBorderExpander plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /bordershop — open the shop GUI
        if (command.getName().equalsIgnoreCase("bordershop")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                return handleReload(sender);
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can open the shop.");
                return true;
            }
            if (!player.hasPermission("wbe.use")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            new BorderShopGUI(plugin).open(player);
            return true;
        }

        // /borderchallenge — view the current challenge or contribute XP
        if (command.getName().equalsIgnoreCase("borderchallenge")) {
            return handleChallengeCommand(sender, args);
        }

        // /border <subcommand> — admin commands
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "set" -> handleSet(sender, args);
            case "growth" -> handleGrowth(sender, args);
            case "penalty" -> handlePenalty(sender, args);
            case "afk" -> handleAfk(sender, args);
            case "status" -> handleStatus(sender);
            case "reset" -> handleReset(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    // /border reload
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        plugin.reloadConfig();
        sender.sendMessage("§aWorldBorderExpander config reloaded!");
        return true;
    }

    // /border status
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        BorderConfig cfg = plugin.getBorderConfig();
        World world = Bukkit.getWorld(cfg.getWorldName());
        double size = world != null ? world.getWorldBorder().getSize() : -1;

        sender.sendMessage("§6§l── World Border Status ──────────────");
        sender.sendMessage("§7World: §f" + cfg.getWorldName());
        sender.sendMessage("§7Current size: §f" + String.format("%.1f", size) + " blocks");
        sender.sendMessage("§7Starting size: §f" + cfg.getStartingSize());
        sender.sendMessage("§7Max size: §f" + cfg.getMaxBorderSize());
        sender.sendMessage("§7Auto growth: §f" + (cfg.isAutoGrowthEnabled() ? "§aEnabled" : "§cDisabled")
            + " §7(§f" + cfg.getGrowthAmount() + " §7blocks/day)");
        sender.sendMessage("§7Pause when empty: §f" + cfg.isPauseWhenEmpty());
        sender.sendMessage("§7Pause when all AFK: §f" + cfg.isPauseWhenAllAfk());
        sender.sendMessage("§7AFK timeout: §f" + cfg.getAfkMinutes() + " minutes");

        // Show AFK status of online players
        sender.sendMessage("§6§l── Player AFK Status ────────────────");
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean afk = plugin.getAfkManager().isAfk(p);
            sender.sendMessage("§7" + p.getName() + ": " + (afk ? "§cAFK" : "§aActive"));
        }
        return true;
    }

    // /border reset
    private boolean handleReset(CommandSender sender) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        BorderConfig cfg = plugin.getBorderConfig();
        World world = Bukkit.getWorld(cfg.getWorldName());
        if (world == null) { sender.sendMessage("§cWorld not found."); return true; }
        double start = cfg.getStartingSize();
        world.getWorldBorder().setSize(start);
        sender.sendMessage("§aBorder reset to starting size: §f" + start + " blocks.");
        return true;
    }

    // /border set <starting|max|world> <value>
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /border set <starting|max|world> <value>");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "starting" -> {
                double val = parseDouble(sender, args[2]); if (val < 0) return true;
                plugin.getBorderConfig().set("starting-size", val);
                sender.sendMessage("§aStarting size set to §f" + val);
            }
            case "max" -> {
                double val = parseDouble(sender, args[2]); if (val < 0) return true;
                plugin.getBorderConfig().set("max-border-size", val);
                sender.sendMessage("§aMax size set to §f" + val);
            }
            case "world" -> {
                plugin.getBorderConfig().set("world", args[2]);
                sender.sendMessage("§aWorld set to §f" + args[2]);
            }
            default -> sender.sendMessage("§cUnknown setting. Use: starting, max, world");
        }
        return true;
    }

    // /border growth <enable|disable|amount|pause-empty|pause-afk> [value]
    private boolean handleGrowth(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /border growth <enable|disable|amount|pause-empty|pause-afk> [value]");
            return true;
        }
        BorderConfig cfg = plugin.getBorderConfig();
        switch (args[1].toLowerCase()) {
            case "enable" -> { cfg.set("auto-growth.enabled", true); sender.sendMessage("§aAuto growth enabled."); }
            case "disable" -> { cfg.set("auto-growth.enabled", false); sender.sendMessage("§cAuto growth disabled."); }
            case "amount" -> {
                if (args.length < 3) { sender.sendMessage("§cProvide a value."); return true; }
                double val = parseDouble(sender, args[2]); if (val < 0) return true;
                cfg.set("auto-growth.blocks-per-day", val);
                sender.sendMessage("§aGrowth amount set to §f" + val + " §ablocks/day.");
            }
            case "pause-empty" -> {
                if (args.length < 3) { sender.sendMessage("§cProvide true or false."); return true; }
                boolean val = Boolean.parseBoolean(args[2]);
                cfg.set("auto-growth.pause-when-empty", val);
                sender.sendMessage("§aPause when empty set to §f" + val);
            }
            case "pause-afk" -> {
                if (args.length < 3) { sender.sendMessage("§cProvide true or false."); return true; }
                boolean val = Boolean.parseBoolean(args[2]);
                cfg.set("auto-growth.pause-when-all-afk", val);
                sender.sendMessage("§aPause when all AFK set to §f" + val);
            }
            default -> sender.sendMessage("§cUnknown option.");
        }
        return true;
    }

    // /border penalty <key> <enable|disable|amount|message> [value]
    // e.g. /border penalty death.fall amount 10
    // e.g. /border penalty death.pvp enable
    // e.g. /border penalty leave-overworld message §cYou left the overworld! -{amount} blocks!
    private boolean handlePenalty(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /border penalty <key> <enable|disable|amount|message> [value]");
            sender.sendMessage("§7Keys: death.fall, death.drowning, death.lava, death.fire,");
            sender.sendMessage("§7      death.mob, death.pvp, death.void, death.magic,");
            sender.sendMessage("§7      death.wither, death.lightning, death.explosion,");
            sender.sendMessage("§7      death.starvation, death.other, leave-overworld");
            return true;
        }
        String key = args[1];
        BorderConfig cfg = plugin.getBorderConfig();
        switch (args[2].toLowerCase()) {
            case "enable" -> { cfg.set("penalties." + key + ".enabled", true); sender.sendMessage("§aPenalty §f" + key + " §aenabled."); }
            case "disable" -> { cfg.set("penalties." + key + ".enabled", false); sender.sendMessage("§cPenalty §f" + key + " §cdisabled."); }
            case "amount" -> {
                if (args.length < 4) { sender.sendMessage("§cProvide a value."); return true; }
                double val = parseDouble(sender, args[3]); if (val < 0) return true;
                cfg.set("penalties." + key + ".blocks", val);
                sender.sendMessage("§aPenalty §f" + key + " §aset to §f-" + val + " §ablocks.");
            }
            case "message" -> {
                if (args.length < 4) { sender.sendMessage("§cProvide a message."); return true; }
                String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                cfg.set("penalties." + key + ".message", msg);
                sender.sendMessage("§aPenalty message for §f" + key + " §aupdated.");
            }
            default -> sender.sendMessage("§cUnknown option. Use: enable, disable, amount, message");
        }
        return true;
    }

    // /border afk <minutes> 
    private boolean handleAfk(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wbe.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /border afk <minutes>"); return true; }
        double val = parseDouble(sender, args[1]); if (val < 0) return true;
        plugin.getBorderConfig().set("afk.minutes-until-afk", (int) val);
        sender.sendMessage("§aAFK timeout set to §f" + (int) val + " §aminutes.");
        return true;
    }

    private boolean handleChallengeCommand(CommandSender sender, String[] args) {
        var manager = plugin.getChallengeManager();
        if (args.length == 0) {
            manager.sendStatus(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can contribute XP.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /borderchallenge pay <amount|all>");
                return true;
            }
            String amountArg = args[1];
            int amount;
            if (amountArg.equalsIgnoreCase("all")) {
                amount = player.getLevel();
            } else {
                double val = parseDouble(sender, amountArg);
                if (val < 0) return true;
                amount = (int) val;
            }
            manager.contributeXp(player, amount);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§6§l── Border Challenge Commands ────");
            sender.sendMessage("§e/borderchallenge §7— View the current challenge");
            sender.sendMessage("§e/borderchallenge pay <amount|all> §7— Contribute XP to a pay-XP challenge");
            return true;
        }

        sender.sendMessage("§cUnknown borderchallenge command. Use /borderchallenge or /borderchallenge help.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l── WorldBorderExpander Commands ────");
        sender.sendMessage("§e/bordershop §7— Open the XP shop");
        sender.sendMessage("§e/borderchallenge §7— View the current border challenge");
        sender.sendMessage("§e/border status §7— View current border info");
        sender.sendMessage("§e/border reload §7— Reload config");
        sender.sendMessage("§e/border reset §7— Reset border to starting size");
        sender.sendMessage("§e/border set <starting|max|world> <value>");
        sender.sendMessage("§e/border growth <enable|disable|amount|pause-empty|pause-afk> [value]");
        sender.sendMessage("§e/border penalty <key> <enable|disable|amount|message> [value]");
        sender.sendMessage("§e/border afk <minutes>");
    }

    private double parseDouble(CommandSender sender, String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { sender.sendMessage("§c'" + s + "' is not a valid number."); return -1; }
    }
}
