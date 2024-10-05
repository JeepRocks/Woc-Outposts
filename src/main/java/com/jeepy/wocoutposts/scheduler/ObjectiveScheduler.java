package com.jeepy.wocoutposts.scheduler;

import com.jeepy.wocoutposts.managers.ObjectiveManager;
import com.jeepy.wocoutposts.managers.ConfigManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ObjectiveScheduler extends BukkitRunnable {

    private final ObjectiveManager objectiveManager;
    private final ConfigManager configManager;
    private long remainingTime; // Time left for the current objective in ticks

    public ObjectiveScheduler(ObjectiveManager objectiveManager, ConfigManager configManager) {
        this.objectiveManager = objectiveManager;
        this.configManager = configManager;
        loadRemainingTime();
    }

    @Override
    public void run() {
        // Activate a new objective every 6 hours
        if (!objectiveManager.isObjectiveActive()) {
            objectiveManager.startObjective();
            configManager.getPlugin().getLogger().info("New objective started.");
        } else {
            remainingTime -= 20; // Decrease by 1 second (20 ticks)

            if (remainingTime <= 0) {
                objectiveManager.stopObjective();
                configManager.getPlugin().getLogger().info("Objective ended.");
                resetRemainingTime(); // Reset for the next cycle
            }
        }
    }

    // Load the remaining time from the config (useful if the server restarts during an objective)
    private void loadRemainingTime() {
        remainingTime = configManager.getCustomConfig().getLong("objective.remainingTime", 72000);  // Default to 6 hours
    }

    // Reset the remaining time for the next objective
    private void resetRemainingTime() {
        remainingTime = 72000; // 6 hours in ticks
        configManager.getCustomConfig().set("objective.remainingTime", remainingTime);  // Save to config
        configManager.saveCustomConfig();
    }

    // Optional: Save the current remaining time, e.g., on server shutdown
    public void saveRemainingTime() {
        configManager.getCustomConfig().set("objective.remainingTime", remainingTime);
        configManager.saveCustomConfig();
    }
}
