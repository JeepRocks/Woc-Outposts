package com.jeepy.wocoutposts.objectives;

import org.bukkit.Location;

public class Outpost {
    private final String name;
    private final Location location;

    public Outpost(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }
}
