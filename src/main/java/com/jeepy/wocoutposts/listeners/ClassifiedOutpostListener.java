package com.jeepy.wocoutposts.listeners;

import com.jeepy.wocoutposts.objectives.ClassifiedOutpost;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.sql.SQLException;

public class ClassifiedOutpostListener implements Listener {

    private final ClassifiedOutpost classifiedOutpost;  // Reference to the outpost

    public ClassifiedOutpostListener(ClassifiedOutpost classifiedOutpost) {
        this.classifiedOutpost = classifiedOutpost;
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        try {
            classifiedOutpost.onPlayerKill(event);  // Delegate to the outpost class
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
