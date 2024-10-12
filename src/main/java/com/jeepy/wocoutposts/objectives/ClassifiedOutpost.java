package com.jeepy.wocoutposts.objectives;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.database.OutpostDatabaseManager;
import com.jeepy.wocoutposts.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.sql.SQLException;
import java.util.logging.Level;

public class ClassifiedOutpost implements Runnable, Listener {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final OutpostDatabaseManager databaseManager;
    private final Location beaconLocation;
    private BukkitTask checkingTask;  // Task for checking player proximity
    private boolean isActive;         // Whether the outpost is active
    private boolean isCharging;       // Whether the charging task is running
    private boolean lastPlayerNearby; // Store the last state of player proximity
    private double chargeTime;        // Total time for charging (in seconds)
    private double timeLeft;    // Time left for charging (in seconds)
    private Scoreboard scoreboard;
    private Objective objective;

    // Store the scoreboard reference for each player
    private final ScoreboardManager scoreboardManager;

    private int lastPercentCharged = -1;

    public ClassifiedOutpost(JavaPlugin plugin, OutpostDatabaseManager databaseManager, ConfigManager configManager, Location beaconLocation) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.beaconLocation = beaconLocation;
        this.isActive = false;
        this.isCharging = false;
        this.lastPlayerNearby = false;
        this.chargeTime = plugin.getConfig().getDouble("outpost.charge-time", 300);
        this.timeLeft = chargeTime;  // Initialize timeLeft with the total charge time

        plugin.getLogger().info("Charge Time set to: " + chargeTime + " seconds");

