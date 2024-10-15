package com.jeepy.wocoutposts.managers;

import com.jeepy.wocoutposts.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private Main plugin;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();  // Get config from plugin

        // General Outpost Settings
        int outpostRadius = config.getInt("outpost.radius", 15);
        double chargeReductionRate = config.getDouble("outpost.charge_reduction_rate", 1.0);
        int overtimeDuration = config.getInt("outpost.overtime.duration", 5);
        double overtimeReductionRate = config.getDouble("outpost.overtime.reduction_rate", 0.05);

        plugin.getLogger().info("Outpost radius: " + outpostRadius);
        plugin.getLogger().info("Charge reduction rate: " + chargeReductionRate + "%");
        plugin.getLogger().info("Overtime duration: " + overtimeDuration + " seconds");
        plugin.getLogger().info("Overtime reduction rate: " + overtimeReductionRate + "% per loop");

        // Classified Outpost Settings
        int classifiedOutpostCaptureRadius = config.getInt("classified_outpost.capture_radius", 20);
        double classifiedChargeReductionRate = config.getDouble("classified_outpost.charge_reduction_rate", 1.5);
        int classifiedOvertimeDuration = config.getInt("classified_outpost.overtime.duration", 10);
        double classifiedOvertimeReductionRate = config.getDouble("classified_outpost.overtime.reduction_rate", 0.1);

        plugin.getLogger().info("Classified Outpost capture radius: " + classifiedOutpostCaptureRadius);
        plugin.getLogger().info("Classified Outpost charge reduction rate: " + classifiedChargeReductionRate + "%");
        plugin.getLogger().info("Classified Outpost overtime duration: " + classifiedOvertimeDuration + " seconds");
        plugin.getLogger().info("Classified Outpost overtime reduction rate: " + classifiedOvertimeReductionRate + "% per loop");

        // LootPool System Settings
        List<Integer> lootRefillIntervals = config.getIntegerList("lootpool.refill_intervals");
        int maxItemsPerPool = config.getInt("lootpool.max_items_per_pool", 27);
        double commonDropRate = config.getDouble("lootpool.rarity_drop_rates.common", 50.0);
        double rareDropRate = config.getDouble("lootpool.rarity_drop_rates.rare", 30.0);
        double epicDropRate = config.getDouble("lootpool.rarity_drop_rates.epic", 15.0);
        double legendaryDropRate = config.getDouble("lootpool.rarity_drop_rates.legendary", 5.0);

        plugin.getLogger().info("Loot refill intervals: " + lootRefillIntervals);
        plugin.getLogger().info("Max items per loot pool: " + maxItemsPerPool);
        plugin.getLogger().info("Common drop rate: " + commonDropRate + "%");
        plugin.getLogger().info("Rare drop rate: " + rareDropRate + "%");
        plugin.getLogger().info("Epic drop rate: " + epicDropRate + "%");
        plugin.getLogger().info("Legendary drop rate: " + legendaryDropRate + "%");

        // Classified Document Settings
        int classifiedDocumentSpawnPercentage = config.getInt("classified_document.spawn_percentage", 100);
        boolean classifiedBroadcast = config.getBoolean("classified_document.broadcast", true);
        boolean randomizedChest = config.getBoolean("classified_document.randomized_chest", true);

        plugin.getLogger().info("Classified document spawn percentage: " + classifiedDocumentSpawnPercentage + "%");
        plugin.getLogger().info("Classified document broadcast: " + classifiedBroadcast);
        plugin.getLogger().info("Classified document randomized chest: " + randomizedChest);

        // Scoreboard Settings
        int scoreboardUpdateInterval = config.getInt("scoreboard.update_interval", 5);
        boolean displayOutpostCharge = config.getBoolean("scoreboard.display.outpost_charge_percentage", true);
        boolean displayTeamProgress = config.getBoolean("scoreboard.display.team_progress", true);
        boolean displayLootPoolStatus = config.getBoolean("scoreboard.display.loot_pool_status", true);

        plugin.getLogger().info("Scoreboard update interval: " + scoreboardUpdateInterval + " seconds");
        plugin.getLogger().info("Display outpost charge percentage: " + displayOutpostCharge);
        plugin.getLogger().info("Display team progress: " + displayTeamProgress);
        plugin.getLogger().info("Display loot pool status: " + displayLootPoolStatus);
    }

    public boolean isDataTransferEnabled() {
        FileConfiguration config = plugin.getConfig();
        return config.getBoolean("data-transfer-enabled", true);  // Default to true if not set
    }

}
