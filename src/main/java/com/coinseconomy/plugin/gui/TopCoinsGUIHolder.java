package com.coinseconomy.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Guarda a página atual do ranking de Coins. */
public final class TopCoinsGUIHolder implements InventoryHolder {

    private final int page;
    private Inventory inventory;

    public TopCoinsGUIHolder(int page) {
        this.page = Math.max(0, page);
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
