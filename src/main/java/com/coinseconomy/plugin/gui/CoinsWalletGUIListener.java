package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Controla o hub /coins e o submenu de transações. */
public final class CoinsWalletGUIListener implements Listener {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public CoinsWalletGUIListener(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof CoinsWalletGUIHolder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;

            switch (slot) {
                case CoinsWalletGUI.SLOT_INFORMACOES ->
                        player.sendMessage(color("&a[Coins] &fO controle de recebimento será conectado ao ledger de transações."));
                case CoinsWalletGUI.SLOT_TRANSACOES ->
                        player.openInventory(CoinsTransactionsGUI.construir(player, plugin.getWalletTransactionManager()));
                case CoinsWalletGUI.SLOT_TOP ->
                        player.openInventory(TopCoinsGUI.construir(economia));
                case CoinsWalletGUI.SLOT_MAGNATA -> {
                    // Item informativo.
                }
                default -> {
                }
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof CoinsTransactionsGUIHolder holder) {
            event.setCancelled(true);
            int size = event.getInventory().getSize();
            int slot = event.getRawSlot();

            if (slot == CoinsTransactionsGUI.slotVoltar(size)) {
                player.openInventory(CoinsWalletGUI.construir(player, economia));
            } else if (slot == CoinsTransactionsGUI.slotAnterior(size) && holder.getPage() > 0) {
                player.openInventory(CoinsTransactionsGUI.construir(
                        player, plugin.getWalletTransactionManager(), holder.getPage() - 1
                ));
            } else if (slot == CoinsTransactionsGUI.slotProxima(size)) {
                player.openInventory(CoinsTransactionsGUI.construir(
                        player, plugin.getWalletTransactionManager(), holder.getPage() + 1
                ));
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CoinsWalletGUIHolder
                || event.getInventory().getHolder() instanceof CoinsTransactionsGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
