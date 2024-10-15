package com.jeepy.wocoutposts;

import com.jeepy.database.WocTeamsDatabaseManager;
import com.jeepy.wocoutposts.commands.OutpostCommand;
import com.jeepy.wocoutposts.database.OutpostDatabaseManager;
import com.jeepy.wocoutposts.listeners.ClassifiedOutpostListener;
import com.jeepy.wocoutposts.managers.ConfigManager;
import com.jeepy.wocoutposts.managers.LootPoolManager;
import com.jeepy.wocoutposts.managers.OutpostManager;
import com.jeepy.wocoutposts.objectives.ClassifiedOutpost;
import com.jeepy.wocoutposts.objectives.Outpost;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private WocTeamsDatabaseManager teamsDatabaseManager;
    private OutpostDatabaseManager wocOutpostsDatabaseManager;
    private DataTransferManager dataTransferManager;
    private static OutpostDatabaseManager databaseManager;
    private final Map<String, ClassifiedOutpost> activeClassifiedOutposts = new ConcurrentHashMap<>();
    private OutpostManager outpostManager;
    private LootPoolManager lootPoolManager;

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Initialize ConfigManager and load config values
        configManager = new ConfigManager(this);
        configManager.loadConfigValues();  // This method will handle loading all configurable settings

        // Get the Woc-Teams Main class instance
        com.jeepy.Main wocTeamsMain = JavaPlugin.getPlugin(com.jeepy.Main.class);

        // Initialize the Woc-Teams and Woc-Outposts database managers
        teamsDatabaseManager = new WocTeamsDatabaseManager(wocTeamsMain);
        String wocDbPath = getDataFolder().getAbsolutePath() + "/outposts.db";
        wocOutpostsDatabaseManager = new OutpostDatabaseManager(this, wocDbPath);
        databaseManager = wocOutpostsDatabaseManager;

        try {
            teamsDatabaseManager.connect();
            wocOutpostsDatabaseManager.connect();

            teamsDatabaseManager.initialize();
            wocOutpostsDatabaseManager.initialize();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "An error occurred during database setup", e);
            return;
        }

        // Initialize Managers
        getLogger().info("Initializing LootPoolManager...");
        lootPoolManager = new LootPoolManager();

        getLogger().info("Initializing OutpostManager...");
        outpostManager = new OutpostManager(lootPoolManager, this);

        if (lootPoolManager != null && outpostManager != null) {
            getLogger().info("Both LootPoolManager and OutpostManager are initialized successfully.");
        }

        // Create and register outposts dynamically based on configuration
        initializeOutpostsFromConfig();

        // Conditional data transfer based on config
        dataTransferManager = new DataTransferManager(teamsDatabaseManager, wocOutpostsDatabaseManager, this);
        if (configManager.isDataTransferEnabled()) {
            try {
                dataTransferManager.transferTeamsData();
                getLogger().info("Teams data transferred successfully.");
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Error during data transfer from Woc-Teams to Woc-Outposts", e);
            }
        }

        // Setup command executors
        if (this.getCommand("outpost") != null) {
            this.getCommand("outpost").setExecutor(new OutpostCommand(outpostManager, lootPoolManager));
        }

        if (this.getCommand("lootpool") != null) {
            this.getCommand("lootpool").setExecutor(new OutpostCommand(outpostManager, lootPoolManager));
        }

        // Add the BukkitRunnable to update outpost charging every second
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Outpost outpost : outpostManager.getActiveClassifiedOutposts()) {
                    if (outpost instanceof ClassifiedOutpost) {
                        ((ClassifiedOutpost) outpost).updateCharging();  // Call updateCharging from ClassifiedOutpost
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);  // Run every second (20 ticks)
    }

    @Override
    public void onDisable() {
        try {
            teamsDatabaseManager.close();
            wocOutpostsDatabaseManager.close();
        } catch (SQLException e) {
            getLogger().severe("Error closing database connections: " + e.getMessage());
            e.printStackTrace();
        }
        getLogger().info("Woc-Outposts plugin has been disabled.");
    }

    public OutpostDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Map<String, ClassifiedOutpost> getActiveClassifiedOutposts() {
        return activeClassifiedOutposts;
    }

    public OutpostManager getOutpostManager() {
        return outpostManager;
    }

    public WocTeamsDatabaseManager getTeamsDatabaseManager() {
        return teamsDatabaseManager;
    }

    // Initialize outposts dynamically from the configuration
    private void initializeOutpostsFromConfig() {
        // Access the outposts section from the config
        if (!getConfig().contains("outposts")) {
            getLogger().warning("No outposts found in the configuration.");
            return;
        }

        ConfigurationSection outpostsSection = getConfig().getConfigurationSection("outposts");

        if (outpostsSection == null) {
            getLogger().warning("The outposts section is null in the config.");
            return;
        }

        for (String outpostName : outpostsSection.getKeys(false)) {
            ConfigurationSection outpostConfig = outpostsSection.getConfigurationSection(outpostName);

            if (outpostConfig == null) {
                getLogger().warning("No configuration found for outpost: " + outpostName);
                continue;
            }

            String worldName = outpostConfig.getString("world");
            double x = outpostConfig.getDouble("x");
            double y = outpostConfig.getDouble("y");
            double z = outpostConfig.getDouble("z");

            World world = getServer().getWorld(worldName);
            if (world == null) {
                getLogger().warning("World " + worldName + " not found for outpost: " + outpostName);
                continue;
            }

            Location beaconLocation = new Location(world, x, y, z);

            // Create the outpost instance and store it
            ClassifiedOutpost classifiedOutpost = new ClassifiedOutpost(outpostName, beaconLocation, this);
            outpostManager.addOutpost(outpostName, classifiedOutpost);
            getLogger().info("Outpost " + outpostName + " loaded successfully.");
        }
    }

    // Utility method to load a location from config
    private Location loadLocationFromConfig(String path) {
        if (getConfig().contains(path + ".world")) {
            String worldName = getConfig().getString(path + ".world");
            double x = getConfig().getDouble(path + ".x");
            double y = getConfig().getDouble(path + ".y");
            double z = getConfig().getDouble(path + ".z");
            return new Location(getServer().getWorld(worldName), x, y, z);
        }
        return null;
    }
}
