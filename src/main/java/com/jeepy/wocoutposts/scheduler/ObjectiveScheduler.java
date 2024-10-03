package com.jeepy.wocoutposts.scheduler;

import com.jeepy.wocoutposts.managers.ObjectiveManager;
import com.jeepy.wocoutposts.Main;
import org.bukkit.scheduler.BukkitRunnable;

public class ObjectiveScheduler extends BukkitRunnable {

    private ObjectiveManager objectiveManager;
    private Main plugin;
    private long remainingTime; // Time left for the current objective in ticks

    public ObjectiveScheduler(ObjectiveManager objectiveManager, Main plugin) {
        this.objectiveManager = objectiveManager;
        this.plugin = plugin;
        loadRemainingTime();
    }

    @Override
    public void run() {
        // Activate a new objective every 6 hours
        if (!objectiveManager.isObjectiveActive()) {
            objectiveManager.startObjective();
        } else {
            remainingTime -= 20; // Decrease by 1 second (20 ticks)

            if (remainingTime <= 0) {
                objectiveManager.stopObjective();
                resetRemainingTime(); // Reset for the next cycle
            }
        }
    }

    // Load the remaining time from the config (use if server restarts during an objective)
    private void loadRemainingTime() {
        plugin.loadCustomConfig();  // Ensure the custom config is loaded
        remainingTime = plugin.getCustomConfig().getLong("objective.remainingTime", 72000);  // Default to 6 hours
    }

    // Reset the remaining time for the next objective
    private void resetRemainingTime() {
        remainingTime = 72000; // 6 hours in ticks
        plugin.getCustomConfig().set("objective.remainingTime", remainingTime);  // Save to config
        plugin.saveCustomConfig();  // Ensure the config is saved
    }

    // Optional: Save the current remaining time, e.g., on server shutdown
    public void saveRemainingTime() {
        plugin.getCustomConfig().set("objective.remainingTime", remainingTime);
        plugin.saveCustomConfig();
    }
}
