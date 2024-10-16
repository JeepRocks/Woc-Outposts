package com.jeepy.wocoutposts.objectives;

import org.bukkit.Location;

public abstract class Outpost {

    protected String outpostName;

    public Outpost(String outpostName, Location beaconLocation) {
        this.outpostName = outpostName;
    }

    public String getOutpostName() {
        return outpostName;
    }

    // Abstract methods that subclasses must implement
    public abstract void startCharging();

    public abstract void stopCharging();

    public abstract void refillLoot();
}