package com.jeepy.wocoutposts.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File customConfigFile;
    private FileConfiguration customConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();  // Ensure the default config.yml is saved
        createCustomConfig();
    }

    // Getter for the plugin
    public JavaPlugin getPlugin() {
        return plugin;
    }

    // Create or load the custom configuration (WOC-Outposts.yml)
    public void createCustomConfig() {
        customConfigFile = new File(plugin.getDataFolder(), "WOC-Outposts.yml");

        if (!customConfigFile.exists()) {
            try {
                customConfigFile.getParentFile().mkdirs();
                customConfigFile.createNewFile();
                plugin.getLogger().info("Created WOC-Outposts.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create WOC-Outposts.yml: " + e.getMessage());
            }
        }

        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    // Save the custom config file
    public void saveCustomConfig() {
        if (customConfigFile != null && customConfig != null) {
            try {
                customConfig.save(customConfigFile);
                plugin.getLogger().info("WOC-Outposts.yml has been saved.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save WOC-Outposts.yml: " + e.getMessage());
            }
        }
    }

    // Get the custom config
    public FileConfiguration getCustomConfig() {
        return customConfig;
    }
}
