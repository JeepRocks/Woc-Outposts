// LootPoolManager.java
package com.jeepy.wocoutposts.managers;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class LootPoolManager {
    private final ConfigManager configManager;
    private Map<ItemStack, String> lootPool;

    public LootPoolManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.lootPool = new HashMap<>();
        loadLootPool();
    }

    // Load the loot pool from the config
    public void loadLootPool() {
        this.lootPool = configManager.loadItemsFromConfig();
    }

    // Get the current loot pool
    public Map<ItemStack, String> getLootPool() {
        return lootPool;
    }

    // Add item to the loot pool and update the config
    public void addItem(ItemStack itemStack, String rarity) {
        lootPool.put(itemStack, rarity);
        configManager.addItemToConfig(itemStack, rarity);
    }

    // Remove item from the loot pool and update the config
    public void removeItem(ItemStack itemStack) {
        lootPool.remove(itemStack);
        configManager.removeItemFromConfig(itemStack);
    }

    // Update the rarity of an item in the loot pool and in the config
    public void updateItemRarity(ItemStack itemStack, String rarity) {
        lootPool.put(itemStack, rarity);
        configManager.updateItemRarityConfig(itemStack, rarity);
    }
}