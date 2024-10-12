package com.jeepy.wocoutposts.listeners;

import com.jeepy.wocoutposts.managers.LootPoolManager;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ChestListener implements Listener {
    private final Plugin plugin;
    private final Set<Location> markedChests; // Ensure this set is updated externally as per your logic
    private final LootPoolManager lootPoolManager;
    private BukkitTask refillTask; // Declaration of refillTask

    public ChestListener(Plugin plugin, LootPoolManager lootPoolManager) {
        this.plugin = plugin;
        this.markedChests = new HashSet<>(); // Assume this will be updated by external logic
        this.lootPoolManager = lootPoolManager;
    }

    public void scheduleRefillTask(int refillInterval) {
        if (refillTask != null) {
            refillTask.cancel();  // Cancel the existing task if it exists
        }
        refillTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refillChests, 0L, refillInterval * 20L); // refillInterval in seconds
    }

    public void refillChests() {
        Random random = new Random();
        for (Location location : markedChests) {
            BlockState state = location.getBlock().getState();
            if (state instanceof Chest) {
                Chest chest = (Chest) state;
                Inventory chestInventory = chest.getInventory();
                chestInventory.clear();
                addItemToChest(chestInventory, random);
            }
        }
        plugin.getLogger().info("Marked chests have been refilled.");
    }

    public void stopRefillTask() {
        if (refillTask != null) {
            refillTask.cancel();
            refillTask = null;
            plugin.getLogger().info("Refill task has been stopped.");
        }
    }

    private void addItemToChest(Inventory chestInventory, Random random) {
        Map<ItemStack, String> lootPool = lootPoolManager.getLootPool();
        double chance;
        for (Map.Entry<ItemStack, String> entry : lootPool.entrySet()) {
            chance = getRarityChance(entry.getValue());
            if (random.nextDouble() <= chance) {
                chestInventory.addItem(entry.getKey());
            }
        }
    }

    public double getRarityChance(String rarity) {
        switch (rarity.toLowerCase()) {
            case "common":    return 0.6;
            case "uncommon":  return 0.25;
            case "rare":      return 0.10;
            case "epic":      return 0.04;
            case "legendary": return 0.01;
            default:          return 0.0;
        }
    }
}