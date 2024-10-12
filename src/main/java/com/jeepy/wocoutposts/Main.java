package com.jeepy.wocoutposts;

import com.jeepy.database.WocTeamsDatabaseManager;
import com.jeepy.wocoutposts.commands.OutpostCommand;
import com.jeepy.wocoutposts.database.OutpostDatabaseManager;  // Outpost-specific database manager // Teams-specific database manager
import com.jeepy.wocoutposts.gui.LootPoolGUI;
import com.jeepy.wocoutposts.listeners.ChestListener;
import com.jeepy.wocoutposts.listeners.LootPoolGUIListener;
import com.jeepy.wocoutposts.managers.ConfigManager;
import com.jeepy.wocoutposts.managers.LootPoolManager;
import com.jeepy.wocoutposts.objectives.ClassifiedOutpost;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static ConfigManager configManager;
    private LootPoolGUI lootPoolGUI;
    private ChestListener chestListener;
    private WocTeamsDatabaseManager teamsDatabaseManager;  // Woc-Teams DB manager
    private OutpostDatabaseManager wocOutpostsDatabaseManager;  // Woc-Outposts DB manager
    private DataTransferManager dataTransferManager;
    private static OutpostDatabaseManager databaseManager;  // Main outpost DB manager
    private final Map<String, ClassifiedOutpost> activeClassifiedOutposts = new ConcurrentHashMap<>();  // Thread-safe map

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Get the Woc-Teams Main class instance
        com.jeepy.Main wocTeamsMain = JavaPlugin.getPlugin(com.jeepy.Main.class);  // Fetch Woc-Teams Main class

        // Initialize the Woc-Teams and Woc-Outposts database managers
        teamsDatabaseManager = new WocTeamsDatabaseManager(wocTeamsMain);  // Pass Woc-Teams main instance
        String wocDbPath = getDataFolder().getAbsolutePath() + "/outposts.db";  // Path for Woc-Outposts database
        wocOutpostsDatabaseManager = new OutpostDatabaseManager(this, wocDbPath);  // Outposts DB manager
        databaseManager = wocOutpostsDatabaseManager;  // Assign outposts DB to static variable

        try {
            teamsDatabaseManager.connect();  // Connect to Woc-Teams database
            wocOutpostsDatabaseManager.connect();  // Connect to Woc-Outposts database

            // Initialize databases
            teamsDatabaseManager.initialize();  // Initialize Woc-Teams DB
            wocOutpostsDatabaseManager.initialize();  // Initialize Woc-Outposts DB
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "An error occurred during database setup", e);
            return;  // Early return if database setup fails
        }

        // Initialize Managers
        configManager = new ConfigManager(this, databaseManager);
        LootPoolManager lootPoolManager = new LootPoolManager(configManager);

        // Initialize Listeners and GUI
        chestListener = new ChestListener(this, lootPoolManager);
        lootPoolGUI = new LootPoolGUI(lootPoolManager, this, chestListener);
        LootPoolGUIListener lootPoolGUIListener = new LootPoolGUIListener(lootPoolManager, lootPoolGUI, this);

        getServer().getPluginManager().registerEvents(lootPoolGUIListener, this);
        getServer().getPluginManager().registerEvents(chestListener, this);

        try {
            dataTransferManager = new DataTransferManager(teamsDatabaseManager, wocOutpostsDatabaseManager, this);
            dataTransferManager.transferTeamsData();  // Transfer teams data from Woc-Teams to Woc-Outposts
            getLogger().info("Teams data transferred successfully.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error during data transfer from Woc-Teams to Woc-Outposts", e);
        }

        try {
            // Pass both database managers to DataTransferManager to handle data transfer
            dataTransferManager = new DataTransferManager(teamsDatabaseManager, wocOutpostsDatabaseManager, this);
            dataTransferManager.transferTeamsData();  // Transfer teams data from Woc-Teams to Woc-Outposts
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "An error occurred during data transfer", e);
        }

        // Load chest locations and beacon location
        try {
            List<Location> chestLocations = wocOutpostsDatabaseManager.loadChestLocations();
            configManager.setChestLocations(chestLocations);

            Location beaconLocation = wocOutpostsDatabaseManager.loadLastBeaconLocation();
            if (beaconLocation == null) {
                World defaultWorld = getServer().getWorld("world");
                if (defaultWorld != null) {
                    beaconLocation = new Location(defaultWorld, 0, 100, 0);  // Example default location
                } else {
                    getLogger().warning("Default world 'world' does not exist. Cannot set default beacon location.");
                    return;
                }
            }

            ClassifiedOutpost classifiedOutpost = new ClassifiedOutpost(this, wocOutpostsDatabaseManager, configManager, beaconLocation);
            getServer().getPluginManager().registerEvents(classifiedOutpost, this);

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error loading data from database", e);
        }

        // Setup command executor for outposts
        int chargeTime = getConfig().getInt("charge-time");
        int refillInterval = getConfig().getInt("refill-interval");
        getCommand("opost").setExecutor(new OutpostCommand(this, chargeTime, refillInterval, configManager));
    }

    @Override
    public void onDisable() {
        try {
            teamsDatabaseManager.close();
            wocOutpostsDatabaseManager.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getLogger().info("Woc-Outposts plugin has been disabled.");
    }

    public List<Location> loadChestLocations() throws SQLException {
        return dataTransferManager.loadChestLocations();
    }

    public void saveChestLocation(Location location) throws SQLException {
        dataTransferManager.saveChestLocation(location);
    }

    public void deleteChestLocation(Location location) throws SQLException {
        dataTransferManager.deleteChestLocation(location);
    }

    public void saveBeaconLocation(Location location, String name) throws SQLException {
        wocOutpostsDatabaseManager.saveBeaconLocation(location, name);
    }

    public Location loadBeaconLocation() throws SQLException {
        return wocOutpostsDatabaseManager.loadLastBeaconLocation();
    }

    public LootPoolGUI getLootPoolGUI() {
        return lootPoolGUI;
    }

    public ChestListener getChestListener() {
        return chestListener;
    }

    public static OutpostDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public synchronized void startOutpost(String name, ClassifiedOutpost outpost) {
        if (!activeClassifiedOutposts.containsKey(name.toLowerCase())) {
            activeClassifiedOutposts.put(name.toLowerCase(), outpost);
            getLogger().info("[Outpost] Started outpost: " + name);
        } else {
            getLogger().warning("[Outpost] Attempted to start outpost '" + name + "', but it's already active.");
        }
    }

    public synchronized void stopOutpost(String name) {
        ClassifiedOutpost outpost = activeClassifiedOutposts.remove(name.toLowerCase());
        if (outpost != null) {
            outpost.stop(name);
            getLogger().info("[Outpost] Stopped outpost: " + name);
        } else {
            getLogger().warning("[Outpost] Attempted to stop outpost '" + name + "', but it was not active.");
        }
    }

    public Map<String, ClassifiedOutpost> getActiveClassifiedOutposts() {
        return activeClassifiedOutposts;
    }
}
