package com.coinseconomy.plugin.commands;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.gui.TopCoinsGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /topcoins (aliases: /coinstop, /baltop)
 * Abre uma GUI mostrando os 10 jogadores com mais coins, com as cabeças
 * deles. Se executado pelo console, mostra o ranking em texto.
 */
public class TopCoinsCommand implements CommandExecutor {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public TopCoinsCommand(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coinseconomy.top")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (sender instanceof Player) {
            Inventory inventario = TopCoinsGUI.construir(economia);
            ((Player) sender).openInventory(inventario);
            return true;
        }

        exibirRankingEmTexto(sender);
        return true;
    }

    private void exibirRankingEmTexto(CommandSender sender) {
        List<Map.Entry<UUID, Double>> top = economia.getTop(10);

        sender.sendMessage(ChatColor.GOLD + "===== Top 10 - Coins =====");

        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Nenhum jogador possui conta ainda.");
            return;
        }

        int posicao = 1;
        for (Map.Entry<UUID, Double> entrada : top) {
            sender.sendMessage(ChatColor.YELLOW + "#" + posicao + " " + ChatColor.GRAY +
                    economia.getNomeConhecido(entrada.getKey()) + ChatColor.DARK_GRAY + " - " +
                    ChatColor.GREEN + economia.formatar(entrada.getValue()));
            posicao++;
        }
    }
}
