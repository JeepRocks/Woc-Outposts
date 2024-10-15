package com.jeepy.wocoutposts.commands;

import com.jeepy.wocoutposts.managers.OutpostManager;
import com.jeepy.wocoutposts.managers.LootPoolManager;
import com.jeepy.wocoutposts.objectives.ClassifiedOutpost;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class OutpostCommand implements CommandExecutor {

    private final OutpostManager outpostManager;
    private final LootPoolManager lootPoolManager;

    public OutpostCommand(OutpostManager outpostManager, LootPoolManager lootPoolManager) {
        this.outpostManager = outpostManager;
        this.lootPoolManager = lootPoolManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("outpost")) {
            if (args.length < 1) {
                player.sendMessage("Usage: /outpost <start|stop|create|delete|refill|debug>");
                return true;
            }

            String subCommand = args[0];

            switch (subCommand.toLowerCase()) {
                case "start":
                    // Start charging an outpost
                    if (args.length == 2) {
                        String outpostName = args[1];
                        outpostManager.startOutpost(player, outpostName);
                    } else {
                        player.sendMessage("Usage: /outpost start [name]");
                    }
                    break;

                case "stop":
                    // Stop charging an outpost
                    if (args.length == 2) {
                        String outpostName = args[1];
                        outpostManager.stopOutpost(player, outpostName);
                    } else {
                        player.sendMessage("Usage: /outpost stop [name]");
                    }
                    break;

                case "create":
                    // Create a new outpost
                    if (args.length == 2) {
                        String outpostName = args[1];
                        outpostManager.createOutpost(player, outpostName);
                    } else {
                        player.sendMessage("Usage: /outpost create [name]");
                    }
                    break;

                case "delete":
                    // Delete an existing outpost
                    if (args.length == 2) {
                        String outpostName = args[1];
                        outpostManager.deleteOutpost(player, outpostName);
                    } else {
                        player.sendMessage("Usage: /outpost delete [name]");
                    }
                    break;

                case "refill":
                    // Ensure the correct number of arguments are provided
                    if (args.length != 2) {
                        player.sendMessage("Usage: /outpost refill <outpostName>");
                        break;
                    }

                    // Retrieve the outpost name from the arguments
                    String outpostName = args[1];

                    // Use the existing outpostManager to refill the loot for the specified outpost
                    outpostManager.refillLoot(player, outpostName);
                    break;

                case "debug":
                    // Enable debug mode
                    outpostManager.toggleDebug(player);
                    break;

                default:
                    player.sendMessage("Unknown command. Use: /outpost <start|stop|create|delete|refill|debug>");
                    break;
            }
            return true;
        }

        // Lootpool commands
        if (command.getName().equalsIgnoreCase("lootpool")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /lootpool <additem|removeitem|view>");
                return true;
            }

            String subCommand = args[0];
            String outpostName = args[1];

            switch (subCommand.toLowerCase()) {
                case "additem":
                    // Add item to loot pool
                    if (args.length >= 4) {
                        String itemId = args[2];
                        String rarity = args[3];
                        Double chance = null; // Default to null if no chance is provided

                        // Check if the player provided a chance argument
                        if (args.length == 5) {
                            try {
                                chance = Double.parseDouble(args[4]);
                            } catch (NumberFormatException e) {
                                player.sendMessage("Invalid chance value. It must be a number between 1 and 100.");
                                return true;
                            }

                            // Ensure chance is valid (between 1 and 100)
                            if (chance < 1 || chance > 100) {
                                player.sendMessage("Invalid chance. Please enter a value between 1 and 100.");
                                return true;
                            }
                        }

                        ItemStack itemInHand = player.getItemInHand();
                        if (itemInHand != null) {
                            lootPoolManager.addItemToLootPool(player, outpostName, itemInHand, itemId, rarity, chance);
                        } else {
                            player.sendMessage("You need to hold an item in your hand.");
                        }
                    } else {
                        player.sendMessage("Usage: /lootpool additem [outpost] [item id] [rarity] [chance%]");
                    }
                    break;

                case "removeitem":
                    // Remove item from lootpool
                    if (args.length == 3) {
                        String itemId = args[2];
                        lootPoolManager.removeItemFromLootPool(outpostName, itemId);
                        player.sendMessage("Item removed from loot pool for outpost: " + outpostName);
                    } else {
                        player.sendMessage("Usage: /lootpool removeitem [outpost] [item id]");
                    }
                    break;

                case "view":
                    // View the loot pool for the outpost
                    lootPoolManager.viewLootPool(player, outpostName);
                    break;

                default:
                    player.sendMessage("Unknown lootpool command. Use: /lootpool <additem|removeitem|view>");
                    break;
            }
            return true;
        }

        return false;
    }
}