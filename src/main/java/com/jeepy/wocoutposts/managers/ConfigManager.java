package com.jeepy.wocoutposts.managers;

import com.jeepy.wocoutposts.database.OutpostDatabaseManager;
import com.jeepy.wocoutposts.objectives.Outpost;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;
    private static final String LOOTPOOL_PATH = "lootpool";
    private final Map<String, Outpost> outpostMap;
    private final OutpostDatabaseManager databaseManager;  // Reference to the OutpostDatabaseManager for database interactions
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(JavaPlugin plugin, OutpostDatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.config = plugin.getConfig();
        loadConfig();
        this.outpostMap = new HashMap<>();
        this.config = plugin.getConfig();
        loadOutpostsFromConfig();  // Load outposts from config file
    }

    // Load config and ensure it's available
    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    // Save config changes
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile.getName(), e);
        }
    }

    // ---- Outpost Handling ----

    // 1. Load outposts from the config file (no ID needed)
    private void loadOutpostsFromConfig() {
        List<Map<?, ?>> outpostsList = config.getMapList("outposts");
        for (Map<?, ?> outpostData : outpostsList) {
            String name = (String) outpostData.get("name");
            String worldName = (String) outpostData.get("world");
            World world = plugin.getServer().getWorld(worldName);

            if (world != null) {
                double x = ((Number) outpostData.get("x")).doubleValue();
                double y = ((Number) outpostData.get("y")).doubleValue();
                double z = ((Number) outpostData.get("z")).doubleValue();
                Location location = new Location(world, x, y, z);
                // Create the Outpost using name and location only
                Outpost outpost = new Outpost(name, location);  // No 'id' needed
                outpostMap.put(name.toLowerCase(), outpost);
            } else {
                plugin.getLogger().warning("World " + worldName + " for outpost " + name + " not found.");
            }
        }
    }

    // Add a new outpost to the config
    public void addOutpost(String outpostName, Location location) {
        if (outpostName == null || location == null || location.getWorld() == null) {
            plugin.getLogger().severe("Error: Outpost name, location, or world is null. Cannot add outpost.");
            return;  // Exit early if outpostName, location, or world is null
        }

        // Ensure the config is initialized before setting values
        if (config == null) {
            plugin.getLogger().severe("Error: Config is null. Make sure loadConfig() was called properly.");
            return;
        }

        // Debug logs to check what's being passed
        plugin.getLogger().info("Saving outpost: " + outpostName + " at location: " + location);

        config.set("outposts." + outpostName + ".world", location.getWorld().getName());
        config.set("outposts." + outpostName + ".x", location.getX());
        config.set("outposts." + outpostName + ".y", location.getY());
        config.set("outposts." + outpostName + ".z", location.getZ());
        saveConfig();
    }

    // Method to remove an outpost
    public void removeOutpost(String outpostName) {
        plugin.getConfig().set("outposts." + outpostName, null);  // Remove the outpost from the config
        plugin.saveConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();  // Reload the configuration from disk
        this.config = plugin.getConfig();  // Reassign the config reference to the reloaded config
    }

    // ---- Loot Pool Handling ----

    // Load items and rarities from the config
    public Map<ItemStack, String> loadItemsFromConfig() {
        Map<ItemStack, String> lootPool = new HashMap<>();

        if (config.contains(LOOTPOOL_PATH)) {
            for (String key : config.getConfigurationSection(LOOTPOOL_PATH).getKeys(false)) {
                Material material = Material.getMaterial(key);
                if (material != null) {
                    ItemStack itemStack = new ItemStack(material);
                    String rarity = config.getString(LOOTPOOL_PATH + "." + key);
                    lootPool.put(itemStack, rarity);
                }
            }
        }
        return lootPool;
    }

    // Add item to the config loot pool
    public void addItemToConfig(ItemStack itemStack, String rarity) {
        config.set(LOOTPOOL_PATH + "." + itemStack.getType().toString(), rarity);
        saveConfig();
    }

    // Remove item from config loot pool
    public void removeItemFromConfig(ItemStack itemStack) {
        config.set(LOOTPOOL_PATH + "." + itemStack.getType().toString(), null);
        saveConfig();
    }

    // Update the item rarity in the config loot pool
    public void updateItemRarityConfig(ItemStack itemStack, String rarity) {
        config.set(LOOTPOOL_PATH + "." + itemStack.getType().toString(), rarity);
        saveConfig();
    }

    // ---- Chest Location Handling ----

    public void setChestLocations(List<Location> locations) {
        List<Map<String, Object>> chests = new ArrayList<>();

        for (Location location : locations) {
            Map<String, Object> chest = new HashMap<>();
            chest.put("world", location.getWorld().getName());
            chest.put("x", location.getX());
            chest.put("y", location.getY());
            chest.put("z", location.getZ());
            chests.add(chest);
        }

        config.set("chests", chests);
        saveConfig();
    }

}
