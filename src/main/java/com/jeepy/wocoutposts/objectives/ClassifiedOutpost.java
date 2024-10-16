package com.jeepy.wocoutposts.objectives;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.database.OutpostDatabaseManager;
import com.jeepy.database.WocTeamsDatabaseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.jeepy.teams.Team;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;

public class ClassifiedOutpost extends Outpost {

    private final Main plugin;
    private final WocTeamsDatabaseManager teamsDbManager;
    private final OutpostDatabaseManager outpostDbManager;
    private final Location beaconLocation;

    // Track players and teams currently inside the radius
    private final Set<UUID> playersInRadius = new HashSet<>();
    private final Set<Integer> teamsInRadius = new HashSet<>();
    private final Map<UUID, Integer> playerKillCount = new HashMap<>();
    private final Map<Integer, Double> teamKillCount = new HashMap<>();

    // Configurable properties specific to ClassifiedOutpost
    private int captureRadius;
    private double chargeReductionRate;
    private int overtimeDuration;
    private List<Integer> chargeThresholds; // List of charge thresholds
    private double soloPlayerBoost;
    private double teamBoost;
    private BukkitRunnable chargingTask;

    private double currentCharge = 0.0; // Track outpost charge (0 to 100%)
    private Integer controllingTeamId = null; // The team currently controlling the outpost
    private long contestStartTime = -1; // Timer for contested state
    private boolean inOvertime = false; // Whether the outpost is in overtime
    private long overtimeStartTime = -1; // Start time for overtime
    private long overtimeRemaining = 0; // Overtime remaining time in milliseconds
    private int overtimeResetCount = 0; // How many times overtime has been reset
    private static final int OVERTIME_RESET_LIMIT = 4; // Number of resets before reducing overtime
    private boolean chargingEnabled = false;
    private boolean chargingLogDisplayed = false;

    private double chargePerSecond; // How much charge to add per second
    private long lastChargeTime;

    // Constructor accepting plugin reference for loading configuration
    public ClassifiedOutpost(String outpostName, Location beaconLocation, Main plugin) {
        super(outpostName, beaconLocation);
        this.beaconLocation = beaconLocation;
        this.plugin = plugin;
        this.teamsDbManager = plugin.getTeamsDatabaseManager();
        this.outpostDbManager = plugin.getDatabaseManager();
        loadConfig();
        this.lastChargeTime = System.currentTimeMillis();
    }

    // Method to load configuration settings
    private void loadConfig() {
        captureRadius = plugin.getConfig().getInt("classified_outpost.capture_radius", 15);
        chargeReductionRate = plugin.getConfig().getDouble("classified_outpost.charge_reduction_rate", 1.0);
        overtimeDuration = plugin.getConfig().getInt("classified_outpost.overtime.duration", 5);
        chargeThresholds = plugin.getConfig().getIntegerList("classified_outpost.charge_thresholds");
        teamBoost = plugin.getConfig().getDouble("classified_outpost.team_boost", 0.5);
        soloPlayerBoost = plugin.getConfig().getDouble("classified_outpost.solo_player_boost", 1.0);

        int chargeTimeSeconds = plugin.getConfig().getInt("classified_outpost.charge_time_seconds", 300);
        chargePerSecond = 1.0 / chargeTimeSeconds;
    }

    public void stopOutpost() {
        if (chargingTask != null) {
            chargingTask.cancel();  // Cancel any ongoing charging task
            chargingTask = null;
        }
        chargingEnabled = false;  // Set charging to disabled
        plugin.getLogger().info("Charging for the outpost has been stopped.");

        // Clear out any other state, like players or teams in the radius
        playersInRadius.clear();
        teamsInRadius.clear();

        plugin.getLogger().info("All events for this outpost have been cancelled.");
    }


