package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.bank.BankManager;
import com.coinseconomy.plugin.bank.BankOperationInputListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class BankGUIListener implements Listener {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;
    private final BankManager banco;
    private final BankOperationInputListener input;

    public BankGUIListener(CoinsEconomyPlugin plugin, EconomyManager economia, BankManager banco,
                           BankOperationInputListener input) {
        this.plugin = plugin;
        this.economia = economia;
        this.banco = banco;
        this.input = input;
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
                    player.openInventory(BankGUI.operacoes(plugin, player, banco));
                } else if (slot == BankGUI.SLOT_HISTORICO) {
                    player.openInventory(BankGUI.historico());
                }
            }
            case OPERATIONS -> {
                if (slot == BankGUI.SLOT_VOLTAR) {
                    player.openInventory(BankGUI.principal(plugin, player, economia, banco));
                } else if (slot == BankGUI.SLOT_DEPOSITAR) {
                    input.start(player, BankOperationInputListener.Operation.DEPOSIT);
                } else if (slot == BankGUI.SLOT_SACAR) {
                    input.start(player, BankOperationInputListener.Operation.WITHDRAW);
                }
            }
            case HISTORY -> {
                if (slot == BankGUI.slotVoltarHistorico(event.getInventory().getSize())) {
                    player.openInventory(BankGUI.principal(plugin, player, economia, banco));
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
