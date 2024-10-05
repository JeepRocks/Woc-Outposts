package com.jeepy.wocoutposts.commands;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashSet;

public class OutpostCommand implements CommandExecutor {

    private final ConfigManager configManager;

    public OutpostCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure there's at least one argument
        if (args.length == 0) {
            sender.sendMessage("Usage: /outpost <start|stop|lootpool|block|chest>");
            return true;
        }

        // Ensure sender is a player where needed
        if (!(sender instanceof Player) && (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("chest"))) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = sender instanceof Player ? (Player) sender : null;

        // Cast plugin to Main to access methods specific to your Main class
        Main mainPlugin = (Main) configManager.getPlugin();

        switch (args[0].toLowerCase()) {
            case "start":
                mainPlugin.getObjectiveManager().startObjective();
                sender.sendMessage("Objective started.");
                break;

            case "stop":
                mainPlugin.getObjectiveManager().stopObjective();
                sender.sendMessage("Objective stopped.");
                break;

            case "lootpool":
                if (player != null) {
                    player.openInventory(mainPlugin.getLootPoolGUI().getLootPoolInventory());
                }
                break;

            case "block":
                if (player != null) {
                    giveOutpostBlock(player);
                    player.sendMessage("You have received an Outpost Block!");
                }
                break;

            case "chest":
                if (player != null) {
                    markChest(player, mainPlugin);
                }
                break;

            default:
                sender.sendMessage("Invalid command. Use /outpost <start|stop|lootpool|block|chest>");
                break;
        }

        return true;
    }

    // Method to give a player the Outpost Block (beacon)
    private void giveOutpostBlock(Player player) {
        ItemStack outpostBlock = new ItemStack(Material.BEACON, 1);
        ItemMeta meta = outpostBlock.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("Outpost Block");
            meta.setLore(Collections.singletonList("Place this to create an outpost."));
            outpostBlock.setItemMeta(meta);
        }

        player.getInventory().addItem(outpostBlock);  // Add the custom beacon to the player's inventory
    }

    // Method to mark a chest for refilling
    private void markChest(Player player, Main mainPlugin) {
        // Use getTargetBlock with a null set for transparent materials in older versions
        Block block = player.getTargetBlock((HashSet<Byte>) null, 5);  // Target any block within 5 blocks

        if (block != null && block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();  // Get the chest state from the block
            mainPlugin.getChestManager().addChest(block.getLocation());  // Store chest location in ChestManager
            player.sendMessage("Chest marked for refilling during the objective.");
        } else {
            player.sendMessage("You need to look at a chest to mark it!");
        }
    }
}