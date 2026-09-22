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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /coins            -> mostra o próprio saldo
 * /coins <jogador>  -> mostra o saldo de outro jogador
 * /coins top        -> atalho para /topcoins
 * /coins give <jogador> <quantidade>  (admin) -> adiciona coins
 * /coins set  <jogador> <quantidade>  (admin) -> define o saldo
 */
public class CoinsCommand implements CommandExecutor, TabCompleter {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economia;

    public CoinsCommand(CoinsEconomyPlugin plugin, EconomyManager economia) {
        this.plugin = plugin;
        this.economia = economia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return mostrarSaldoProprio(sender);
        }

        String primeiroArgumento = args[0].toLowerCase();

        switch (primeiroArgumento) {
            case "give":
            case "add":
                return alterarSaldoAdmin(sender, args, TipoAlteracao.ADICIONAR);
            case "set":
                return alterarSaldoAdmin(sender, args, TipoAlteracao.DEFINIR);
            case "top":
                Bukkit.dispatchCommand(sender, "topcoins");
                return true;
            default:
                return mostrarSaldoDeOutro(sender, args[0]);
        }
    }

    private boolean mostrarSaldoProprio(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Use /coins <jogador> quando executar pelo console.");
            return true;
        }

        Player jogador = (Player) sender;
        economia.criarConta(jogador);
        double saldo = economia.getSaldo(jogador.getUniqueId());

        sender.sendMessage(ChatColor.GOLD + "Você possui " + ChatColor.YELLOW +
                economia.formatar(saldo) + ChatColor.GOLD + ".");
        return true;
    }

    private boolean mostrarSaldoDeOutro(CommandSender sender, String nomeJogador) {
        OfflinePlayer alvo = Bukkit.getOfflinePlayer(nomeJogador);

        if (!alvo.hasPlayedBefore() && !alvo.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Esse jogador nunca entrou no servidor.");
            return true;
        }

        economia.criarConta(alvo);
        double saldo = economia.getSaldo(alvo.getUniqueId());

        sender.sendMessage(ChatColor.GOLD + alvo.getName() + ChatColor.GRAY + " possui " +
                ChatColor.YELLOW + economia.formatar(saldo) + ChatColor.GRAY + ".");
        return true;
    }

    private enum TipoAlteracao { ADICIONAR, DEFINIR }

    private boolean alterarSaldoAdmin(CommandSender sender, String[] args, TipoAlteracao tipo) {
        if (!sender.hasPermission("coinseconomy.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length < 3) {
            String sub = tipo == TipoAlteracao.ADICIONAR ? "give" : "set";
            sender.sendMessage(ChatColor.RED + "Uso correto: /coins " + sub + " <jogador> <quantidade>");
            return true;
        }

        OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[1]);
        if (!alvo.hasPlayedBefore() && !alvo.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Esse jogador nunca entrou no servidor.");
            return true;
        }

        double quantidade;
        try {
            quantidade = Double.parseDouble(args[2].replace(",", "."));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantidade inválida.");
            return true;
        }

        if (quantidade < 0) {
            sender.sendMessage(ChatColor.RED + "A quantidade não pode ser negativa.");
            return true;
        }

        if (tipo == TipoAlteracao.ADICIONAR) {
            economia.depositar(alvo, quantidade);
        } else {
            economia.definirSaldo(alvo, quantidade);
        }

        double novoSaldo = economia.getSaldo(alvo.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Saldo de " + alvo.getName() + " agora é " +
                economia.formatar(novoSaldo) + ".");

        if (alvo.isOnline()) {
            ((Player) alvo).sendMessage(ChatColor.GOLD + "Seu saldo foi atualizado para " +
                    economia.formatar(novoSaldo) + ".");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sugestoes = new ArrayList<>();
            if (sender.hasPermission("coinseconomy.admin")) {
                sugestoes.add("give");
                sugestoes.add("set");
            }
            sugestoes.add("top");
            sugestoes.addAll(nomesOnline());
            return filtrar(sugestoes, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set"))) {
            return filtrar(nomesOnline(), args[1]);
        }

        return List.of();
    }

    private List<String> nomesOnline() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> filtrar(List<String> opcoes, String inicio) {
        return opcoes.stream()
                .filter(op -> op.toLowerCase().startsWith(inicio.toLowerCase()))
                .collect(Collectors.toList());
    }
}
