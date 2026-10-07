package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Navegação da GUI paginada de Top jogadores. */
public final class TopCoinsGUIListener implements Listener {

    private final EconomyManager economia;

    public TopCoinsGUIListener(EconomyManager economia) {
        this.economia = economia;
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent evento) {
        if (!(evento.getInventory().getHolder() instanceof TopCoinsGUIHolder holder)) {
            return;
        }

        evento.setCancelled(true);
        if (!(evento.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = evento.getRawSlot();
        if (slot < 0 || slot >= evento.getInventory().getSize()) {
            return;
        }

        if (slot == TopCoinsGUI.SLOT_ANTERIOR && holder.getPage() > 0) {
            player.openInventory(TopCoinsGUI.construir(economia, holder.getPage() - 1));
            return;
        }

        if (slot == TopCoinsGUI.SLOT_PROXIMA) {
            player.openInventory(TopCoinsGUI.construir(economia, holder.getPage() + 1));
            return;
        }

        if (slot == TopCoinsGUI.SLOT_VOLTAR) {
            player.openInventory(CoinsWalletGUI.construir(player, economia));
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent evento) {
        if (evento.getInventory().getHolder() instanceof TopCoinsGUIHolder) {
            evento.setCancelled(true);
        }
    }
}
