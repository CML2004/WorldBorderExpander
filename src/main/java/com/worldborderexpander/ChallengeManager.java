package com.worldborderexpander;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ChallengeManager implements Listener {

    private final WorldBorderExpander plugin;
    private BukkitTask task;
    private ChallengeInstance currentChallenge;
    private long countdownSeconds;
    private final Random random = new Random();

    private static final Set<EntityType> HOSTILE_MOBS = Set.of(
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.CREEPER,
        EntityType.SPIDER,
        EntityType.CAVE_SPIDER,
        EntityType.ENDERMAN,
        EntityType.BLAZE,
        EntityType.GHAST,
        EntityType.PILLAGER,
        EntityType.VINDICATOR,
        EntityType.EVOKER,
        EntityType.WITCH,
        EntityType.SLIME,
        EntityType.MAGMA_CUBE,
        EntityType.PHANTOM,
        EntityType.DROWNED,
        EntityType.HUSK,
        EntityType.STRAY,
        EntityType.WITHER_SKELETON,
        EntityType.ZOMBIE_VILLAGER,
        EntityType.SKELETON_HORSE,
        EntityType.ZOMBIE_HORSE
    );

    public ChallengeManager(WorldBorderExpander plugin) {
        this.plugin = plugin;
        pickNewCountdown();
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("random-challenges.enabled", true)) {
            return;
        }

        if (currentChallenge == null) {
            if (plugin.getBorderConfig().isRequireActivePlayers() && !hasActivePlayers()) {
                return;
            }
            if (countdownSeconds > 0) {
                countdownSeconds--;
                return;
            }
            startRandomChallenge();
            return;
        }

        if (plugin.getBorderConfig().isRequireActivePlayers() && !hasActivePlayers()) {
            return;
        }

        currentChallenge.tick();
        if (currentChallenge.type == ChallengeType.REACH_LEVEL) {
            updateReachLevelProgress();
        }
        if (currentChallenge.isComplete()) {
            completeChallenge(true);
            return;
        }

        if (currentChallenge.isExpired()) {
            completeChallenge(false);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (currentChallenge == null || currentChallenge.type != ChallengeType.KILL_ENTITIES) {
            return;
        }

        if (event.getEntity().getKiller() == null) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (plugin.getAfkManager().isAfk(killer)) {
            return;
        }

        if (currentChallenge.matchesEntity(event.getEntityType())) {
            currentChallenge.addProgress(1);
            broadcastProgressIfNeeded();
            if (currentChallenge.isComplete()) {
                completeChallenge(true);
            }
        }
    }

    public void contributeXp(Player player, int amount) {
        if (currentChallenge == null || currentChallenge.type != ChallengeType.PAY_XP) {
            player.sendMessage("§cThere is no active pay-XP challenge right now.");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("§cEnter a positive XP amount to contribute.");
            return;
        }
        if (player.getLevel() < amount) {
            player.sendMessage("§cYou do not have enough XP levels.");
            return;
        }

        player.setLevel(player.getLevel() - amount);
        currentChallenge.addProgress(amount);
        player.sendMessage("§aYou contributed §e" + amount + " §aXP levels to the challenge.");
        broadcastProgressIfNeeded();

        if (currentChallenge.isComplete()) {
            completeChallenge(true);
        }
    }

    public void sendStatus(org.bukkit.command.CommandSender sender) {
        if (currentChallenge == null) {
            sender.sendMessage("§6§l── Border Challenge Status ────");
            sender.sendMessage("§7No challenge is active right now.");
            sender.sendMessage("§7Next challenge will begin in §f" + formatTime(countdownSeconds) + "§7 of active playtime.");
            return;
        }

        sender.sendMessage("§6§l── Border Challenge Active ────");
        sender.sendMessage("§e" + currentChallenge.displayName);
        sender.sendMessage("§7Objective: §f" + currentChallenge.getObjectiveText());
        sender.sendMessage("§7Progress: §f" + currentChallenge.getProgressText());
        sender.sendMessage("§7Time remaining: §f" + formatTime(currentChallenge.remainingSeconds));
        sender.sendMessage("§7Failure penalty: §f" + currentChallenge.failureShrinkAmount + " §7blocks");
    }

    private void startRandomChallenge() {
        List<ChallengeInstance> candidates = new ArrayList<>();
        if (plugin.getConfig().getBoolean("random-challenges.pay-xp.enabled", false)) {
            candidates.add(createPayXpChallenge());
        }
        if (plugin.getConfig().getBoolean("random-challenges.kill-entities.enabled", false)) {
            candidates.add(createKillEntitiesChallenge());
        }
        if (plugin.getConfig().getBoolean("random-challenges.reach-level.enabled", false)) {
            candidates.add(createReachLevelChallenge());
        }

        if (candidates.isEmpty()) {
            pickNewCountdown();
            return;
        }

        currentChallenge = candidates.get(random.nextInt(candidates.size()));
        currentChallenge.remainingSeconds = currentChallenge.durationSeconds;
        String startMessage = "§6A Border Challenge has begun: §e" + currentChallenge.displayName + "§6! §7"
            + currentChallenge.getStartMessage();
        Bukkit.getServer().broadcastMessage(startMessage);
        plugin.getLogger().info("Started challenge: " + currentChallenge.displayName);

        if (currentChallenge.isComplete()) {
            completeChallenge(true);
        }
    }

    private void completeChallenge(boolean success) {
        World world = Bukkit.getWorld(plugin.getBorderConfig().getWorldName());
        if (success) {
            Bukkit.getServer().broadcastMessage("§aChallenge completed. The border remains stable.");
        } else {
            if (world != null) {
                plugin.shrinkBorderSafely(world, currentChallenge.failureShrinkAmount);
                Bukkit.getServer().broadcastMessage("§cChallenge failed. The border shrinks by §f" + currentChallenge.failureShrinkAmount + " §cblocks.");
            } else {
                Bukkit.getServer().broadcastMessage("§cChallenge failed. The border would have shrunk, but the configured world is unavailable.");
            }
        }
        currentChallenge = null;
        pickNewCountdown();
    }

    private void broadcastProgressIfNeeded() {
        if (currentChallenge == null) {
            return;
        }
        String progress = currentChallenge.getProgressText();
        Bukkit.getServer().broadcastMessage("§e" + currentChallenge.displayName + ": §f" + progress + "§e.");
    }

    private void updateReachLevelProgress() {
        int highestLevel = 0;
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (plugin.getAfkManager().isAfk(player)) {
                continue;
            }
            highestLevel = Math.max(highestLevel, player.getLevel());
        }
        currentChallenge.progressAmount = highestLevel;
    }

    private boolean hasActivePlayers() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (!plugin.getAfkManager().isAfk(player)) {
                return true;
            }
        }
        return false;
    }

    private void pickNewCountdown() {
        int minMinutes = plugin.getConfig().getInt("random-challenges.min-active-minutes-between-challenges", 30);
        int maxMinutes = plugin.getConfig().getInt("random-challenges.max-active-minutes-between-challenges", 60);
        if (minMinutes < 1) {
            minMinutes = 1;
        }
        if (maxMinutes < minMinutes) {
            maxMinutes = minMinutes;
        }
        countdownSeconds = minMinutes * 60L + random.nextInt((maxMinutes - minMinutes) * 60 + 1);
    }

    private ChallengeInstance createPayXpChallenge() {
        String displayName = plugin.getConfig().getString("random-challenges.pay-xp.display-name", "Border Tribute");
        int duration = plugin.getConfig().getInt("random-challenges.pay-xp.duration-minutes", 5) * 60;
        int requiredLevels = plugin.getConfig().getInt("random-challenges.pay-xp.required-levels", 20);
        int failureShrink = plugin.getConfig().getInt("random-challenges.pay-xp.failure-shrink-amount", 10);
        return new ChallengeInstance(ChallengeType.PAY_XP, displayName, duration, failureShrink, requiredLevels, "Pay " + requiredLevels + " XP levels before time runs out.");
    }

    private ChallengeInstance createKillEntitiesChallenge() {
        String displayName = plugin.getConfig().getString("random-challenges.kill-entities.display-name", "Monster Migration");
        int duration = plugin.getConfig().getInt("random-challenges.kill-entities.duration-minutes", 10) * 60;
        String entityType = plugin.getConfig().getString("random-challenges.kill-entities.entity-type", "ZOMBIE");
        int requiredKills = plugin.getConfig().getInt("random-challenges.kill-entities.required-kills", 25);
        int failureShrink = plugin.getConfig().getInt("random-challenges.kill-entities.failure-shrink-amount", 10);
        return new ChallengeInstance(ChallengeType.KILL_ENTITIES, displayName, duration, failureShrink, requiredKills, entityType, "Kill " + requiredKills + " " + entityType + "s before time runs out.");
    }

    private ChallengeInstance createReachLevelChallenge() {
        String displayName = plugin.getConfig().getString("random-challenges.reach-level.display-name", "Power Surge");
        int duration = plugin.getConfig().getInt("random-challenges.reach-level.duration-minutes", 10) * 60;
        int requiredLevel = plugin.getConfig().getInt("random-challenges.reach-level.required-level", 15);
        int failureShrink = plugin.getConfig().getInt("random-challenges.reach-level.failure-shrink-amount", 10);
        return new ChallengeInstance(ChallengeType.REACH_LEVEL, displayName, duration, failureShrink, requiredLevel, "Reach level " + requiredLevel + " before time expires.");
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remaining = seconds % 60;
        return String.format("%d:%02d", minutes, remaining);
    }

    private static class ChallengeInstance {
        private final ChallengeType type;
        private final String displayName;
        private final int durationSeconds;
        private final int failureShrinkAmount;
        private final String objective;
        private final String entityTypeName;
        private int remainingSeconds;
        private int requiredAmount;
        private int progressAmount;

        ChallengeInstance(ChallengeType type, String displayName, int durationSeconds, int failureShrinkAmount, int requiredAmount, String objective) {
            this(type, displayName, durationSeconds, failureShrinkAmount, requiredAmount, null, objective);
        }

        ChallengeInstance(ChallengeType type, String displayName, int durationSeconds, int failureShrinkAmount, int requiredAmount, String entityTypeName, String objective) {
            this.type = type;
            this.displayName = displayName;
            this.durationSeconds = durationSeconds;
            this.failureShrinkAmount = failureShrinkAmount;
            this.requiredAmount = Math.max(1, requiredAmount);
            this.entityTypeName = entityTypeName;
            this.objective = objective;
            this.remainingSeconds = durationSeconds;
            this.progressAmount = 0;
        }

        void tick() {
            if (remainingSeconds > 0) {
                remainingSeconds--;
            }
        }

        boolean isComplete() {
            return switch (type) {
                case PAY_XP, KILL_ENTITIES -> progressAmount >= requiredAmount;
                case REACH_LEVEL -> progressAmount >= requiredAmount;
            };
        }

        boolean isExpired() {
            return remainingSeconds <= 0 && !isComplete();
        }

        void addProgress(int amount) {
            progressAmount += amount;
            if (progressAmount > requiredAmount) {
                progressAmount = requiredAmount;
            }
        }

        boolean matchesEntity(EntityType entityType) {
            if (type != ChallengeType.KILL_ENTITIES) {
                return false;
            }
            if (entityTypeName == null) {
                return false;
            }
            if (entityTypeName.equalsIgnoreCase("HOSTILE_MOBS")) {
                return HOSTILE_MOBS.contains(entityType);
            }
            try {
                return EntityType.valueOf(entityTypeName.toUpperCase()) == entityType;
            } catch (IllegalArgumentException e) {
                return entityType == EntityType.ZOMBIE;
            }
        }

        String getObjectiveText() {
            return objective;
        }

        String getProgressText() {
            return switch (type) {
                case PAY_XP -> progressAmount + "/" + requiredAmount + " XP paid";
                case KILL_ENTITIES -> progressAmount + "/" + requiredAmount + " kills";
                case REACH_LEVEL -> "Highest active player level: " + progressAmount + "/" + requiredAmount;
            };
        }

        String getStartMessage() {
            return objective + " Fail and the border will shrink by " + failureShrinkAmount + " blocks.";
        }
    }

    private enum ChallengeType {
        PAY_XP,
        KILL_ENTITIES,
        REACH_LEVEL
    }
}
