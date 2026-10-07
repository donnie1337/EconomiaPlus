package com.coinseconomy.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BankGUIHolder implements InventoryHolder {
    public enum Screen { MAIN, OPERATIONS, HISTORY }
    private final Screen screen;
    private Inventory inventory;

    public BankGUIHolder(Screen screen) { this.screen = screen; }
    public Screen getScreen() { return screen; }
    @Override public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
