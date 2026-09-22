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
 * /pagar <jogador> <quantidade>
 * Transfere coins do saldo de quem executa o comando para o jogador alvo.
 */
public class PagarCommand implements CommandExecutor, TabCompleter {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public PagarCommand(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        Player pagador = (Player) sender;

        if (!pagador.hasPermission("coinseconomy.pagar")) {
            pagador.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            pagador.sendMessage(ChatColor.RED + "Uso correto: /pagar <jogador> <quantidade>");
            return true;
        }

        if (args[0].equalsIgnoreCase(pagador.getName())) {
            pagador.sendMessage(ChatColor.RED + "Você não pode pagar a si mesmo.");
            return true;
        }

        OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[0]);
        if (!alvo.hasPlayedBefore() && !alvo.isOnline()) {
            pagador.sendMessage(ChatColor.RED + "Esse jogador nunca entrou no servidor.");
            return true;
        }

        double quantidade;
        try {
            quantidade = Double.parseDouble(args[1].replace(",", "."));
        } catch (NumberFormatException e) {
            pagador.sendMessage(ChatColor.RED + "Quantidade inválida.");
            return true;
        }

        if (quantidade <= 0) {
            pagador.sendMessage(ChatColor.RED + "A quantidade deve ser maior que zero.");
            return true;
        }

        economia.criarConta(pagador);
        economia.criarConta(alvo);

        if (!economia.tem(pagador.getUniqueId(), quantidade)) {
            pagador.sendMessage(ChatColor.RED + "Você não possui coins suficientes para essa transação.");
            return true;
        }

        economia.sacar(pagador, quantidade);
        economia.depositar(alvo, quantidade);

        pagador.sendMessage(ChatColor.GREEN + "Você pagou " + economia.formatar(quantidade) +
                " para " + alvo.getName() + ".");

        if (alvo.isOnline()) {
            ((Player) alvo).sendMessage(ChatColor.GREEN + pagador.getName() + " te pagou " +
                    economia.formatar(quantidade) + "!");
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
