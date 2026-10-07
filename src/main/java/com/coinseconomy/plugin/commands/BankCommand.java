package com.coinseconomy.plugin.commands;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.bank.BankManager;
import com.coinseconomy.plugin.gui.BankGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BankCommand implements CommandExecutor {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;
    private final BankManager banco;

    public BankCommand(CoinsEconomyPlugin plugin, EconomyManager economia, BankManager banco) {
        this.plugin = plugin;
        this.economia = economia;
        this.banco = banco;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem abrir o banco.");
            return true;
        }

        economia.criarConta(player);
        player.openInventory(BankGUI.principal(plugin, player, economia, banco));
        return true;
    }
}
