package com.jeepy.wocoutposts.commands;

import com.jeepy.wocoutposts.Main;
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
import java.util.Set;

public class OutpostCommand implements CommandExecutor {

    private Main plugin;

    public OutpostCommand(Main plugin) {
        this.plugin = plugin;
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

        switch (args[0].toLowerCase()) {
            case "start":
                plugin.getObjectiveManager().startObjective();
                sender.sendMessage("Objective started.");
                break;

            case "stop":
                plugin.getObjectiveManager().stopObjective();
                sender.sendMessage("Objective stopped.");
                break;

            case "lootpool":
                if (player != null) {
                    player.openInventory(plugin.getLootPoolGUI().getLootPoolInventory());
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
                    markChest(player);
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
    private void markChest(Player player) {
        // Use getTargetBlock with a null set for transparent materials in older versions
        Block block = player.getTargetBlock((HashSet<Byte>) null, 5);  // Target any block within 5 blocks

        if (block != null && block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();  // Get the chest state from the block
            plugin.getChestManager().addChest(block.getLocation());  // Store chest location in ChestManager
            player.sendMessage("Chest marked for refilling during the objective.");
        } else {
            player.sendMessage("You need to look at a chest to mark it!");
        }
    }

}
