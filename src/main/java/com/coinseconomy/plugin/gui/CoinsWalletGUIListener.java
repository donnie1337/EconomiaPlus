package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Controla os cliques da carteira aberta por /coins. */
public final class CoinsWalletGUIListener implements Listener {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public CoinsWalletGUIListener(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CoinsWalletGUIHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        switch (slot) {
            case CoinsWalletGUI.SLOT_SALDO -> {
                // Atualiza o menu para refletir qualquer alteração recente no saldo.
                player.openInventory(CoinsWalletGUI.construir(player, economia));
            }
            case CoinsWalletGUI.SLOT_PAGAR -> {
                player.closeInventory();
                player.sendMessage(color("&a[Coins] &fUse &a/coins pagar <jogador> <quantidade> &fpara enviar Coins."));
            }
            case CoinsWalletGUI.SLOT_RANKING ->
                    player.openInventory(TopCoinsGUI.construir(economia));
            case CoinsWalletGUI.SLOT_AJUDA -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(
                        plugin,
                        () -> player.performCommand("coins ajuda")
                );
            }
            case CoinsWalletGUI.SLOT_FECHAR -> player.closeInventory();
            default -> {
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CoinsWalletGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
