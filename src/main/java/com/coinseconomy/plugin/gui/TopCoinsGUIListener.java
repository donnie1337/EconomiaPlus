package com.coinseconomy.plugin.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Impede que jogadores retirem, movam ou arrastem os itens (cabeças/vidros)
 * da GUI do /topcoins - ela é apenas para visualização.
 */
public class TopCoinsGUIListener implements Listener {

    @EventHandler
    public void aoClicar(InventoryClickEvent evento) {
        if (evento.getInventory().getHolder() instanceof TopCoinsGUIHolder) {
            evento.setCancelled(true);
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent evento) {
        if (evento.getInventory().getHolder() instanceof TopCoinsGUIHolder) {
            evento.setCancelled(true);
        }
    }
}