        // Register the class as a listener for block placement
        Bukkit.getPluginManager().registerEvents(this, plugin);
        scoreboardManager = Bukkit.getScoreboardManager();
    }

    @EventHandler
    public void onBeaconPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        // Check if the placed block is a beacon
        if (block.getType() == Material.BEACON) {
            Location beaconLocation = block.getLocation();
            Player player = event.getPlayer();

            // Debugging: Log the beacon placement event
            plugin.getLogger().info("[DEBUG] Beacon placed by " + player.getName() + " at location " + beaconLocation);

            // Get the item in the player's hand to check the beacon's name
            ItemStack itemInHand = event.getItemInHand();
            if (itemInHand != null && itemInHand.hasItemMeta()) {
                ItemMeta meta = itemInHand.getItemMeta();

                // Check if the beacon has a display name
                if (meta.hasDisplayName()) {
                    String outpostName = meta.getDisplayName();  // Use the beacon's display name as the outpost name

                    plugin.getLogger().info("[DEBUG] Beacon name: " + outpostName);

                    try {
                        // Save the beacon location and outpost name to the database
                        databaseManager.saveBeaconLocation(beaconLocation, outpostName);
                        player.sendMessage(ChatColor.GREEN + "Outpost created with name: " + outpostName);
                        plugin.getLogger().info("[Outpost] Beacon placed and outpost '" + outpostName + "' saved at " + beaconLocation);

                        // Save the outpost to the config file using the name and location
                        configManager.addOutpost(outpostName, beaconLocation);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "[Outpost] Error saving beacon location for '" + outpostName + "'", e);
                        player.sendMessage(ChatColor.RED + "An error occurred while saving the beacon location.");
                    }
                } else {
                    plugin.getLogger().info("[DEBUG] Beacon placed without a name.");
                    player.sendMessage(ChatColor.RED + "The beacon needs to have a name.");
                }
            } else {
                plugin.getLogger().info("[DEBUG] Invalid item or item meta during beacon placement.");
                player.sendMessage(ChatColor.RED + "Invalid item or item meta.");
            }
        }
    }

    @EventHandler
    public void onBeaconBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Check if the broken block is a beacon
        if (block.getType() == Material.BEACON) {
            Location beaconLocation = block.getLocation();
            Player player = event.getPlayer();

            // Debugging: Log the beacon break event
            plugin.getLogger().info("[DEBUG] Beacon broken by " + player.getName() + " at location " + beaconLocation);

            try {
                // Retrieve the outpost name from the database using the beacon's location
                String outpostName = databaseManager.getOutpostNameByLocation(beaconLocation);

                if (outpostName != null) {
                    // Debugging: Log the outpost name retrieved from the database
                    plugin.getLogger().info("[DEBUG] Outpost found for broken beacon: " + outpostName);

                    // Delete the beacon location from the database using the outpost name
                    databaseManager.deleteBeaconLocation(beaconLocation);
                    player.sendMessage(ChatColor.YELLOW + "Outpost with name: " + outpostName + " has been removed.");
                    plugin.getLogger().info("[Outpost] Outpost '" + outpostName + "' at " + beaconLocation + " has been removed.");

                    // Remove the outpost from the config manager
                    configManager.removeOutpost(outpostName);
                } else {
                    plugin.getLogger().info("[DEBUG] No outpost found for the broken beacon at " + beaconLocation);
                    player.sendMessage(ChatColor.RED + "No outpost found at this location.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[Outpost] Error deleting beacon location at " + beaconLocation, e);
                player.sendMessage(ChatColor.RED + "An error occurred while removing the outpost.");
            }
        }
    }

    // Start the outpost and add it to the activeClassifiedOutposts map
    public void startClassifiedObjective(Player player, String outpostName) {
        if (isActive) {
            player.sendMessage(ChatColor.RED + "The outpost is already active.");
            return;
        }

        // Fetch charge time dynamically from config each time
        this.chargeTime = plugin.getConfig().getDouble("charge-time", 300);
        this.timeLeft = chargeTime;  // Reset the charging time
        lastPercentCharged = -1;  // Reset progress
        lastUpdateTime = System.currentTimeMillis();  // Reset update time

        plugin.getLogger().info("[Outpost] Starting Classified Outpost: " + outpostName);

        // Add the outpost to the active map
        ((Main) plugin).getActiveClassifiedOutposts().put(outpostName.toLowerCase(), this);

        startProximityCheck();
        isActive = true;
        player.sendMessage(ChatColor.GREEN + "Charging started at the Classified Beacon.");

        setupScoreboard(player);
    }

    private void setupScoreboard(Player player) {
        // Check if the scoreboard is already initialized
        if (scoreboard == null) {
            scoreboard = scoreboardManager.getNewScoreboard();

            // Create the scoreboard objective
            objective = scoreboard.registerNewObjective("Charge", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.GREEN + "Charging (0%)"); // Display name shows charging progress

            // Set a single static score to ensure the scoreboard is displayed
            Score progressLabel = objective.getScore(ChatColor.YELLOW + "Placeholder");
            progressLabel.setScore(1);  // The actual value of the score doesn't matter
        }

        // Set the player's scoreboard once
        player.setScoreboard(scoreboard);
    }

    private void updateScoreboard(Player player, int roundedPercent) {
        // Update the display name with the new percentage
        objective.setDisplayName(ChatColor.GREEN + "Charging (" + roundedPercent + "%)");

        plugin.getLogger().info("[Outpost] Updated progress: " + roundedPercent + "%");
    }

    public boolean stop(String outpostName) {
        plugin.getLogger().info("[Outpost] stop() called. isCharging: " + isCharging + ", isActive: " + isActive);

        if (isCharging || isActive) {
            plugin.getLogger().info("[Outpost] Stopping outpost: " + outpostName);

            stopCharging();
            stopProximityCheck();
            isActive = false;
            isCharging = false;
            lastPercentCharged = -1;
            lastPlayerNearby = false;

            // Remove from the active map
            ((Main) plugin).getActiveClassifiedOutposts().remove(outpostName.toLowerCase());

            plugin.getLogger().info("[Outpost] Outpost " + outpostName + " stopped.");

            // Clear the scoreboard for all players in the same world as the beacon
            for (Player player : beaconLocation.getWorld().getPlayers()) {
                clearScoreboard(player);
            }

            return true;
        } else {
            plugin.getLogger().info("[Outpost] Attempted to stop outpost " + outpostName + ", but it was not active.");
            return false;
        }
    }

    private void startProximityCheck() {
        if (checkingTask == null) {
            lastUpdateTime = System.currentTimeMillis();
            checkingTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0L, 1L);  // Schedule every tick
            plugin.getLogger().info("[Outpost] Proximity check started.");
        }
    }

    private void stopProximityCheck() {
        if (checkingTask != null) {
            checkingTask.cancel();
            checkingTask = null;
            plugin.getLogger().info("[Outpost] Proximity check stopped.");
        }
    }

    private void startCharging() {
        if (!isCharging) {
            isCharging = true;
            lastUpdateTime = System.currentTimeMillis();  // Initialize lastUpdateTime when charging starts
            plugin.getLogger().info("[Outpost] Charging process started at the outpost.");
        }
    }

    private void stopCharging() {
        isCharging = false;
        plugin.getLogger().info("[Outpost] Charging process stopped.");
    }

    // Update the progress percentage calculation in your run() method
    private long lastUpdateTime = System.currentTimeMillis();  // Track system time

    @Override
    public void run() {
        double proximityRadius = plugin.getConfig().getDouble("proximity-radius", 10.0);
        boolean playerNearby = false;

        // Check if any player is within proximity of the beacon
        for (Player player : beaconLocation.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(beaconLocation);
            if (distance <= proximityRadius) {
                playerNearby = true;
                break;  // Stop checking once we find a nearby player
            }
        }

        if (playerNearby) {
            if (!lastPlayerNearby) {
                plugin.getLogger().info("[Outpost] Player is within range. Starting charging.");
            }
            lastPlayerNearby = true;

            if (!isCharging) {
                startCharging();
            }

            // Use system time to calculate accurate time progression
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUpdateTime;

            // Reduce timeLeft based on elapsed time
            double elapsedSeconds = elapsedTime / 1000.0;
            timeLeft = Math.max(0, timeLeft - elapsedSeconds);

            // Calculate the charging percentage
            double percentCharged = ((chargeTime - timeLeft) / chargeTime) * 100;
            int roundedPercent = (int) Math.floor(percentCharged);

            // Only update if the percentage has changed
            if (roundedPercent != lastPercentCharged) {
                plugin.getLogger().info("[Outpost] Time Left: " + timeLeft);
                plugin.getLogger().info("[Outpost] Percent Charged: " + roundedPercent + "%");

                // Update the scoreboard for all nearby players
                for (Player player : beaconLocation.getWorld().getPlayers()) {
                    updateScoreboard(player, roundedPercent);
                    plugin.getLogger().info("[Outpost] Updating scoreboard for player: " + player.getName());
                }

                // Update lastPercentCharged to the new percentage
                lastPercentCharged = roundedPercent;
            }

            if (timeLeft <= 0) {
                plugin.getLogger().info("[Outpost] Charging complete. Calling stop() to end outpost.");
                boolean stopResult = stop(objective.getName());  // Capture the result of stop()

                if (stopResult) {
                    plugin.getLogger().info("[Outpost] Outpost successfully stopped after charging completion.");

                    // Notify all players and clear the scoreboard
                    for (Player player : beaconLocation.getWorld().getPlayers()) {
                        clearScoreboard(player);
                        player.sendMessage(ChatColor.GREEN + "The Classified Beacon is fully charged!");
                    }
                } else {
                    plugin.getLogger().warning("[Outpost] Failed to stop outpost after charging completion.");
                }
            }

            // Update lastUpdateTime to the current time
            lastUpdateTime = currentTime;

        } else {
            // If no player is nearby, stop charging
            if (lastPlayerNearby) {
                plugin.getLogger().info("[Outpost] No players nearby. Stopping charging.");
                stopCharging();
            }
            lastPlayerNearby = false;
        }
    }

    private void clearScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);  // Clear only the sidebar slot
    }
}