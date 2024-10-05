package com.jeepy.wocoutposts;

import com.jeepy.wocoutposts.commands.OutpostCommand;
import com.jeepy.wocoutposts.gui.LootPoolGUI;
import com.jeepy.wocoutposts.listeners.OutpostListener;
import com.jeepy.wocoutposts.managers.ChestManager;
import com.jeepy.wocoutposts.managers.ObjectiveManager;
import com.jeepy.wocoutposts.scheduler.ObjectiveScheduler;
import com.jeepy.wocoutposts.managers.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ObjectiveManager objectiveManager;
    private ChestManager chestManager;
    private LootPoolGUI lootPoolGUI;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("Woc-Outposts plugin is starting...");

        // Initialize configuration manager to handle both default and custom configurations
        configManager = new ConfigManager(this);
        getLogger().info("ConfigManager initialized.");

        // Initialize managers and systems
        this.objectiveManager = new ObjectiveManager(this, configManager.getCustomConfig());
        getLogger().info("ObjectiveManager initialized.");

        this.chestManager = new ChestManager(configManager);
        getLogger().info("ChestManager initialized.");

        this.lootPoolGUI = new LootPoolGUI(configManager);
        getLogger().info("LootPoolGUI initialized.");

        // Register commands, events, and schedule tasks
        getCommand("outpost").setExecutor(new OutpostCommand(configManager));
        getLogger().info("OutpostCommand registered.");

        getServer().getScheduler().runTaskTimer(this, new ObjectiveScheduler(objectiveManager, configManager), 0L, 72000L); // 6-hour interval
        getLogger().info("ObjectiveScheduler task scheduled.");

        getServer().getPluginManager().registerEvents(new OutpostListener(configManager), this);
        getLogger().info("OutpostListener registered.");

        getServer().getPluginManager().registerEvents(new LootPoolGUI(configManager), this);
        getLogger().info("LootPoolGUI registered.");

        getLogger().info("Woc-Outposts plugin has been successfully enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Woc-Outposts plugin is shutting down...");

        // Cancel running tasks if objectives are active
        if (objectiveManager != null && objectiveManager.isObjectiveActive()) {
            getServer().getScheduler().cancelTasks(this);
            getLogger().info("All tasks associated with Woc-Outposts plugin have been canceled.");

            ObjectiveScheduler scheduler = objectiveManager.getScheduler();
            if (scheduler != null) {
                scheduler.saveRemainingTime();
                getLogger().info("Remaining time for active objectives has been saved.");
            }
        }

        // Save configurations
        configManager.saveCustomConfig();
        getLogger().info("Custom configuration (WOC-Outposts.yml) has been saved.");

        saveConfig();  // Save the default config.yml
        getLogger().info("Default configuration (config.yml) has been saved.");

        getLogger().info("Woc-Outposts plugin has been successfully disabled.");
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