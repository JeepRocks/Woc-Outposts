package com.jeepy.wocoutposts.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import com.jeepy.wocoutposts.Main;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class ObjectiveManager {

    private final Main plugin;
    private final HashMap<UUID, Integer> playerHoldProgress;  // To track player progress
    private final HashMap<UUID, Scoreboard> playerScoreboards;  // To store player-specific scoreboards
    private Location outpostLocation;  // The location of the outpost
    private final int radius;  // Configurable radius from config.yml
    private final int progressRate;  // Configurable progress rate from config.yml
    private boolean isObjectiveActive;

    public ObjectiveManager(Main plugin) {
        this.plugin = plugin;
        this.playerHoldProgress = new HashMap<>();
        this.playerScoreboards = new HashMap<>();
        this.isObjectiveActive = false;

        // Load radius and progress rate from config.yml
        this.radius = plugin.getConfig().getInt("objective.radius", 10);  // Default to 10 if not specified
        this.progressRate = plugin.getConfig().getInt("objective.progressRate", 1);  // Default to 1 if not specified

        // Initialize the outpost location (can be dynamically set later)
        this.outpostLocation = new Location(Bukkit.getWorld("world"), 100, 64, 100);  // Example default location
    }

    // Method to set the outpost location dynamically
    public void setOutpostLocation(Location location) {
        this.outpostLocation = location;
    }

    public boolean isObjectiveActive() {
        return isObjectiveActive;
    }

    // Start the objective
    public void startObjective() {
        this.isObjectiveActive = true;
        Bukkit.broadcastMessage("Objective is now active: Hold the outpost!");
        playerHoldProgress.clear();  // Reset progress for all players
        playerScoreboards.clear();   // Clear old scoreboards

        // Activate the beacon if available at the outpost location
        activateBeacon();

        // Start the chest refill task
        plugin.getChestManager().refillChests(); // Refill chests at the start of the objective
        plugin.getChestManager().startChestRefillTask(); // Schedule periodic refills

        // Start a repeating task to check player progress every second (20 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isObjectiveActive) {
                    this.cancel();  // Stop the task when the objective ends
                }

                // Get all online players
                Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
                for (Player player : onlinePlayers) {
                    if (player.getLocation().distance(outpostLocation) <= radius) {
                        updateProgressIfPlayerInZone(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);  // Schedule to run every second (20 ticks)
    }

    // Stop the objective
    public void stopObjective() {
        this.isObjectiveActive = false;
        Bukkit.broadcastMessage("Objective has ended!");
        playerHoldProgress.clear();  // Clear progress when the objective ends

        // Clear all player scoreboards
        for (UUID playerId : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                clearPlayerScoreboard(player);
            }
        }
        playerScoreboards.clear();  // Clear stored scoreboards

        // Deactivate the beacon when the objective ends
        deactivateBeacon();
    }

    // Helper method to update progress if player is in the outpost zone
    private void updateProgressIfPlayerInZone(Player player) {
        if (!isObjectiveActive) return;

        UUID playerId = player.getUniqueId();
        if (isPlayerInOutpostArea(player)) {
            int currentProgress = playerHoldProgress.getOrDefault(playerId, 0);
            if (currentProgress < 100) {
                playerHoldProgress.put(playerId, currentProgress + progressRate);  // Progress rate from config.yml
                updatePlayerScoreboard(player, currentProgress + progressRate);
            }

            if (currentProgress >= 100) {
                Bukkit.broadcastMessage(player.getName() + " has completed the objective!");
                rewardPlayer(player);
                stopObjective();  // Optionally stop the objective after completion
            }
        } else {
            playerHoldProgress.remove(playerId);
            clearPlayerScoreboard(player);
        }
    }

    // Activate the beacon at the outpost location
    private void activateBeacon() {
        Location beaconLocation = outpostLocation.clone().add(0, -1, 0);
        if (beaconLocation.getBlock().getType() == Material.BEACON) {
            // You can simulate beacon activation here (e.g., check if beacon is powered)
            Bukkit.broadcastMessage("Beacon at the outpost is activated!");
        }
    }

    // Deactivate the beacon at the outpost location
    private void deactivateBeacon() {
        Location beaconLocation = outpostLocation.clone().add(0, -1, 0);
        if (beaconLocation.getBlock().getType() == Material.BEACON) {
            // You can simulate beacon deactivation here
            Bukkit.broadcastMessage("Beacon at the outpost is deactivated.");
        }
    }

    // Method to update the player's scoreboard with progress
    private void updatePlayerScoreboard(Player player, int progress) {
        Scoreboard board = playerScoreboards.getOrDefault(player.getUniqueId(), null);
        if (board == null) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            board = manager.getNewScoreboard();

            Objective objective = board.registerNewObjective("OutpostProgress", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            playerScoreboards.put(player.getUniqueId(), board);
        }

        Objective objective = board.getObjective("OutpostProgress");
        if (objective != null) {
            Score score = objective.getScore("Progress:");
            score.setScore(progress);
        }

        player.setScoreboard(board);
    }

    // Clear the player's scoreboard
    private void clearPlayerScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());  // Clear the scoreboard
    }

    // Helper method to check if the player is within the outpost area
    private boolean isPlayerInOutpostArea(Player player) {
        return player.getLocation().distance(outpostLocation) <= radius;
    }

    // Method to reward a player upon completing the objective
    private void rewardPlayer(Player player) {
        player.sendMessage("You have been rewarded for completing the objective!");
        // Add your reward logic here, e.g., player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
    }
}