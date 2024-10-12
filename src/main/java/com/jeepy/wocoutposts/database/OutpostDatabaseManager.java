package com.jeepy.wocoutposts.database;

import com.jeepy.database.WocTeamsDatabaseManager;
import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.objectives.Outpost;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class OutpostDatabaseManager {
    private final Main plugin;  // No need to be static
    private Connection connection;  // No need to be static
    private static final String DEFAULT_RANK = "MEMBER";

    public OutpostDatabaseManager(Main plugin, String teamsDbPath) {
        this.plugin = plugin;  // Assign plugin reference to each instance
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
            // Ensure that teams table has the correct columns
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS teams (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "teamName TEXT NOT NULL," +
                            "ownerUUID TEXT NOT NULL" +
                            ")")) {
                statement.executeUpdate();
                plugin.getLogger().info("Teams table in Outposts DB initialized successfully.");
            }





            // Create chests table
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS chests (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "world TEXT NOT NULL, " +
                            "x DOUBLE NOT NULL, " +
                            "y DOUBLE NOT NULL, " +
                            "z DOUBLE NOT NULL" +
                            ")")) {
                statement.executeUpdate();
            }

            // Create beacons table
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS beacons (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "world TEXT NOT NULL, " +
                            "x DOUBLE NOT NULL, " +
                            "y DOUBLE NOT NULL, " +
                            "z DOUBLE NOT NULL, " +
                            "outpostName TEXT NOT NULL" +
                            ")")) {
                statement.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error initializing database: " + e.getMessage());
            throw e;
        }
    }

    // ---- Chest-related Methods ----

    // Save chest location
    public void saveChestLocation(Location location) throws SQLException {
        String query = "INSERT INTO chests (world, x, y, z) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, location.getWorld().getName());
            statement.setDouble(2, location.getX());
            statement.setDouble(3, location.getY());
            statement.setDouble(4, location.getZ());
            statement.executeUpdate();
        }
    }

    // Load chest locations
    public List<Location> loadChestLocations() throws SQLException {
        List<Location> locations = new ArrayList<>();
        String query = "SELECT world, x, y, z FROM chests";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                World world = plugin.getServer().getWorld(rs.getString("world"));
                if (world != null) {
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    locations.add(new Location(world, x, y, z));
                }
            }
        }
        return locations;
    }

    // Delete chest location
    public void deleteChestLocation(Location location) throws SQLException {
        String query = "DELETE FROM chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, location.getWorld().getName());
            statement.setDouble(2, location.getX());
            statement.setDouble(3, location.getY());
            statement.setDouble(4, location.getZ());
            statement.executeUpdate();
        }
    }

    // ---- Beacon-related Methods ----

    // Save beacon location
    public void saveBeaconLocation(Location location, String outpostName) throws SQLException {
        String query = "INSERT INTO beacons (world, x, y, z, outpostName) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, location.getWorld().getName());
            statement.setDouble(2, location.getX());
            statement.setDouble(3, location.getY());
            statement.setDouble(4, location.getZ());
            statement.setString(5, outpostName);
            statement.executeUpdate();
        }
    }

    // Load the last beacon location
    public Location loadLastBeaconLocation() throws SQLException {
        String query = "SELECT world, x, y, z FROM beacons ORDER BY id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                World world = plugin.getServer().getWorld(rs.getString("world"));
                if (world != null) {
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    return new Location(world, x, y, z);
                }
            }
        }
        return null;
    }

    // Delete beacon location
    public void deleteBeaconLocation(Location location) throws SQLException {
        String query = "DELETE FROM beacons WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, location.getWorld().getName());
            statement.setDouble(2, location.getX());
            statement.setDouble(3, location.getY());
            statement.setDouble(4, location.getZ());
            statement.executeUpdate();
        }
    }

    // ---- Team-related methods ----

    public synchronized void deleteTeamFromOutpostsDb(Integer teamId) throws SQLException {
        String sql = "DELETE FROM teams WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not delete team from database", e);
            throw new SQLException("Could not delete team from database", e);
        }
    }

    public synchronized List<com.jeepy.teams.Team> fetchAllTeamsFromTeamsDb(WocTeamsDatabaseManager teamsDatabaseManager) throws SQLException {
        List<com.jeepy.teams.Team> teams = new ArrayList<>();
        String sql = "SELECT id, teamName, ownerUUID FROM teams";  // Fetch the UUID as well

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Integer id = rs.getInt("id");  // Fetch the team's ID as Integer
                String teamName = rs.getString("teamName");  // Fetch the team's name
                UUID ownerUUID = UUID.fromString(rs.getString("ownerUUID"));  // Fetch and convert the UUID

                // Now, pass the correct WocTeamsDatabaseManager instance
                com.jeepy.teams.Team team = new com.jeepy.teams.Team(id, teamName, ownerUUID, teamsDatabaseManager);  // Pass teamsDatabaseManager instead of plugin
                teams.add(team);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch teams from database", e);
            throw new SQLException("Could not fetch teams from database", e);
        }
        return teams;
    }

    public synchronized void saveTeamToOutpostsDb(Integer teamId, String teamName, UUID ownerUUID) throws SQLException {
        plugin.getLogger().info("Saving team to Outposts DB: ID=" + teamId + ", Name=" + teamName + ", OwnerUUID=" + ownerUUID);
        if (teamId == null || teamName == null || ownerUUID == null) {
            throw new SQLException("Team ID, Team Name, or Owner UUID cannot be null");
        }

        String sql = "INSERT OR REPLACE INTO teams (id, teamName, ownerUUID) VALUES (?, ?, ?)";
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

    // ---- Outpost-related method ----

    public Outpost getOutpostByName(String outpostName) throws SQLException {
        String query = "SELECT id, world, x, y, z FROM beacons WHERE outpostName = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, outpostName);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String id = rs.getString("id");
                World world = plugin.getServer().getWorld(rs.getString("world"));
                if (world != null) {
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    Location location = new Location(world, x, y, z);
                    return new Outpost(outpostName, location);
                }
            }
        }
        return null;
    }

    public String getOutpostNameByLocation(Location location) throws SQLException {
        String query = "SELECT outpostName FROM beacons WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, location.getWorld().getName());
            statement.setDouble(2, location.getX());
            statement.setDouble(3, location.getY());
            statement.setDouble(4, location.getZ());

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getString("outpostName");
            }
        }
        return null;
    }

}
