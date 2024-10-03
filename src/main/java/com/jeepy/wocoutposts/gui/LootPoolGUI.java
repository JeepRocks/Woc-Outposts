package com.jeepy.wocoutposts.gui;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.enums.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootPoolGUI implements Listener {

    private Main plugin;
    private Inventory lootPoolInventory;
    private List<LootItem> lootPoolItems; // List of items with rarity

    public LootPoolGUI(Main plugin) {
        this.plugin = plugin;
        this.lootPoolItems = new ArrayList<>();
        createLootPoolGUI();
    }

    // Load loot pool items from custom config (WOC-Outposts.yml)
    private void createLootPoolGUI() {
        lootPoolInventory = Bukkit.createInventory(null, 54, "Outpost Loot Pool");

        // Load items from the custom config file (WOC-Outposts.yml)
        FileConfiguration config = plugin.getCustomConfig();
        if (config != null && config.contains("lootpool")) {
            for (String key : config.getConfigurationSection("lootpool").getKeys(false)) {
                String materialName = config.getString("lootpool." + key + ".material");
                Material material = Material.getMaterial(materialName);

                // Ensure the material is valid
                if (material == null) {
                    plugin.getLogger().warning("Invalid material in config: " + materialName);
                    continue; // Skip invalid materials
                }

                int amount = config.getInt("lootpool." + key + ".amount", 1);
                String rarityString = config.getString("lootpool." + key + ".rarity");

                // Ensure rarity is valid
                Rarity rarity;
                try {
                    rarity = Rarity.valueOf(rarityString);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid rarity in config: " + rarityString);
                    continue; // Skip invalid rarity
                }

                // Add items back to the GUI and the loot pool, ignoring empty slots
                if (amount > 0) {
                    ItemStack item = new ItemStack(material, amount);
                    addItemToLootPool(item, rarity, false); // Load from config without saving back
                }
            }
        } else {
            plugin.getLogger().warning("No items found in the loot pool.");  // Handle case when lootpool section is empty
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Inventory getLootPoolInventory() {
        return lootPoolInventory;
    }

    public List<LootItem> getLootPoolItems() {
        return lootPoolItems;
    }

    // Method to add an item with a rarity and save it to the custom config if needed
    public void addItemToLootPool(ItemStack item, Rarity rarity, boolean saveToConfig) {
        // Prevent duplicate entries
        if (lootPoolItems.stream().anyMatch(lootItem -> lootItem.getItem().equals(item))) {
            plugin.getLogger().info("Item already exists in the loot pool.");
            return; // Do not add duplicate items
        }

        LootItem lootItem = new LootItem(item, rarity);
        lootPoolItems.add(lootItem);
        lootPoolInventory.addItem(item);  // Add to the GUI as well

        // Save to custom config if necessary
        if (saveToConfig) {
            saveItemToConfig(lootItem);
        }
    }

    // Remove an item from the loot pool and optionally remove from custom config
    public void removeItemFromLootPool(ItemStack item) {
        lootPoolItems.removeIf(lootItem -> lootItem.getItem().equals(item));
        lootPoolInventory.remove(item);  // Remove from the GUI as well

        // Remove from custom config
        removeItemFromConfig(item);
    }

    // Save an item to the custom config file (WOC-Outposts.yml)
    private void saveItemToConfig(LootItem lootItem) {
        FileConfiguration config = plugin.getCustomConfig();  // Ensure using the correct custom config
        String path = "lootpool." + lootItem.getItem().getType().toString() + "_" + lootItem.getItem().getAmount();

        // Save the item's material, amount, and rarity
        config.set(path + ".material", lootItem.getItem().getType().toString());
        config.set(path + ".amount", lootItem.getItem().getAmount());
        config.set(path + ".rarity", lootItem.getRarity().toString());

        // Save the custom config to disk
        plugin.saveCustomConfig();  // Make sure this saves to `WOC-Outposts.yml`
    }

    // Remove an item from the custom config file
    private void removeItemFromConfig(ItemStack item) {
        FileConfiguration config = plugin.getCustomConfig();
        String path = "lootpool." + item.getType().toString() + "_" + item.getAmount();
        config.set(path, null);  // Remove the item from the config
        plugin.saveCustomConfig();  // Save the changes
    }

    // Event handler for inventory clicks (if necessary)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase("Outpost Loot Pool")) {
            event.setCancelled(true); // Prevent item removal from the GUI
            // Additional handling logic for inventory clicks
        }
    }
}