package com.coinseconomy.plugin.commands;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.gui.CoinsWalletGUI;
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
 * /coins            -> abre a carteira em GUI
 * /coins <jogador>  -> mostra o saldo de outro jogador
 * /coins ajuda      -> mostra todos os comandos de Coins
 * /coins help       -> alias de /coins ajuda
 * /coins top        -> atalho para /topcoins
 * /coins pagar <jogador> <quantidade> -> transfere coins
 * /coins pay   <jogador> <quantidade> -> alias de /coins pagar
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
            case "pagar":
            case "pay":
                return pagar(sender, args);
            case "ajuda":
            case "help":
                return mostrarAjuda(sender);
            case "top":
                Bukkit.dispatchCommand(sender, "topcoins");
                return true;
            default:
                return mostrarSaldoDeOutro(sender, args[0]);
        }
    }

    private boolean mostrarSaldoProprio(CommandSender sender) {
        if (!(sender instanceof Player jogador)) {
            sender.sendMessage(ChatColor.RED + "Use /coins <jogador> quando executar pelo console.");
            return true;
        }

        jogador.openInventory(CoinsWalletGUI.construir(jogador, economia));
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

    private boolean mostrarAjuda(CommandSender sender) {
        sender.sendMessage(color("&a[Coins] &fComandos disponíveis para economia:"));
        sender.sendMessage(color("&8» &f/coins &8- &bMostra seu saldo."));
        sender.sendMessage(color("&8» &f/coins <jogador> &8- &bMostra o saldo de outro jogador."));
        sender.sendMessage(color("&8» &f/coins pagar <jogador> <quantidade> &8- &bEnvia coins para outro jogador."));
        sender.sendMessage(color("&8» &f/topcoins &8- &bAbre o ranking de coins."));
        sender.sendMessage(color("&8» &f/coins ajuda &8- &bExibe todos os comandos disponíveis."));
        return true;
    }

    private boolean pagar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player pagador)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        if (!pagador.hasPermission("coinseconomy.pagar")) {
            pagador.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length < 3) {
            pagador.sendMessage(ChatColor.RED + "Uso correto: /coins pagar <jogador> <quantidade>");
            return true;
        }

        if (args[1].equalsIgnoreCase(pagador.getName())) {
            pagador.sendMessage(ChatColor.RED + "Você não pode pagar a si mesmo.");
            return true;
        }

        OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[1]);
        if (!alvo.hasPlayedBefore() && !alvo.isOnline()) {
            pagador.sendMessage(ChatColor.RED + "Esse jogador nunca entrou no servidor.");
            return true;
        }

        double quantidade;
        try {
            quantidade = Double.parseDouble(args[2].replace(",", "."));
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

        pagador.sendMessage(ChatColor.GREEN + "Você pagou " + economia.formatar(quantidade)
                + " para " + alvo.getName() + ".");

        if (alvo.isOnline()) {
            ((Player) alvo).sendMessage(ChatColor.GREEN + pagador.getName() + " te pagou "
                    + economia.formatar(quantidade) + "!");
        }

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
            sugestoes.add("ajuda");
            sugestoes.add("help");
            sugestoes.add("pagar");
            sugestoes.add("pay");
            sugestoes.add("top");
            sugestoes.addAll(nomesOnline());
            return filtrar(sugestoes, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give")
                || args[0].equalsIgnoreCase("set")
                || args[0].equalsIgnoreCase("pagar")
                || args[0].equalsIgnoreCase("pay"))) {
            return filtrar(nomesOnline(), args[1]);
        }

        return List.of();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
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
