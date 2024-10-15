package com.jeepy.wocoutposts.managers;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LootPoolManager {

    private static final int DEFAULT_INVENTORY_SIZE = 27;

    // Map to store loot pools for different outposts (outpostName -> loot pool)
    private Map<String, Map<String, LootItem>> outpostLootPools = new ConcurrentHashMap<>();

    // Define valid rarities
    private final List<String> validRarities = Arrays.asList("common", "rare", "epic", "legendary");

    // Default chances based on rarity
    private final Map<String, Double> rarityChances = new ConcurrentHashMap<String, Double>() {{
        put("common", 50.0);
        put("rare", 30.0);
        put("epic", 15.0);
        put("legendary", 5.0);
    }};

    // Immutable class to represent a loot item with rarity and drop chance
    class LootItem {
        final ItemStack item;
        final String rarity;
        final double dropChance;

        LootItem(ItemStack item, String rarity, double dropChance) {
            this.item = item;
            this.rarity = rarity;
            this.dropChance = dropChance;
        }
    }

    public void createLootPool(String outpostName) {
        if (!outpostLootPools.containsKey(outpostName)) {
            outpostLootPools.put(outpostName, new ConcurrentHashMap<>());
        }
    }

    // Method to delete a loot pool when an outpost is deleted
    public void deleteLootPool(String outpostName) {
        outpostLootPools.remove(outpostName);
    }

    // Utility method to create a custom ItemStack
    private ItemStack customizeItemStack(ItemStack item, String itemId, String rarity, double dropChance) {
        ItemStack itemStack = item.clone();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("Loot ID: " + itemId);
            List<String> lore = new ArrayList<>();
            lore.add("Rarity: " + rarity);
            lore.add("Drop Chance: " + dropChance + "%");
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    // Add an item to the loot pool for the specified outpost with validation
    public void addItemToLootPool(Player player, String outpostName, ItemStack item, String itemId, String rarity, Double chance) {
        if (!outpostLootPools.containsKey(outpostName)) {
            player.sendMessage("Outpost '" + outpostName + "' does not exist.");
            return;
        }

        if (!validRarities.contains(rarity.toLowerCase())) {
            player.sendMessage("Invalid rarity. Valid rarities are: " + String.join(", ", validRarities));
            return;
        }

        if (chance == null) {
            chance = rarityChances.getOrDefault(rarity.toLowerCase(), 100.0);
        }

        if (chance < 1 || chance > 100) {
            player.sendMessage("Invalid loot chance. It must be between 1% and 100%.");
            return;
        }

        Map<String, LootItem> lootPool = outpostLootPools.get(outpostName);
        lootPool.put(itemId, new LootItem(item, rarity, chance));
        player.sendMessage("Item '" + itemId + "' added to the loot pool for outpost '" + outpostName + "' with rarity '" + rarity + "' and drop chance '" + chance + "%'.");
    }

    // Remove an item from the loot pool for the specified outpost
    public void removeItemFromLootPool(String outpostName, String itemId) {
        Map<String, LootItem> lootPool = outpostLootPools.get(outpostName);
        if (lootPool != null) {
            lootPool.remove(itemId);
        }
    }

    // View the loot pool as a GUI for the specified outpost
    public void viewLootPool(Player player, String outpostName) {
        Map<String, LootItem> lootPool = outpostLootPools.get(outpostName);

        if (lootPool == null) {
            player.sendMessage("No loot pool found for outpost: " + outpostName);
        } else {
            int size = Math.min(54, ((lootPool.size() - 1) / 9 + 1) * 9);
            Inventory lootInventory = Bukkit.createInventory(null, size, "Loot Pool: " + outpostName);

            for (Map.Entry<String, LootItem> entry : lootPool.entrySet()) {
                String itemId = entry.getKey();
                LootItem lootItem = entry.getValue();

                lootInventory.addItem(customizeItemStack(lootItem.item, itemId, lootItem.rarity, lootItem.dropChance));
            }

            player.openInventory(lootInventory);
        }
    }
}