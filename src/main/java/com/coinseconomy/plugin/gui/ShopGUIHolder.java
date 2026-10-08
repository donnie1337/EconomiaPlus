package com.coinseconomy.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ShopGUIHolder implements InventoryHolder {
    private final String categoryId;
    private final int page;
    private Inventory inventory;

    public ShopGUIHolder(String categoryId, int page) {
        this.categoryId = categoryId;
        this.page = Math.max(0, page);
    }

    public boolean isMain() {
        return categoryId == null;
    }

    public String getCategoryId() {
        return categoryId;
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
