package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class BankGUIListener implements Listener {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public BankGUIListener(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BankGUIHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getScreen()) {
            case MAIN -> {
                if (slot == BankGUI.SLOT_OPERACOES) {
                    player.openInventory(BankGUI.operacoes(plugin));
                } else if (slot == BankGUI.SLOT_HISTORICO) {
                    player.openInventory(BankGUI.historico());
                }
            }
            case OPERATIONS -> {
                if (slot == BankGUI.SLOT_VOLTAR) {
                    player.openInventory(BankGUI.principal(plugin, player, economia));
                } else if (slot == BankGUI.SLOT_DEPOSITAR) {
                    player.sendMessage(color("&a[Banco] &fO módulo de depósito será conectado ao saldo bancário persistente."));
                } else if (slot == BankGUI.SLOT_SACAR) {
                    player.sendMessage(color("&a[Banco] &fO módulo de saque será conectado ao saldo bancário persistente."));
                }
            }
            case HISTORY -> {
                if (slot == BankGUI.slotVoltarHistorico(event.getInventory().getSize())) {
                    player.openInventory(BankGUI.principal(plugin, player, economia));
                }
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BankGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
