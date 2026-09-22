package com.coinseconomy.plugin.commands;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /cobrar <jogador> <quantidade>
 * Comando administrativo: remove (cobra) uma quantidade de coins do saldo
 * do jogador alvo. Por padrão, exige permissão de operador
 * (coinseconomy.cobrar).
 */
public class CobrarCommand implements CommandExecutor, TabCompleter {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public CobrarCommand(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coinseconomy.cobrar")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /cobrar <jogador> <quantidade>");
            return true;
        }

        OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[0]);
        if (!alvo.hasPlayedBefore() && !alvo.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Esse jogador nunca entrou no servidor.");
            return true;
        }

        double quantidade;
        try {
            quantidade = Double.parseDouble(args[1].replace(",", "."));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantidade inválida.");
            return true;
        }

        if (quantidade <= 0) {
            sender.sendMessage(ChatColor.RED + "A quantidade deve ser maior que zero.");
            return true;
        }

        economia.criarConta(alvo);

        boolean permiteNegativo = plugin.getConfig().getBoolean("cobranca-permite-saldo-negativo", false);

        if (!permiteNegativo && !economia.tem(alvo.getUniqueId(), quantidade)) {
            sender.sendMessage(ChatColor.RED + alvo.getName() +
                    " não possui coins suficientes para cobrar essa quantidade.");
            return true;
        }

        if (permiteNegativo) {
            double saldoAtual = economia.getSaldo(alvo.getUniqueId());
            economia.definirSaldo(alvo, saldoAtual - quantidade);
        } else {
            economia.sacar(alvo, quantidade);
        }

        sender.sendMessage(ChatColor.GREEN + "Você cobrou " + economia.formatar(quantidade) +
                " de " + alvo.getName() + ".");

        if (alvo.isOnline()) {
            ((Player) alvo).sendMessage(ChatColor.RED + "Foi cobrado de você " +
                    economia.formatar(quantidade) + " por " + sender.getName() + ".");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String inicio = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(nome -> nome.toLowerCase().startsWith(inicio))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
