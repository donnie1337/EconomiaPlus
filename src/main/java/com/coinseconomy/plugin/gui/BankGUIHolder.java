package com.coinseconomy.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BankGUIHolder implements InventoryHolder {
    public enum Screen { MAIN, OPERATIONS, HISTORY }

    private final Screen screen;
    private final int page;
    private Inventory inventory;

    public BankGUIHolder(Screen screen) {
        this(screen, 0);
    }

    public BankGUIHolder(Screen screen, int page) {
        this.screen = screen;
        this.page = Math.max(0, page);
    }

    public Screen getScreen() { return screen; }
    public int getPage() { return page; }

    @Override public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
