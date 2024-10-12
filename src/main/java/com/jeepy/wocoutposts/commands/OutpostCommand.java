package com.jeepy.wocoutposts.commands;

import com.jeepy.wocoutposts.Main;
import com.jeepy.wocoutposts.managers.ConfigManager;
import com.jeepy.wocoutposts.objectives.ClassifiedOutpost;
import com.jeepy.wocoutposts.objectives.Outpost;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class OutpostCommand implements CommandExecutor {
    private final Main plugin;
    private final int chargeTime;
    private final int refillInterval;
    private final ConfigManager configManager;
    private final Map<String, ClassifiedOutpost> activeClassifiedOutposts;

    public OutpostCommand(Main plugin, int chargeTime, int refillInterval, ConfigManager configManager) {
        this.plugin = plugin;
        this.chargeTime = chargeTime;
        this.refillInterval = refillInterval;
        this.configManager = configManager;
        this.activeClassifiedOutposts = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GRAY + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "Unknown command. Use /opost lootpool, /opost MarkC, /opost refill, /opost give, or /opost start.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "lootpool":
                plugin.getLootPoolGUI().open(player);
                return true;

            case "markc":
                return handleMarkC(player);

            case "refill":
                return handleRefill(player);

            case "give":
                return handleGive(player, args);

            case "start":
                return handleStart(player, args);

            case "stop":
                return handleStop(sender, args);

            case "reload":
                return handleReload(player);

            default:
                player.sendMessage(ChatColor.GRAY + "Unknown command. Use /opost lootpool, /opost MarkC, /opost refill, /opost give, or /opost start.");
                return true;
        }
    }

    private boolean handleMarkC(Player player) {
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage(ChatColor.GRAY + "You need to look at a chest to mark/unmark its location.");
            return true;
        }

        Location chestLocation = targetBlock.getLocation();
        try {
            List<Location> markedLocations = plugin.loadChestLocations();

            if (markedLocations.contains(chestLocation)) {
                plugin.deleteChestLocation(chestLocation);
                player.sendMessage(ChatColor.GREEN + "Chest location unmarked!");
            } else {
                plugin.saveChestLocation(chestLocation);
                player.sendMessage(ChatColor.GREEN + "Chest location marked!");
            }
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Failed to mark/unmark chest location.");
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleRefill(Player player) {
        plugin.getChestListener().refillChests();
        player.sendMessage(ChatColor.GREEN + "Marked chests have been refilled!");
        return true;
    }

    private boolean handleGive(Player player, String[] args) {
        // Check if the player provided an objective name
        if (args.length > 1) {
            String objectiveName = args[1];  // Use the second argument as the objective name

            // Create and give the beacon with the objective name
            giveClassifiedBeacon(player, objectiveName);
            return true;
        } else {
            player.sendMessage(ChatColor.GRAY + "Usage: /opost give <ObjectiveName>.");
            return true;
        }
    }

    private boolean handleStart(Player player, String[] args) {
        if (args.length == 2) {
            String outpostName = args[1];
            startOutpost(outpostName, player);  // Check if this is being called
            return true;
        } else {
            player.sendMessage(ChatColor.GRAY + "Please specify an outpost name. Usage: /opost start <OutpostName>");
            return true;
        }
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String outpostName = args[1].toLowerCase();  // Normalize case for consistency
            ClassifiedOutpost classifiedOutpost = plugin.getActiveClassifiedOutposts().get(outpostName);

            if (classifiedOutpost != null) {
                boolean success = classifiedOutpost.stop(outpostName);
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "The outpost " + outpostName + " has been stopped successfully.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to stop the outpost " + outpostName + ".");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "No active outpost found with the name: " + outpostName);
            }
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /opost stop <OutpostName>");
            return false;
        }
    }

    private boolean handleReload(Player player) {
        Main.getConfigManager().reloadConfig();  // Reload the config
        player.sendMessage(ChatColor.GREEN + "Woc-Outposts configuration reloaded!");
        return true;
    }

    private void startOutpost(String outpostName, Player player) {
        try {
            Outpost outpost = Main.getDatabaseManager().getOutpostByName(outpostName);

            if (outpost != null) {
                ClassifiedOutpost classifiedOutpost = new ClassifiedOutpost(
                        plugin,
                        Main.getDatabaseManager(),
                        configManager,
                        outpost.getLocation());  // Re-initialize the outpost with the proper location

                // Call the method to start the outpost
                classifiedOutpost.startClassifiedObjective(player, outpostName);

                player.sendMessage(ChatColor.GREEN + "Classified Outpost " + outpostName + " started.");
            } else {
                player.sendMessage(ChatColor.RED + "No Outpost found with the name: " + outpostName);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting the outpost", e);
            player.sendMessage(ChatColor.RED + "An error occurred while starting the outpost.");
        }
    }

    public void stopOutpost(String outpostName, CommandSender sender) {
        ClassifiedOutpost classifiedOutpost = activeClassifiedOutposts.get(outpostName.toLowerCase());  // Use lowercase consistently

        if (classifiedOutpost != null) {
            if (classifiedOutpost.stop(outpostName)) {
                activeClassifiedOutposts.remove(outpostName.toLowerCase());  // Ensure case consistency
                sender.sendMessage(ChatColor.GREEN + "The outpost " + outpostName + " has been stopped successfully.");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to stop the outpost " + outpostName + ". It may not be charging or active.");
                plugin.getLogger().warning("Failed to stop outpost " + outpostName + ": Outpost not charging or already inactive.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "The classified objective for outpost " + outpostName + " is not active.");
            plugin.getLogger().warning("Attempted to stop outpost " + outpostName + ", but it was not found in the active list.");
        }
    }


    // Modify giveClassifiedBeacon to accept the objective name and assign it to the beacon
    private void giveClassifiedBeacon(Player player, String objectiveName) {
        ItemStack beacon = new ItemStack(Material.BEACON);
        ItemMeta meta = beacon.getItemMeta();

        // Set the display name to the objective name (e.g., "Stronghold")
        meta.setDisplayName(objectiveName);
        beacon.setItemMeta(meta);

        // Add the beacon to the player's inventory
        player.getInventory().addItem(beacon);
        player.sendMessage(ChatColor.GRAY + "You have been given a Classified Beacon with the name: " + objectiveName);
    }
}
