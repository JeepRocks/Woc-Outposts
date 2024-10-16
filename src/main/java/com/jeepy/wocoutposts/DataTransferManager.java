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
            plugin.getLogger().info("Number of teams fetched from Woc-Teams: " + teams.size());
            for (Team team : teams) {
                plugin.getLogger().info("Team fetched: ID=" + team.getId() + ", Name=" + team.getName());
            }

            for (Team team : teams) {
                Integer teamId = team.getId();
                String teamName = team.getName();
                UUID ownerUUID = team.getOwner();

                plugin.getLogger().info("Transferring team: ID=" + teamId + ", Name=" + teamName + ", Owner=" + ownerUUID);

                // Save the team into Woc-Outposts database
                wocOutpostsDatabaseManager.saveTeamToOutpostsDb(teamId, teamName, ownerUUID);
                plugin.getLogger().info("Team transferred successfully to Woc-Outposts DB: " + teamName);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not transfer teams data from Woc-Teams to Woc-Outposts", e);
            throw new SQLException("Could not transfer teams data", e);
        }
    }

    // ---- Chest-related Methods ----


    // ---- Beacon-related Methods ----
}
