package com.coinseconomy.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marca as instâncias de Inventory que pertencem à GUI do /topcoins, para
 * que o listener saiba quando deve bloquear cliques/arrastos.
 */
public class TopCoinsGUIHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