    // Method to handle player kills and assign boosts
    public void onPlayerKill(PlayerDeathEvent event) throws SQLException {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            UUID killerUUID = killer.getUniqueId();

            // Use getTeamByPlayer to get the Team object
            Team team = teamsDbManager.getTeamByPlayer(killer);

            if (team != null) {
                int teamId = team.getId();  // Get the team ID
                int teamSize = teamsDbManager.getTeamMembersById(teamId).size();
                int smallestTeamSize = getSmallestTeamSize();

                if (teamSize <= smallestTeamSize) {
                    teamKillCount.put(teamId, teamKillCount.getOrDefault(teamId, 0.0) + 1.0);
                    killer.sendMessage("Your team's capture speed has increased due to your kill!");
                } else {
                    teamKillCount.put(teamId, teamKillCount.getOrDefault(teamId, 0.0) + 0.5);
                    killer.sendMessage("Your team's capture speed has slightly increased.");
                }

            } else {
                // Solo player boost
                playerKillCount.put(killerUUID, playerKillCount.getOrDefault(killerUUID, 0) + 1);
                killer.sendMessage("Your solo capture speed has increased!");
            }
        }
    }

    private int getSmallestTeamSize() throws SQLException {
        int smallestSize = Integer.MAX_VALUE;

        for (Integer teamId : teamsInRadius) {
            int teamSize = teamsDbManager.getTeamMembersById(teamId).size();
            if (teamSize < smallestSize) {
                smallestSize = teamSize;
            }
        }
        return smallestSize == Integer.MAX_VALUE ? 0 : smallestSize;
    }

    // Check if a player is inside the outpost radius and handle adding/removing teams or players to/from the database
    public void updatePlayerInRadius(Player player) {
        Location playerLocation = player.getLocation();
        double distance = playerLocation.distance(beaconLocation);  // Get the distance from player to beacon
        UUID playerUUID = player.getUniqueId();

        try {
            if (distance <= captureRadius) {
                // Player/team is inside the capture radius
                if (!playersInRadius.contains(playerUUID)) {
                    // New player enters the radius
                    Team team = teamsDbManager.getTeamByPlayer(player);  // Fetch the player's team (null if solo player)

                    if (team != null && !teamsInRadius.contains(team.getId())) {
                        // Handle team players
                        UUID ownerUUID = team.getOwner();
                        outpostDbManager.saveTeamToOutpostsDb(team.getId(), team.getName(), ownerUUID);  // Save team to database
                        teamsInRadius.add(team.getId());  // Track the team in radius
                        player.sendMessage("Your team has entered the outpost radius.");
                    } else if (team == null) {
                        // Handle solo player (team is null)
                        outpostDbManager.savePlayerToOutpostsDb(playerUUID, player.getName());
                        playersInRadius.add(playerUUID);  // Track the player in radius
                        player.sendMessage("You have entered the outpost radius as a solo player.");
                    }
                }
            } else {
                // Player/team is outside the capture radius, remove them from the database
                if (playersInRadius.contains(playerUUID)) {
                    outpostDbManager.removePlayerFromOutpostsDb(playerUUID);
                    playersInRadius.remove(playerUUID);
                    player.sendMessage("You have exited the outpost radius.");
                }

                Team team = teamsDbManager.getTeamByPlayer(player);  // Fetch the player's team
                if (team != null && teamsInRadius.contains(team.getId())) {
                    // Remove team from the database
                    outpostDbManager.removeTeamFromOutpostsDb(team.getId());
                    teamsInRadius.remove(team.getId());
                    player.sendMessage("Your team has exited the outpost radius.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating player/team in radius: " + e.getMessage());
        }
    }

    @Override
    public void startCharging() {
        if (!chargingEnabled) {
            plugin.getLogger().info("Charging is not enabled for this outpost.");
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerInRadius(player);  // Update the player's presence in the radius
        }

        // If outpost is in overtime, handle that separately
        if (inOvertime) {
            enterOvertime();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = (currentTime - lastChargeTime) / 1000;  // Convert to seconds

        // Ensure enough time has passed since the last charge increment
        if (elapsedTime < 1) {
            return;  // Only charge every second
        }

        lastChargeTime = currentTime;

        // Calculate how much charge to add based on the charge per second (configured in charge_time_seconds)
        double chargeIncrement = chargePerSecond * elapsedTime;

        // Only one team or player in the radius
        if (teamsInRadius.size() == 1) {
            Integer teamId = teamsInRadius.iterator().next();

            if (controllingTeamId == null) {
                // First team to capture the outpost
                controllingTeamId = teamId;
                plugin.getLogger().info("Team " + teamId + " has started capturing the outpost.");
            }

            if (controllingTeamId.equals(teamId)) {
                // The controlling team is charging
                double boost = 0.0;

                // Apply team boost based on kills if it's a team capture
                if (teamId != null) {
                    boost = teamKillCount.getOrDefault(teamId, 0.0) * teamBoost;
                }

                // Increment charge based on chargePerSecond and any boosts
                currentCharge = Math.min(currentCharge + chargeIncrement + boost, 100.0);  // Cap at 100%
                plugin.getLogger().info("Outpost charge: " + currentCharge + "% with boost: " + boost + "%");

                // If the outpost reaches 100% charge, enter overtime
                if (currentCharge >= 100) {
                    enterOvertime();
                }
            } else {
                // New team is contesting, stop the previous team's progress
                plugin.getLogger().info("Outpost is being contested by another team.");
                pauseCharging();
            }

            // Solo player case: No teams but at least one player in radius
        } else if (teamsInRadius.isEmpty() && playersInRadius.size() == 1) {
            UUID playerUUID = playersInRadius.iterator().next();
            if (controllingTeamId == null) {
                // Solo player captures the outpost first
                controllingTeamId = null; // No team, it's a solo player capture
                plugin.getLogger().info("Player " + playerUUID + " has started capturing the outpost as a solo player.");
            }

            if (controllingTeamId == null) {
                // Solo player charging
                double boost = playerKillCount.getOrDefault(playerUUID, 0) * soloPlayerBoost;
                currentCharge = Math.min(currentCharge + chargeIncrement + boost, 100.0);  // Cap at 100%
                plugin.getLogger().info("Solo player outpost charge: " + currentCharge + "% with boost: " + boost + "%");

                if (currentCharge >= 100) {
                    enterOvertime();
                }
            }

        } else if (teamsInRadius.size() > 1) {
            // Multiple teams are contesting the outpost
            handleContest();

        } else {
            // No teams in radius, stop charging
            pauseCharging();
        }
    }

    public void setChargingEnabled(boolean enabled) {
        this.chargingEnabled = enabled; // Set the charging status
    }

    private void handleContest() {
        if (contestStartTime == -1) {
            contestStartTime = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - contestStartTime;
        if (elapsed >= 60_000L) { // 1 minute contesting
            // Reduce charge by 1% every 1 minutes
            currentCharge = Math.max(currentCharge - chargeReductionRate, 0);
            applyChargeBalancing();  // Ensure charge doesn't drop below thresholds
            plugin.getLogger().info("Outpost charge reduced to " + currentCharge + "% due to contesting.");
            contestStartTime = System.currentTimeMillis(); // Reset contest timer
        }

        pauseCharging();
    }


    private void applyChargeBalancing() {
        // Ensure the charge does not drop below the nearest threshold
        for (int threshold : chargeThresholds) {
            if (currentCharge >= threshold) {
                // This is the highest threshold we have passed, set as the minimum
                currentCharge = Math.max(currentCharge, threshold);
                break;
            }
        }
    }

    @Override
    public void stopCharging() {
        plugin.getLogger().info("Stopped charging for Classified Outpost.");
    }

    @Override
    public void refillLoot() {
        plugin.getLogger().info("Refilling loot for " + this.getOutpostName());
    }

    private void pauseCharging() {
        contestStartTime = -1; // Reset contest timer
    }

    public void updateCharging() {
        if (!chargingEnabled) {
            if (!chargingLogDisplayed) {
                plugin.getLogger().info("Charging is not enabled for this outpost.");
                chargingLogDisplayed = true; // Only log this message once.
            }
            return; // Exit if charging is disabled.
        }
        chargingLogDisplayed = false; // Reset the log flag when charging is enabled again.
        startCharging(); // Call the charging process only if enabled.
    }

    public void resetCharge() {
        this.currentCharge = 0.0;
        this.lastChargeTime = System.currentTimeMillis();
    }

    private void enterOvertime() {
        if (!inOvertime) {
            inOvertime = true;
            overtimeStartTime = System.currentTimeMillis();
            overtimeRemaining = overtimeDuration * 1000L; // Start with full overtime duration
            plugin.getLogger().info("Overtime has started for the outpost.");
        }

        long elapsed = System.currentTimeMillis() - overtimeStartTime;

        if (elapsed >= overtimeRemaining) {
            // Overtime duration has elapsed, check if a team has won
            if (teamsInRadius.size() == 1) {
                Integer teamId = teamsInRadius.iterator().next();

                // Check if it's the controlling team
                if (teamId.equals(controllingTeamId)) {
                    plugin.getLogger().info("Team " + controllingTeamId + " has successfully captured the outpost during overtime!");
                    endOvertime(teamId);  // End the overtime and declare the winner
                } else {
                    // A new team has contested the outpost
                    plugin.getLogger().info("Team " + teamId + " has contested the outpost.");
                    resetOvertime(teamId);  // Reset the overtime timer for the new team
                }
            } else if (teamsInRadius.isEmpty()) {
                // No teams are contesting the point, end overtime with the controlling team winning
                plugin.getLogger().info("No teams contested the outpost. Team " + controllingTeamId + " has won!");
                endOvertime(controllingTeamId);
            }
        } else {
            // Overtime is still running
            plugin.getLogger().info("Overtime ongoing. Remaining time: " + (overtimeRemaining - elapsed) / 1000.0 + " seconds.");
        }
    }

    // Helper method to reset overtime with a new team or reset the timer with decreasing time
    private void resetOvertime(Integer newControllingTeamId) {
        if (newControllingTeamId != null) {
            controllingTeamId = newControllingTeamId; // Change controlling team
        }

        if (overtimeResetCount < OVERTIME_RESET_LIMIT) {
            overtimeRemaining = overtimeDuration * 1000L;  // Reset to full duration (e.g., 5 seconds)
            overtimeResetCount++;
            plugin.getLogger().info("Overtime reset to " + overtimeRemaining / 1000.0 + " seconds. Reset count: " + overtimeResetCount);
        } else {
            // After 4 resets, start reducing the overtime timer
            overtimeDuration = Math.max(overtimeDuration - 1, 1);  // Decrease overtime by 1 second but not below 1 second
            overtimeRemaining = overtimeDuration * 1000L;  // Update overtime timer
            plugin.getLogger().info("Overtime duration reduced to " + overtimeDuration + " seconds.");
        }
        overtimeStartTime = System.currentTimeMillis();  // Reset the timer start point
    }

    // Helper method to end overtime and declare a winner
    private void endOvertime(Integer winningTeamId) {
        inOvertime = false;
        overtimeStartTime = -1;
        overtimeRemaining = 0;
        overtimeResetCount = 0;  // Reset overtime reset counter

        if (winningTeamId != null) {
            plugin.getLogger().info("Team " + winningTeamId + " has officially won the outpost!");
            // Implement logic to reward the winning team (points, resources, etc.)
        } else {
            plugin.getLogger().info("Overtime ended with no winner.");
        }
    }
}
