package com.jeepy.wocoutposts.gui;

import org.bukkit.inventory.ItemStack;
import com.jeepy.wocoutposts.enums.Rarity;

import java.util.Objects;

public class LootItem {

    private final ItemStack item;
    private final Rarity rarity;

    public LootItem(ItemStack item, Rarity rarity) {
        this.item = item;
        this.rarity = rarity;
    }

    public ItemStack getItem() {
        return item;
    }

    public Rarity getRarity() {
        return rarity;
    }

    // Override equals to compare LootItem objects based on item and rarity
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LootItem lootItem = (LootItem) o;
        return Objects.equals(item, lootItem.item) && rarity == lootItem.rarity;
    }

    // Override hashCode to maintain consistency with equals
    @Override
    public int hashCode() {
        return Objects.hash(item, rarity);
    }

    // Override toString for better logging and debugging
    @Override
    public String toString() {
        return "LootItem{" +
                "item=" + item +
                ", rarity=" + rarity +
                '}';
    }
}
