package com.jeepy.wocoutposts.objectives;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.database.OutpostDatabaseManager;
import com.jeepy.database.WocTeamsDatabaseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.jeepy.teams.Team;
import org.bukkit.event.entity.PlayerDeathEvent;

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

    private double currentCharge = 0.0; // Track outpost charge (0 to 100%)
    private Integer controllingTeamId = null; // The team currently controlling the outpost
    private long contestStartTime = -1; // Timer for contested state
    private boolean inOvertime = false; // Whether the outpost is in overtime
    private long overtimeStartTime = -1; // Start time for overtime
    private long overtimeRemaining = 0; // Overtime remaining time in milliseconds
    private int overtimeResetCount = 0; // How many times overtime has been reset
    private static final int OVERTIME_RESET_LIMIT = 4; // Number of resets before reducing overtime

    // Constructor accepting plugin reference for loading configuration
    public ClassifiedOutpost(String outpostName, Location beaconLocation, Main plugin) {
        super(outpostName, beaconLocation);
        this.beaconLocation = beaconLocation;
        this.plugin = plugin;
        this.teamsDbManager = plugin.getTeamsDatabaseManager();
        this.outpostDbManager = plugin.getDatabaseManager();
        loadConfig();
    }

    // Method to load configuration settings
    private void loadConfig() {
        captureRadius = plugin.getConfig().getInt("classified_outpost.capture_radius", 15);
        chargeReductionRate = plugin.getConfig().getDouble("classified_outpost.charge_reduction_rate", 1.0);
        overtimeDuration = plugin.getConfig().getInt("classified_outpost.overtime.duration", 5);
        chargeThresholds = plugin.getConfig().getIntegerList("classified_outpost.charge_thresholds");
        teamBoost = plugin.getConfig().getDouble("classified_outpost.team_boost", 0.5);
        soloPlayerBoost = plugin.getConfig().getDouble("classified_outpost.solo_player_boost", 1.0);
    }

    // Method to handle player kills and assign boosts
    public void onPlayerKill(PlayerDeathEvent event) throws SQLException {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            UUID killerUUID = killer.getUniqueId();
            Integer teamId = teamsDbManager.getTeamIdForPlayer(killerUUID);

            if (teamId != null) {
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
                handlePlayerEntry(player, playerUUID);

            } else {
                // Player/team is outside the capture radius
                handlePlayerExit(player, playerUUID);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating player/team in radius: " + e.getMessage());
        }
    }

    private void handlePlayerEntry(Player player, UUID playerUUID) throws SQLException {
        if (!playersInRadius.contains(playerUUID)) {
            // New player enters the radius
            Integer teamId = teamsDbManager.getTeamIdForPlayer(playerUUID);  // Check if the player belongs to a team

            if (teamId != null && !teamsInRadius.contains(teamId)) {
                // If player is part of a team and team not already inside the radius
                Team team = teamsDbManager.getTeamById(teamId);  // Fetch team by teamId
                if (team != null) {
                    // Save the team and mark it as inside the radius
                    handleTeamEntry(player, team, teamId);
                }
            } else if (teamId == null) {
                // Player doesn't belong to a team, add them as a solo player
                handleSoloPlayerEntry(player, playerUUID);
            }
        }
    }

    private void handleTeamEntry(Player player, Team team, int teamId) throws SQLException {
        UUID ownerUUID = team.getOwner();
        outpostDbManager.saveTeamToOutpostsDb(teamId, team.getName(), ownerUUID);  // Save team to database
        teamsInRadius.add(teamId);  // Track the team in radius
        player.sendMessage("Your team has entered the outpost radius.");
    }

    private void handleSoloPlayerEntry(Player player, UUID playerUUID) throws SQLException {
        outpostDbManager.savePlayerToOutpostsDb(playerUUID, player.getName());
        playersInRadius.add(playerUUID);  // Track the player in radius
        player.sendMessage("You have entered the outpost radius as a solo player.");
    }

    private void handlePlayerExit(Player player, UUID playerUUID) throws SQLException {
        // Check if the player is already tracked inside the radius
        if (playersInRadius.contains(playerUUID)) {
            // Handle solo player exit
            handleSoloPlayerExit(player, playerUUID);
        }

        Integer teamId = teamsDbManager.getTeamIdForPlayer(playerUUID);
        if (teamId != null && teamsInRadius.contains(teamId)) {
            // Handle team exit
            handleTeamExit(player, teamId);
        }
    }

    private void handleSoloPlayerExit(Player player, UUID playerUUID) throws SQLException {
        // Remove solo player from the database and internal tracking
        outpostDbManager.removePlayerFromOutpostsDb(playerUUID);
        playersInRadius.remove(playerUUID);
        player.sendMessage("You have exited the outpost radius.");
    }

    private void handleTeamExit(Player player, int teamId) throws SQLException {
        // Remove team from the database and internal tracking
        outpostDbManager.removeTeamFromOutpostsDb(teamId);
        teamsInRadius.remove(teamId);
        player.sendMessage("Your team has exited the outpost radius.");
    }

    @Override
    public void startCharging() {
        // Update the player's presence in the radius
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerInRadius(player);
        }

        // If outpost is in overtime, handle that separately
        if (inOvertime) {
            enterOvertime();
            return;
        }

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

                currentCharge = Math.min(currentCharge + 1.0 + boost, 100.0);  // Cap at 100%
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
                currentCharge = Math.min(currentCharge + 1.0 + boost, 100.0);  // Cap at 100%
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

    private void handleContest() {
        if (contestStartTime == -1) {
            contestStartTime = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - contestStartTime;
        if (elapsed >= 120_000L) { // 2 minutes contesting
            // Reduce charge by 1% every 2 minutes
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
        plugin.getLogger().info("Charging paused. Current charge: " + currentCharge + "%");
        contestStartTime = -1; // Reset contest timer
    }

    public void updateCharging() {
        if (!inOvertime) {
            startCharging();
        } else {
            enterOvertime();
        }
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
