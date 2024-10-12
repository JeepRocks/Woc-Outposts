package com.jeepy.wocoutposts;

import com.jeepy.database.WocTeamsDatabaseManager;
import com.jeepy.wocoutposts.database.OutpostDatabaseManager;
import com.jeepy.teams.Team;  // Import the Team class from Woc-Teams
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DataTransferManager {

    private final WocTeamsDatabaseManager wocTeamsDatabaseManager;  // Use the WocTeamsDatabaseManager
    private final OutpostDatabaseManager wocOutpostsDatabaseManager;
    private final Main plugin;  // Add reference to the Main class for logging

    // Constructor to initialize both database managers and plugin reference
    public DataTransferManager(WocTeamsDatabaseManager wocTeamsDatabaseManager, OutpostDatabaseManager wocOutpostsDatabaseManager, Main plugin) {
        this.wocTeamsDatabaseManager = wocTeamsDatabaseManager;
        this.wocOutpostsDatabaseManager = wocOutpostsDatabaseManager;
        this.plugin = plugin;  // Assign the plugin for logging purposes
    }

    // Transfer team data from Woc-Teams to Woc-Outposts
    public void transferTeamsData() throws SQLException {
        try {
            // Fetch all teams from Woc-Teams
            List<Team> teams = wocTeamsDatabaseManager.getAllTeams();
            for (Team team : teams) {
                Integer teamId = team.getId();
                String teamName = team.getName();
                UUID ownerUUID = team.getOwner();

                // Save the team into Woc-Outposts database
                wocOutpostsDatabaseManager.saveTeamToOutpostsDb(teamId, teamName, ownerUUID);
            }
        } catch (SQLException e) {
            throw new SQLException("Could not transfer teams data from Woc-Teams to Woc-Outposts", e);
        }
    }

    /**
     * Removes a disbanded team from the Woc-Outposts database if it no longer exists in Woc-Teams.
     *
     * @param teamId the ID of the team to remove.
     * @throws SQLException if there is an issue accessing the databases.
     */
    public void removeDisbandedTeam(Integer teamId) throws SQLException {
        // Fetch all teams from Woc-Teams
        List<Team> teams = wocTeamsDatabaseManager.getAllTeams();  // Correctly use the teams DB manager

        // Check if the team still exists
        boolean teamExists = teams.stream().anyMatch(team -> team.getId().equals(teamId));

        // If the team doesn't exist in Woc-Teams, remove it from the Woc-Outposts database
        if (!teamExists) {
            wocOutpostsDatabaseManager.deleteTeamFromOutpostsDb(teamId);
            plugin.getLogger().info("Team " + teamId + " removed from Woc-Outposts as it no longer exists in Woc-Teams.");
        }
    }

    // ---- Chest-related Methods ----

    public List<Location> loadChestLocations() throws SQLException {
        return wocOutpostsDatabaseManager.loadChestLocations();
    }

    public void saveChestLocation(Location location) throws SQLException {
        wocOutpostsDatabaseManager.saveChestLocation(location);
    }

    public void deleteChestLocation(Location location) throws SQLException {
        wocOutpostsDatabaseManager.deleteChestLocation(location);
    }

    // ---- Beacon-related Methods ----

    public Location loadLastBeaconLocation() throws SQLException {
        return wocOutpostsDatabaseManager.loadLastBeaconLocation();
    }

    public void saveBeaconLocation(Location location, String name) throws SQLException {
        wocOutpostsDatabaseManager.saveBeaconLocation(location, name);
    }

    public void deleteBeaconLocation(Location location) throws SQLException {
        wocOutpostsDatabaseManager.deleteBeaconLocation(location);
    }
}
