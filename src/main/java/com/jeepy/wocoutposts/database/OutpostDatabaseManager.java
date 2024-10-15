package com.jeepy.wocoutposts.database;

import com.jeepy.wocoutposts.Main;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class OutpostDatabaseManager {
    private final Main plugin;
    private Connection connection;
    private static final String DEFAULT_RANK = "MEMBER";

    public OutpostDatabaseManager(Main plugin, String teamsDbPath) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/database.db");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Unable to create a database connection: " + e.getMessage());
            throw new SQLException("Unable to create a database connection.", e);
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void initialize() throws SQLException {
        connect();
        try {
            // Create teams table
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS teams (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "teamName TEXT NOT NULL," +
                            "ownerUUID TEXT NOT NULL" +
                            ")")) {
                statement.executeUpdate();
                plugin.getLogger().info("Teams table in Outposts DB initialized successfully.");
            }

            // Create players table (for non-team players)
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "uuid TEXT PRIMARY KEY, " +
                            "playerName TEXT NOT NULL" +
                            ")")) {
                statement.executeUpdate();
                plugin.getLogger().info("Players table in Outposts DB initialized successfully.");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error initializing database: " + e.getMessage());
            throw e;
        }
    }

    // ---- Team-related methods ----

    // Method to check if a team is already in the database
    private boolean isTeamInDatabase(Integer teamId) throws SQLException {
        String query = "SELECT id FROM teams WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, teamId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();  // Returns true if a result is found
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not check if team exists in database", e);
            throw new SQLException("Could not check if team exists in database", e);
        }
    }

    // Save team to database only if not already present
    public synchronized void saveTeamToOutpostsDb(Integer teamId, String teamName, UUID ownerUUID) throws SQLException {
        plugin.getLogger().info("Saving team to Outposts DB: ID=" + teamId + ", Name=" + teamName + ", OwnerUUID=" + ownerUUID);

        if (teamId == null || teamName == null || ownerUUID == null) {
            throw new SQLException("Team ID, Team Name, or Owner UUID cannot be null");
        }

        // Check if the team is already in the database
        if (isTeamInDatabase(teamId)) {
            plugin.getLogger().info("Team " + teamName + " is already in the database.");
            return;  // Team already exists, no need to add
        }

        // Add the team to the database
        String sql = "INSERT INTO teams (id, teamName, ownerUUID) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            pstmt.setString(2, teamName);
            pstmt.setString(3, ownerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save team to database", e);
            throw new SQLException("Could not save team to database", e);
        }
    }

    // Remove a team from the database
    public synchronized void removeTeamFromOutpostsDb(Integer teamId) throws SQLException {
        plugin.getLogger().info("Removing team from Outposts DB: ID=" + teamId);

        String sql = "DELETE FROM teams WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove team from database", e);
            throw new SQLException("Could not remove team from database", e);
        }
    }

    // ---- Player-related methods ----

    // Method to check if a player is already in the database
    private boolean isPlayerInDatabase(UUID playerUUID) throws SQLException {
        plugin.getLogger().info("Checking if player " + playerUUID + " exists in the database.");

        String query = "SELECT uuid FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean exists = rs.next();
                plugin.getLogger().info("Player " + playerUUID + " exists in the database: " + exists);
                return exists;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not check if player exists in database", e);
            throw new SQLException("Could not check if player exists in database", e);
        }
    }

    // Save player to database if not already present
    public synchronized void savePlayerToOutpostsDb(UUID playerUUID, String playerName) throws SQLException {
        plugin.getLogger().info("Saving player to Outposts DB: UUID=" + playerUUID + ", Name=" + playerName);

        if (playerUUID == null || playerName == null) {
            throw new SQLException("Player UUID or Name cannot be null");
        }

// Check if the player is already in the database
        if (isPlayerInDatabase(playerUUID)) {
            plugin.getLogger().info("Player " + playerName + " is already in the database.");
            return;  // Player already exists, no need to add
        }

// Add the player to the database
        String sql = "INSERT INTO players (uuid, playerName) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerName);
            pstmt.executeUpdate();
            plugin.getLogger().info("Player " + playerName + " successfully saved to the database.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player to database", e);
            throw new SQLException("Could not save player to database", e);
        }}

    // Remove a player from the database
    public synchronized void removePlayerFromOutpostsDb(UUID playerUUID) throws SQLException {
        plugin.getLogger().info("Removing player from Outposts DB: UUID=" + playerUUID);

        String sql = "DELETE FROM players WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
            plugin.getLogger().info("Player " + playerUUID + " successfully removed from the database.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove player from database", e);
            throw new SQLException("Could not remove player from database", e);
        }
    }

}