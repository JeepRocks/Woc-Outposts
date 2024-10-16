package com.jeepy.wocoutposts.managers;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.objectives.Outpost;
import com.jeepy.wocoutposts.objectives.ClassifiedOutpost;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OutpostManager {

    private Map<String, Outpost> outposts = new HashMap<>();
    private LootPoolManager lootPoolManager;
    private final Main plugin;  // Reference to Main plugin instance
    private boolean debugMode = false;

    // Constructor that accepts LootPoolManager and Main plugin instance
    public OutpostManager(LootPoolManager lootPoolManager, Main plugin) {
        this.lootPoolManager = lootPoolManager;
        this.plugin = plugin;  // Assign the Main plugin instance
    }

    // Create a new outpost
    public void createOutpost(Player player, String outpostName) {
        if (outposts.containsKey(outpostName)) {
            player.sendMessage("Outpost " + outpostName + " already exists.");
            return;
        }

        Block targetBlock = getTargetBeacon(player);
        if (targetBlock == null || targetBlock.getType() != Material.BEACON) {
            player.sendMessage("You must be looking at a beacon to create an outpost.");
            return;
        }

        Location beaconLocation = targetBlock.getLocation();

        // Create a new ClassifiedOutpost, passing the Main plugin instance
        Outpost newOutpost = new ClassifiedOutpost(outpostName, beaconLocation, plugin);
        outposts.put(outpostName, newOutpost);

        // Store outpost in the configuration for persistence
        plugin.getConfig().set("outposts." + outpostName + ".world", beaconLocation.getWorld().getName());
        plugin.getConfig().set("outposts." + outpostName + ".x", beaconLocation.getX());
        plugin.getConfig().set("outposts." + outpostName + ".y", beaconLocation.getY());
        plugin.getConfig().set("outposts." + outpostName + ".z", beaconLocation.getZ());
        plugin.saveConfig();

        player.sendMessage("Outpost " + outpostName + " has been created at beacon location: " + formatLocation(beaconLocation));
    }


    public void addOutpost(String outpostName, Outpost outpost) {
        if (!outposts.containsKey(outpostName)) {
            outposts.put(outpostName, outpost);
            plugin.getLogger().info("Outpost " + outpostName + " has been added to the manager.");
        }
    }

    // Utility method to check if the player is looking at a beacon block
    private Block getTargetBeacon(Player player) {
        BlockIterator iterator = new BlockIterator(player, 10);  // Look up to 10 blocks ahead
        Block targetBlock;
        while (iterator.hasNext()) {
            targetBlock = iterator.next();
            if (targetBlock.getType() == Material.BEACON) {
                return targetBlock;
            }
        }
        return null;  // No beacon found in the player's line of sight
    }

    // Utility method to format the location for player messages
    private String formatLocation(Location location) {
        return "X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ();
    }

    // Delete an existing outpost
    public void deleteOutpost(Player player, String outpostName) {
        if (!outposts.containsKey(outpostName)) {
            player.sendMessage("Outpost " + outpostName + " does not exist.");
            return;
        }

        outposts.remove(outpostName);
        lootPoolManager.deleteLootPool(outpostName);
        player.sendMessage("Outpost " + outpostName + " has been deleted.");
    }

    // Start charging the specified outpost
    public void startOutpost(Player player, String outpostName) {
        Outpost outpost = outposts.get(outpostName);

        if (outpost == null) {
            player.sendMessage("Outpost " + outpostName + " does not exist.");
            return;
        }

        if (outpost instanceof ClassifiedOutpost) {
            ClassifiedOutpost classifiedOutpost = (ClassifiedOutpost) outpost;
            classifiedOutpost.setChargingEnabled(true);
            classifiedOutpost.resetCharge(); // Add a method to reset the charge and lastChargeTime
            outpost.startCharging();
            player.sendMessage("Charging for " + outpostName + " has started.");
        } else {
            player.sendMessage("Outpost " + outpostName + " is not a ClassifiedOutpost and cannot start charging.");
        }
    }

    // Stop charging the specified outpost
    public void stopOutpost(Player player, String outpostName) {
        Outpost outpost = outposts.get(outpostName);

        if (outpost == null) {
            player.sendMessage("Outpost " + outpostName + " does not exist.");
            return;
        }

        // Ensure only ClassifiedOutpost stops charging
        if (outpost instanceof ClassifiedOutpost) {
            ((ClassifiedOutpost) outpost).stopOutpost();
            player.sendMessage("ClassifiedOutpost " + outpostName + " has stopped charging.");
        } else {
            player.sendMessage("Outpost " + outpostName + " is not a ClassifiedOutpost and cannot stop charging.");
        }
    }

    // Manually refill loot for an outpost
    public void refillLoot(Player player, String outpostName) {
        Outpost outpost = outposts.get(outpostName);

        if (outpost == null) {
            player.sendMessage("Outpost " + outpostName + " does not exist.");
            return;
        }

        if (outpost instanceof ClassifiedOutpost) {
            outpost.refillLoot();
            player.sendMessage("Loot chests have been refilled for outpost " + outpostName + ".");
        } else {
            player.sendMessage("Refill loot is not applicable for this outpost type.");
        }
    }

    // Toggle debug mode
    public void toggleDebug(Player player) {
        debugMode = !debugMode;
        String status = debugMode ? "enabled" : "disabled";
        player.sendMessage("Debug mode " + status + ".");
    }

    // Helper method to check if debug mode is enabled
    public boolean isDebugMode() {
        return debugMode;
    }

    public Collection<Outpost> getActiveClassifiedOutposts() {
        return outposts.values();
    }

}
