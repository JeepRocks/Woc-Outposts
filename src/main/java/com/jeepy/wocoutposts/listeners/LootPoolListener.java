package com.jeepy.wocoutposts.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.ChatColor;

public class LootPoolListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();

        // Check if this is the loot pool GUI by checking the inventory title
        if (topInventory != null && view.getTitle().contains("Loot Pool")) {
            // Cancel the event to prevent item movement
            event.setCancelled(true);

        }
    }
}
