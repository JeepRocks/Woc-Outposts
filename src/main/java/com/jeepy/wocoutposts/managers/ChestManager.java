package com.jeepy.wocoutposts.managers;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.enums.Rarity;
import com.jeepy.wocoutposts.gui.LootItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChestManager {

    private Main plugin;
    private List<Location> activeChests; // Store chest locations as Location objects
    private Random random;

    // Rarity percentages, configurable via WOC-Outposts.yml
    private int commonChance;
    private int uncommonChance;
    private int rareChance;
    private int epicChance;
    private int legendaryChance;

    public ChestManager(Main plugin) {
        this.plugin = plugin;
        this.activeChests = new ArrayList<>();
        this.random = new Random();

        // Load rarity percentages from WOC-Outposts.yml
        loadRarityChances();
    }

    // Load rarity chances from WOC-Outposts.yml
    private void loadRarityChances() {
        this.commonChance = plugin.getCustomConfig().getInt("loot.rarity.common", 60);
        this.uncommonChance = plugin.getCustomConfig().getInt("loot.rarity.uncommon", 25);
        this.rareChance = plugin.getCustomConfig().getInt("loot.rarity.rare", 10);
        this.epicChance = plugin.getCustomConfig().getInt("loot.rarity.epic", 5);
        this.legendaryChance = plugin.getCustomConfig().getInt("loot.rarity.legendary", 1);
    }

    // Add a chest location to the list
    public void addChest(Location chestLocation) {
        if (chestLocation.getBlock().getType() == Material.CHEST) {
            activeChests.add(chestLocation);
        }
    }

    // Refill all active chests
    public void refillChests() {
        if (!plugin.getObjectiveManager().isObjectiveActive()) return;

        for (Location location : activeChests) {
            if (location.getBlock().getType() == Material.CHEST) {
                Chest chest = (Chest) location.getBlock().getState();
                chest.getInventory().clear();  // Clear the current inventory

                // Roll for 5 items and add them to the chest
                List<ItemStack> selectedItems = rollForItems(5);  // Roll for 5 items
                for (ItemStack item : selectedItems) {
                    chest.getInventory().addItem(item);  // Add items to chest
                }
            } else {
                plugin.getLogger().warning("Block at " + location + " is not a chest.");
            }
        }
    }

    // Roll for multiple items (e.g., 5 items)
    private List<ItemStack> rollForItems(int numberOfItems) {
        List<ItemStack> selectedItems = new ArrayList<>();
        for (int i = 0; i < numberOfItems; i++) {
            ItemStack item = rollForItem();
            if (item != null) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    // RNG logic to roll for an item based on rarity
    private ItemStack rollForItem() {
        int roll = random.nextInt(100);  // Random number between 0 and 99

        Rarity selectedRarity;
        if (roll < commonChance) {
            selectedRarity = Rarity.COMMON;
        } else if (roll < commonChance + uncommonChance) {
            selectedRarity = Rarity.UNCOMMON;
        } else if (roll < commonChance + uncommonChance + rareChance) {
            selectedRarity = Rarity.RARE;
        } else if (roll < commonChance + uncommonChance + rareChance + epicChance) {
            selectedRarity = Rarity.EPIC;
        } else {
            selectedRarity = Rarity.LEGENDARY;
        }

        // Get items of the selected rarity from the loot pool
        List<LootItem> filteredItems = plugin.getLootPoolGUI().getLootPoolItems().stream()
                .filter(lootItem -> lootItem.getRarity() == selectedRarity)
                .collect(Collectors.toList());

        if (!filteredItems.isEmpty()) {
            // Randomly pick an item from the filtered list
            return filteredItems.get(random.nextInt(filteredItems.size())).getItem();
        }

        return null;  // If no items of that rarity are available
    }

    // Periodically refill chests while the objective is active
    public void startChestRefillTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::refillChests, 0L, 600L); // 600 ticks = 30 seconds
    }
}