package com.jeepy.wocoutposts.listeners;

import com.jeepy.wocoutposts.managers.LootPoolManager;
import com.jeepy.wocoutposts.gui.LootPoolGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class LootPoolGUIListener implements Listener {
    private final LootPoolManager lootPoolManager;
    private final LootPoolGUI lootPoolGUI;
    private final Plugin plugin;
    private ItemStack itemToAddOrEdit;

    public LootPoolGUIListener(LootPoolManager lootPoolManager, LootPoolGUI lootPoolGUI, Plugin plugin) {
        this.lootPoolManager = lootPoolManager;
        this.lootPoolGUI = lootPoolGUI;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();

        // If the click happens inside the custom loot pool GUI
        if (event.getView().getTitle().equals("Loot Pool")) {
            event.setCancelled(true); // Prevent normal item movement in the custom GUI

            if (event.isLeftClick()) {
                itemToAddOrEdit = clickedItem;
                player.closeInventory();
                player.sendMessage("Type the new rarity for the item in chat (common, uncommon, rare, epic, legendary):");
                plugin.getServer().getPluginManager().registerEvents(new ChatListener(), plugin);
            } else if (event.isRightClick()) {
                handleRemoveItem(clickedItem);
            }
        } else if (event.getInventory().equals(player.getInventory())) {
            // Prevent adding items to the loot pool when not interacting with the LootPoolGUI
            if (itemToAddOrEdit != null) {
                player.sendMessage("You must be inside the Loot Pool GUI to add items.");
                itemToAddOrEdit = null;
            }
        }
    }

    private void handleAddItemToGUI(Player player, int slot) {
        itemToAddOrEdit = player.getInventory().getItem(slot);
        if (itemToAddOrEdit != null && itemToAddOrEdit.getType() != Material.AIR) {
            player.sendMessage("Type the rarity for the item in chat (common, uncommon, rare, epic, legendary):");
            plugin.getServer().getPluginManager().registerEvents(new ChatListener(), plugin);
        }
    }

    private void handleChangeRarity(Player player, ItemStack item) {
        itemToAddOrEdit = item;
        player.sendMessage("Type the new rarity for the item in chat (common, uncommon, rare, epic, legendary):");
        plugin.getServer().getPluginManager().registerEvents(new ChatListener(), plugin);
    }

    private void handleRemoveItem(ItemStack item) {
        lootPoolGUI.getInventory().remove(item);
        lootPoolManager.removeItem(item);
        plugin.getLogger().info("Removed item from loot pool: " + item);
    }

    // Inner class to handle chat events
    private class ChatListener implements Listener {
        @EventHandler
        public void onPlayerChat(AsyncPlayerChatEvent chatEvent) {
            Player player = chatEvent.getPlayer();
            String message = chatEvent.getMessage().toLowerCase();
            chatEvent.setCancelled(true);

            if (Arrays.asList("common", "uncommon", "rare", "epic", "legendary").contains(message)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String rarity = message;
                        if (itemToAddOrEdit != null) {
                            if (lootPoolGUI.getInventory().contains(itemToAddOrEdit)) {
                                itemToAddOrEdit.setItemMeta(lootPoolGUI.addItemLore(itemToAddOrEdit, rarity).getItemMeta());
                                lootPoolManager.updateItemRarity(itemToAddOrEdit, rarity);
                                plugin.getLogger().info("Updated item rarity in loot pool: " + itemToAddOrEdit);
                            } else {
                                itemToAddOrEdit.setItemMeta(lootPoolGUI.addItemLore(itemToAddOrEdit, rarity).getItemMeta());
                                lootPoolManager.addItem(itemToAddOrEdit, rarity);
                                lootPoolGUI.getInventory().addItem(itemToAddOrEdit);
                                plugin.getLogger().info("Added item to loot pool: " + itemToAddOrEdit);
                            }
                        }
                        player.openInventory(lootPoolGUI.getInventory()); // Re-open the GUI and reset itemToAddOrEdit
                        itemToAddOrEdit = null;
                    }
                }.runTask(plugin);
            } else {
                player.sendMessage("Invalid rarity. Please type one of (common, uncommon, rare, epic, legendary):");
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
            }
            AsyncPlayerChatEvent.getHandlerList().unregister(this);
        }
    }
}