package com.coinseconomy.plugin.commands;

import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.gui.ShopGUI;
import com.coinseconomy.plugin.shop.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShopCommand implements CommandExecutor {

    private final EconomyManager economy;
    private final ShopManager shop;

    public ShopCommand(EconomyManager economy, ShopManager shop) {
        this.economy = economy;
        this.shop = shop;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem abrir a loja.");
            return true;
        }

        player.openInventory(ShopGUI.main(player, economy, shop));
        return true;
    }
}
