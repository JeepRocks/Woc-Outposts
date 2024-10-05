package com.jeepy.wocoutposts.managers;

import com.jeepy.wocoutposts.enums.Rarity;
import com.jeepy.wocoutposts.gui.LootItem;
import com.jeepy.wocoutposts.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChestManager {

    private final ConfigManager configManager;
    private final List<Location> activeChests;
    private final Random random;

    private int commonChance;
    private int uncommonChance;
    private int rareChance;
    private int epicChance;
    private int legendaryChance;

    public ChestManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.activeChests = new ArrayList<>();
        this.random = new Random();
        loadRarityChances();
    }

    private void loadRarityChances() {
        this.commonChance = configManager.getCustomConfig().getInt("loot.rarity.common", 60);
        this.uncommonChance = configManager.getCustomConfig().getInt("loot.rarity.uncommon", 25);
        this.rareChance = configManager.getCustomConfig().getInt("loot.rarity.rare", 10);
        this.epicChance = configManager.getCustomConfig().getInt("loot.rarity.epic", 5);
        this.legendaryChance = configManager.getCustomConfig().getInt("loot.rarity.legendary", 1);
    }

    public void addChest(Location chestLocation) {
        if (chestLocation.getBlock().getType() == Material.CHEST) {
            activeChests.add(chestLocation);
        } else {
            configManager.getPlugin().getLogger().warning("Tried to add a non-chest block as a chest at " + chestLocation);
        }
    }

    public void refillChests() {
        Main mainPlugin = (Main) configManager.getPlugin();

        if (!mainPlugin.getObjectiveManager().isObjectiveActive()) return;

        int itemsPerChest = configManager.getCustomConfig().getInt("loot.items_per_chest", 5);  // Configurable number of items

        for (Location location : activeChests) {
            if (location.getBlock().getType() == Material.CHEST) {
                Chest chest = (Chest) location.getBlock().getState();
                chest.getBlockInventory().clear();

                List<ItemStack> items = rollForItems(itemsPerChest);
                for (ItemStack item : items) {
                    if (item != null) {
                        chest.getBlockInventory().addItem(item);
                    }
                }

                configManager.getPlugin().getLogger().info("Refilled chest at " + location);
            } else {
                configManager.getPlugin().getLogger().warning("Block at " + location + " is not a chest.");
            }
        }
    }

    // Method to roll for multiple items
    private List<ItemStack> rollForItems(int numberOfItems) {
        List<ItemStack> selectedItems = new ArrayList<>();
        for (int i = 0; i < numberOfItems; i++) {
            ItemStack item = rollForItem();
            if (item != null) {
                selectedItems.add(item);
            } else {
                configManager.getPlugin().getLogger().warning("No item found for rarity roll.");
            }
        }
        return selectedItems;
    }

    // Method to roll for a single item based on rarity
    private ItemStack rollForItem() {
        int roll = random.nextInt(100);  // Roll for rarity
        Main mainPlugin = (Main) configManager.getPlugin();

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

        // Log the rolled rarity using the raw enum name (name())
        configManager.getPlugin().getLogger().info("Rolled rarity: " + selectedRarity.name());

        // Filter the items based on rarity (compare raw enum values)
        List<LootItem> filteredItems = mainPlugin.getLootPoolGUI().getLootPoolItems().stream()
                .filter(lootItem -> lootItem.getRarity() == selectedRarity)  // Compare the raw enum, not toString()
                .collect(Collectors.toList());

        if (!filteredItems.isEmpty()) {
            ItemStack selectedItem = filteredItems.get(random.nextInt(filteredItems.size())).getItem();
            configManager.getPlugin().getLogger().info("Selected item: " + selectedItem.getType() + " for rarity: " + selectedRarity.name());
            return selectedItem;
        } else {
            configManager.getPlugin().getLogger().warning("No items found for rarity: " + selectedRarity.name());
        }

        return null;  // No items of the selected rarity found
    }

    public void startChestRefillTask() {
        Main mainPlugin = (Main) configManager.getPlugin();
        mainPlugin.getServer().getScheduler().runTaskTimer(mainPlugin, this::refillChests, 0L, 600L);  // 600 ticks = 30 seconds
    }
}
