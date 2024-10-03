package com.jeepy.wocoutposts;

import com.jeepy.wocoutposts.commands.OutpostCommand;
import com.jeepy.wocoutposts.gui.LootPoolGUI;
import com.jeepy.wocoutposts.listeners.OutpostListener;
import com.jeepy.wocoutposts.managers.ChestManager;
import com.jeepy.wocoutposts.managers.ObjectiveManager;
import com.jeepy.wocoutposts.scheduler.ObjectiveScheduler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private ObjectiveManager objectiveManager;
    private ChestManager chestManager;
    private LootPoolGUI lootPoolGUI;
    private File customConfigFile;
    private FileConfiguration customConfig;

    @Override
    public void onEnable() {
        // Load or create the default configuration file (config.yml)
        saveDefaultConfig(); // This manages config.yml

        // Load or create the custom loot configuration file (WOC-Outposts.yml)
        createCustomConfig();

        // Initialize all the managers and systems
        this.objectiveManager = new ObjectiveManager(this);
        this.chestManager = new ChestManager(this);
        this.lootPoolGUI = new LootPoolGUI(this);

        // Register any commands, events, and schedules
        getCommand("outpost").setExecutor(new OutpostCommand(this));
        getServer().getScheduler().runTaskTimer(this, new ObjectiveScheduler(objectiveManager, this), 0L, 72000L); // 6 hours interval
        getServer().getPluginManager().registerEvents(new OutpostListener(this), this);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);  // Cancel all tasks associated with this plugin
        saveCustomConfig();  // Ensure WOC-Outposts.yml is saved on disable
        saveConfig(); // Save the regular config.yml
    }

    // Create or load the custom loot table config file (WOC-Outposts.yml)
    public void createCustomConfig() {
        customConfigFile = new File(getDataFolder(), "WOC-Outposts.yml");

        if (!customConfigFile.exists()) {
            try {
                // Create the WOC-Outposts.yml file only if it doesn't exist
                customConfigFile.getParentFile().mkdirs();
                customConfigFile.createNewFile();  // Create a new empty config file
                getLogger().info("Created WOC-Outposts.yml file as it did not exist.");
            } catch (IOException e) {
                getLogger().severe("Could not create WOC-Outposts.yml");
                e.printStackTrace();
            }
        }

        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);  // Load the custom config
    }

    // Save the custom loot table config file
    public void saveCustomConfig() {
        try {
            if (customConfigFile != null && customConfig != null) {
                customConfig.save(customConfigFile);  // Save the WOC-Outposts.yml file only if initialized properly
                getLogger().info("WOC-Outposts.yml has been saved.");
            }
        } catch (IOException e) {
            getLogger().severe("Could not save WOC-Outposts.yml");
            e.printStackTrace();
        }
    }

    // Load the custom loot table config
    public void loadCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "WOC-Outposts.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    // Get the custom loot table config
    public FileConfiguration getCustomConfig() {
        return this.customConfig;
    }

    public ObjectiveManager getObjectiveManager() {
        return objectiveManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }

    public LootPoolGUI getLootPoolGUI() {
        return lootPoolGUI;
    }
}