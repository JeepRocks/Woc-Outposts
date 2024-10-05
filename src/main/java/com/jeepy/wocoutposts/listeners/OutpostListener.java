package com.jeepy.wocoutposts.listeners;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OutpostListener implements Listener {

    private final ConfigManager configManager;

    public OutpostListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    // Detect placement of the Outpost Block (custom beacon) and create an outpost
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block placedBlock = event.getBlockPlaced();

        // Check if item exists and is a beacon
        if (item != null && item.getType() == Material.BEACON) {
            ItemMeta meta = item.getItemMeta();

            // Check if item has metadata and matches display name (case-insensitive)
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equalsIgnoreCase("Outpost Block")) {

                // Additional validation (optional): Check lore or other custom data for extra validation
                if (meta.hasLore() && meta.getLore().contains("Place this to create an outpost.")) {

                    Main mainPlugin = (Main) configManager.getPlugin();  // Cast to Main to access custom methods

                    // Check if an objective is already active
                    if (mainPlugin.getObjectiveManager().isObjectiveActive()) {
                        player.sendMessage("An objective is already active. Wait until it is completed before placing another outpost.");
                        event.setCancelled(true);
                        return;
                    }

                    // This is a valid Outpost Block - set the outpost location and start the objective
                    mainPlugin.getObjectiveManager().setOutpostLocation(placedBlock.getLocation());
                    mainPlugin.getObjectiveManager().startObjective();
                    player.sendMessage("You have placed an Outpost Block! The outpost has been created.");
                } else {
                    player.sendMessage("This beacon is not a valid Outpost Block.");
                }
            }
        }
    }
}