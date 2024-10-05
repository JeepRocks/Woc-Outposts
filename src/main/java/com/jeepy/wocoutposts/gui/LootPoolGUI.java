package com.jeepy.wocoutposts.gui;

import com.jeepy.wocoutposts.enums.Rarity;
import com.jeepy.wocoutposts.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LootPoolGUI implements Listener {

    private final ConfigManager configManager;
    private Inventory lootPoolInventory;
    private List<LootItem> lootPoolItems; // List of items with rarity

    public LootPoolGUI(ConfigManager configManager) {
        this.configManager = configManager;
        this.lootPoolItems = new ArrayList<>();
        createLootPoolGUI();
    }

    // Load loot pool items from custom config (WOC-Outposts.yml)
    private void createLootPoolGUI() {
        // Create or reset the lootPoolInventory
        if (lootPoolInventory == null) {
            lootPoolInventory = Bukkit.createInventory(null, 54, "Outpost Loot Pool");
        } else {
            lootPoolInventory.clear();  // Clear inventory to avoid duplicates
        }

        // Clear current loot pool items to prevent duplicates and reset the GUI
        lootPoolItems.clear();

        // Variables to track the count of items by rarity
        int commonCount = 0;
        int uncommonCount = 0;
        int rareCount = 0;
        int epicCount = 0;
        int legendaryCount = 0;

        // Load items from the custom config file (WOC-Outposts.yml)
        FileConfiguration config = configManager.getCustomConfig();
        if (config != null && config.contains("lootpool")) {
            for (String key : config.getConfigurationSection("lootpool").getKeys(false)) {
                String materialName = config.getString("lootpool." + key + ".material");
                Material material = Material.getMaterial(materialName);

                if (material == null) {
                    configManager.getPlugin().getLogger().warning("Invalid material in config: " + materialName);
                    continue;
                }

                int amount = config.getInt("lootpool." + key + ".amount", 1);

                String rarityString = config.getString("lootpool." + key + ".rarity");

                Rarity rarity;
                try {
                    rarity = Rarity.valueOf(rarityString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    configManager.getPlugin().getLogger().warning("Invalid rarity in config: " + rarityString);
                    continue;
                }

                // Debugging: Track item and rarity as they are loaded
                configManager.getPlugin().getLogger().info("Loading item: " + materialName + " with rarity: " + rarity.name());

                ItemStack itemStack = new ItemStack(material, amount);
                LootItem lootItem = new LootItem(itemStack, rarity);

                // Add the loot item to both the in-memory list and the inventory
                lootPoolItems.add(lootItem);
                lootPoolInventory.addItem(itemStack);

                // Increment the count for each rarity
                switch (rarity) {
                    case COMMON:
                        commonCount++;
                        break;
                    case UNCOMMON:
                        uncommonCount++;
                        break;
                    case RARE:
                        rareCount++;
                        break;
                    case EPIC:
                        epicCount++;
                        break;
                    case LEGENDARY:
                        legendaryCount++;
                        break;
                }
            }

            // Log the count of items per rarity after loading
            configManager.getPlugin().getLogger().info("Loot pool loaded with: " +
                    commonCount + " Common items, " +
                    uncommonCount + " Uncommon items, " +
                    rareCount + " Rare items, " +
                    epicCount + " Epic items, " +
                    legendaryCount + " Legendary items.");

        } else {
            // Debugging: If no items were loaded from the config
            configManager.getPlugin().getLogger().info("No items found in the lootpool configuration.");
        }
    }


    public void refreshLootPoolGUI(Player player) {
        createLootPoolGUI();  // Reload the inventory from the config and update GUI
        player.openInventory(lootPoolInventory);  // Open the refreshed GUI for the player
    }

    // Get the list of loot pool items
    public Inventory getLootPoolInventory() {
        return lootPoolInventory;
    }

    public List<LootItem> getLootPoolItems() {
        return lootPoolItems;
    }

    // Add an item to the loot pool and optionally save to the config
    public void addItemToLootPool(LootItem lootItem, boolean saveToConfig) {
        lootPoolItems.add(lootItem);  // Add the item to the list
        lootPoolInventory.addItem(lootItem.getItem());  // Add the item to the GUI

        if (saveToConfig) {
            saveItemToConfig(lootItem);  // Save to config if requested
        }
    }

    // Remove an item from the loot pool and optionally remove from custom config
    public void removeItemFromLootPool(ItemStack item) {
        lootPoolItems.removeIf(lootItem -> lootItem.getItem().equals(item));  // Remove from list
        lootPoolInventory.remove(item);  // Remove from GUI

        removeItemFromConfig(item);  // Remove from config
    }

    // Save an item to the custom config file (WOC-Outposts.yml)
    private void saveItemToConfig(LootItem lootItem) {
        FileConfiguration config = configManager.getCustomConfig();
        String path = "lootpool." + lootItem.getItem().getType().toString() + "_" + lootItem.getItem().getAmount();

        config.set(path + ".material", lootItem.getItem().getType().toString());
        config.set(path + ".amount", lootItem.getItem().getAmount());
        config.set(path + ".rarity", lootItem.getRarity().name());  // Use name() for raw enum value

        configManager.saveCustomConfig();  // Save the updated config
    }

    // Remove an item from the custom config file
    private void removeItemFromConfig(ItemStack item) {
        FileConfiguration config = configManager.getCustomConfig();
        String path = "lootpool." + item.getType().toString() + "_" + item.getAmount();
        config.set(path, null);
        configManager.saveCustomConfig();  // Save after removal
    }

    // Handle inventory clicks to prevent item movement in the Loot Pool GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Ensure only the LootPoolGUI is processed
        if (!event.getView().getTitle().equalsIgnoreCase("Outpost Loot Pool")) {
            return;  // If it's not the Loot Pool GUI, stop processing
        }

        refreshLootPoolGUI(player);  // Force GUI refresh when opened
        player.sendMessage("InventoryClickEvent triggered.");  // Debugging message

        ItemStack clickedItem = event.getCurrentItem();

        // Cancel movement within the Loot Pool GUI
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(lootPoolInventory)) {
            event.setCancelled(true);  // Cancel item movement in the GUI
            player.sendMessage("Loot Pool GUI clicked.");  // Debugging message

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                player.sendMessage("No valid item clicked in the Loot Pool.");  // Debugging message
                return;
            }

            // Remove item from the loot pool if clicked in GUI
            removeItemFromLootPool(clickedItem);
            player.sendMessage("Item removed from the loot pool.");

            // Refresh the GUI after removing the item
            refreshLootPoolGUI(player);
            return;
        }

        // Allow interactions with the player's inventory while the GUI is open
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(true);  // Cancel movement in the player's inventory, but allow adding to GUI
            player.sendMessage("Player Inventory clicked.");  // Debugging message

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                player.sendMessage("No valid item clicked in the Player Inventory.");  // Debugging message
                return;
            }

            // Debugging: Track item clicked
            player.sendMessage("Item clicked in inventory: " + clickedItem.getType());

            // Clone the clicked item and add it to the loot pool
            ItemStack itemToAdd = clickedItem.clone();  // Clone the clicked item
            player.sendMessage("Item cloned successfully: " + itemToAdd.getType());

            // Create a LootItem and add it to the loot pool
            LootItem lootItem = new LootItem(itemToAdd, Rarity.COMMON);  // Default rarity
            addItemToLootPool(lootItem, true);  // Add item to loot pool and config

            // Check if the item was added successfully
            if (lootPoolItems.contains(lootItem)) {
                player.sendMessage("Item successfully added to loot pool: " + lootItem.getItem().getType());
            } else {
                player.sendMessage("Item not added to loot pool.");
            }

            // Refresh the GUI after adding the item
            refreshLootPoolGUI(player);
        }
    }

    // Prevent dragging in the Loot Pool GUI
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase("Outpost Loot Pool")) {
            event.setCancelled(true);  // Cancel all drag interactions within the loot pool GUI
        }
    }
}