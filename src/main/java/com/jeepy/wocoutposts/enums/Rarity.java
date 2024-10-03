package com.jeepy.wocoutposts.enums;

public enum Rarity {
    COMMON(60),     // Higher chance
    UNCOMMON(25),   // Medium chance
    RARE(10),       // Lower chance
    EPIC(5),        // Very low chance
    LEGENDARY(1);   // Very very low chance

    private final int weight;

    Rarity(int weight) {
        this.weight = weight;
    }

    // Get the weight for the rarity level (used for RNG probability)
    public int getWeight() {
        return weight;
    }

    // Override toString for better logging or display purposes
    @Override
    public String toString() {
        switch (this) {
            case COMMON:
                return "Common (60%)";
            case UNCOMMON:
                return "Uncommon (25%)";
            case RARE:
                return "Rare (10%)";
            case EPIC:
                return "Epic (5%)";
            case LEGENDARY:
                return "Legendary (1%)";
            default:
                return "Unknown Rarity";
        }
    }
}