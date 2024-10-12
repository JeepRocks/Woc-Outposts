package com.jeepy.wocoutposts.gui;

import com.jeepy.wocoutposts.managers.LootPoolManager;
import com.jeepy.wocoutposts.listeners.ChestListener; // Import ChestListener
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Map;

public class LootPoolGUI {
    private final Inventory lootPoolInventory;
    private final LootPoolManager lootPoolManager;
    private final Plugin plugin;
    private final ChestListener chestListener; // Declare chestListener

    public LootPoolGUI(LootPoolManager lootPoolManager, Plugin plugin, ChestListener chestListener) {
        this.lootPoolManager = lootPoolManager;
        this.plugin = plugin;
        this.chestListener = chestListener; // Initialize chestListener
        this.lootPoolInventory = Bukkit.createInventory(null, 54, "Loot Pool");
        loadItemsFromManager();
    }

    public Inventory getInventory() {
        return lootPoolInventory;
    }

    public void open(Player player) {
        player.openInventory(lootPoolInventory);
    }

    private void loadItemsFromManager() {
        lootPoolInventory.clear();
        for (Map.Entry<ItemStack, String> entry : lootPoolManager.getLootPool().entrySet()) {
            ItemStack item = addItemLore(entry.getKey(), entry.getValue());
            lootPoolInventory.addItem(item);
        }
    }

    public ItemStack addItemLore(ItemStack item, String rarity) {
        ItemMeta meta = item.getItemMeta();
        ChatColor color = getRarityColor(rarity);
        double chance = chestListener.getRarityChance(rarity); // Use ChestListener's method
        meta.setLore(Arrays.asList(color + rarity, ChatColor.WHITE + "Chance: " + (chance * 100) + "%"));
        item.setItemMeta(meta);
        return item;
    }

    private ChatColor getRarityColor(String rarity) {
        switch (rarity.toLowerCase()) {
            case "common":    return ChatColor.GRAY;
            case "uncommon":  return ChatColor.GREEN;
            case "rare":      return ChatColor.BLUE;
            case "epic":      return ChatColor.DARK_PURPLE;
            case "legendary": return ChatColor.GOLD;
            default:          return ChatColor.WHITE;
        }
    }
}