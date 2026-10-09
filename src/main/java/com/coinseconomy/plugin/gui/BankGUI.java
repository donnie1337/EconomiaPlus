package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.bank.BankManager;
import com.coinseconomy.plugin.bank.BankTransaction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Telas visuais do /banco inspiradas no layout enviado pelo servidor. */
public final class BankGUI {

    public static final int SLOT_INFO = 11;
    public static final int SLOT_OPERACOES = 13;
    public static final int SLOT_HISTORICO = 15;
    public static final int SLOT_DEPOSITAR = 11;
    public static final int SLOT_SACAR = 15;
    public static final int SLOT_VOLTAR = 31;

    private BankGUI() {
    }

    public static Inventory principal(CoinsEconomyPlugin plugin, Player player, EconomyManager economia, BankManager banco) {
        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, "Banco");
        holder.setInventory(inv);

        double saldoCarteira = economia.getSaldo(player.getUniqueId());
        double diaria = plugin.getConfig().getDouble("banco.rendimento-diario-percentual", 0.035);
        double mensal = (Math.pow(1.0D + (diaria / 100.0D), 30.0D) - 1.0D) * 100.0D;

        inv.setItem(SLOT_INFO, item(
                Material.NETHER_BRICKS,
                "&bSuas informações",
                List.of(
                        "",
                        "&7Resumo da sua vida financeira",
                        "",
                        "&fCarteira: &a" + formatCompactCoins(saldoCarteira),
                        "&fReserva bancária: &a" + formatCompactCoins(banco.getBalance(player.getUniqueId())),
                        "",
                        "&fRendimento diário: &b+" + formatPercent(diaria) + "%",
                        "&fRendimento mensal: &b+" + formatPercent(mensal) + "%",
                        "",
                        "&8O rendimento incide sobre a reserva bancária."
                )
        ));

        inv.setItem(SLOT_OPERACOES, item(
                Material.CRAFTING_TABLE,
                "&bOperações",
                List.of(
                        "",
                        "&7Organize seus Coins entre",
                        "&7carteira e reserva bancária.",
                        "",
                        "&8Deposite ou retire quando precisar.",
                        "",
                        "&eClique para movimentar"
                )
        ));

        inv.setItem(SLOT_HISTORICO, item(
                Material.PAPER,
                "&bHistórico",
                List.of(
                        "",
                        "&7Consulte o caminho dos seus Coins.",
                        "",
                        "&8Depósitos, saques e rendimentos",
                        "&8ficarão registrados neste extrato.",
                        "",
                        "&eClique para consultar"
                )
        ));

        return inv;
    }

    public static Inventory operacoes(CoinsEconomyPlugin plugin, Player player, BankManager banco) {
        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.OPERATIONS);
        Inventory inv = Bukkit.createInventory(holder, 36, "Banco • Operações");
        holder.setInventory(inv);

        double minimo = plugin.getConfig().getDouble("banco.operacoes.minimo", 1000.0);
        int limite = plugin.getConfig().getInt("banco.operacoes.limite-saques-diarios", 10);
        int saquesHoje = banco.getWithdrawCount(player.getUniqueId());
        String corSaques = saquesHoje >= 9 ? "&c" : (saquesHoje >= 6 ? "&6" : "&a");

        inv.setItem(SLOT_DEPOSITAR, item(
                Material.ENDER_CHEST,
                "&bDepositar",
                List.of(
                        "",
                        "&7Transfira parte da carteira",
                        "&7para sua reserva no banco.",
                        "",
                        "&fEntrada mínima: &a$" + compact(minimo) + " coins",
                        "&8O valor deixa sua carteira e vai ao banco.",
                        "",
                        "&eClique para iniciar um depósito"
                )
        ));

        inv.setItem(SLOT_SACAR, item(
                Material.CHEST,
                "&bSacar",
                List.of(
                        "",
                        "&7Traga Coins da reserva",
                        "&7de volta para sua carteira.",
                        "",
                        "&fRetirada mínima: &a$" + compact(minimo) + " coins",
                        "&fSaques de hoje: " + corSaques + saquesHoje + "/" + limite,
                        "",
                        "&eClique para iniciar um saque"
                )
        ));

        inv.setItem(SLOT_VOLTAR, item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar ao menu do banco.")
        ));

        return inv;
    }

    public static Inventory historico(Player player, BankManager banco) {
        return historico(player, banco, 0);
    }

    public static Inventory historico(Player player, BankManager banco, int pagina) {
        List<BankTransaction> movimentacoes = banco.getHistory(player.getUniqueId());
        final int maxPorPagina = 21;
        int totalPaginas = Math.max(1, (int) Math.ceil(movimentacoes.size() / (double) maxPorPagina));
        int paginaValida = Math.max(0, Math.min(pagina, totalPaginas - 1));

        int inicio = paginaValida * maxPorPagina;
        int quantidadePagina = Math.min(maxPorPagina, Math.max(0, movimentacoes.size() - inicio));
        int tamanho = tamanhoHistorico(quantidadePagina);

        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.HISTORY, paginaValida);
        Inventory inv = Bukkit.createInventory(holder, tamanho, "Banco • Histórico");
        holder.setInventory(inv);

        if (quantidadePagina == 0) {
            inv.setItem(slotCentralHistorico(tamanho), item(
                    Material.GRAY_DYE,
                    "&7Nenhuma movimentação bancária",
                    List.of(
                            "",
                            "&7Seu extrato bancário está vazio.",
                            "",
                            "&8Depósitos, saques e rendimentos",
                            "&8aparecerão organizados aqui."
                    )
            ));
        } else {
            int[] slots = slotsHistorico(tamanho);
            for (int i = 0; i < quantidadePagina; i++) {
                inv.setItem(slots[i], transactionItem(movimentacoes.get(inicio + i)));
            }
        }

        int base = tamanho - 9;
        if (paginaValida > 0) {
            inv.setItem(base + 3, item(
                    Material.ARROW,
                    "&aAnterior",
                    List.of("", "&7Clique para voltar à página anterior.")
            ));
        }

        inv.setItem(base + 4, item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar ao menu do banco.")
        ));

        if (paginaValida + 1 < totalPaginas) {
            inv.setItem(base + 5, item(
                    Material.ARROW,
                    "&aPróxima",
                    List.of("", "&7Clique para avançar para a próxima página.")
            ));
        }

        return inv;
    }

    public static int slotAnteriorHistorico(int tamanhoInventario) {
        return tamanhoInventario - 6;
    }

    public static int slotProximaHistorico(int tamanhoInventario) {
        return tamanhoInventario - 4;
    }

    public static int slotVoltarHistorico(int tamanhoInventario) {
        return tamanhoInventario - 5;
    }

    private static int tamanhoHistorico(int quantidade) {
        int linhasConteudo = Math.max(1, (int) Math.ceil(Math.max(1, quantidade) / 7.0D));
        // 1 linha vazia no topo + conteúdo + 1 linha vazia de separação + navegação.
        return Math.min(54, (linhasConteudo + 3) * 9);
    }

    private static int slotCentralHistorico(int tamanho) {
        int linhasConteudo = (tamanho / 9) - 3;
        int linha = 1 + Math.max(0, (linhasConteudo - 1) / 2);
        return linha * 9 + 4;
    }

    private static int[] slotsHistorico(int tamanho) {
        int linhasConteudo = (tamanho / 9) - 3;
        int[] slots = new int[linhasConteudo * 7];
        int indice = 0;

        for (int linha = 1; linha <= linhasConteudo; linha++) {
            for (int coluna = 1; coluna <= 7; coluna++) {
                slots[indice++] = linha * 9 + coluna;
            }
        }
        return slots;
    }

    private static ItemStack transactionItem(BankTransaction transaction) {
        Material material;
        String prefix;
        String description;

        switch (transaction.type()) {
            case DEPOSIT -> {
                material = Material.EMERALD;
                prefix = "&a+";
                description = "&fQuantia depositada: &a";
            }
            case WITHDRAW -> {
                material = Material.REDSTONE;
                prefix = "&c-";
                description = "&fQuantia sacada: &c";
            }
            case INTEREST -> {
                material = Material.GOLD_INGOT;
                prefix = "&e+";
                description = "&fRendimento bancário: &e";
            }
            default -> throw new IllegalStateException("Tipo de movimentação bancária desconhecido.");
        }

        String date = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(transaction.timestamp()));

        return item(
                material,
                prefix + " " + formatCompactCoins(transaction.amount()),
                List.of(
                        "",
                        "&fData: &7" + date,
                        description + formatCompactCoins(transaction.amount())
                )
        );
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(BankGUI::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatCompactCoins(double value) {
        double abs = Math.abs(value);
        String suffix = "";
        double divisor = 1.0D;

        if (abs >= 1_000_000_000_000.0D) {
            suffix = "T";
            divisor = 1_000_000_000_000.0D;
        } else if (abs >= 1_000_000_000.0D) {
            suffix = "B";
            divisor = 1_000_000_000.0D;
        } else if (abs >= 1_000_000.0D) {
            suffix = "M";
            divisor = 1_000_000.0D;
        } else if (abs >= 1_000.0D) {
            suffix = "K";
            divisor = 1_000.0D;
        }

        double shown = value / divisor;
        String number = String.format(java.util.Locale.of("pt", "BR"), "%.2f", shown)
                .replaceAll("0+$", "")
                .replaceAll(",$", "");

        return number + suffix + " Coins";
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.3f", value).replace('.', ',');
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return String.format(java.util.Locale.US, "%.0f", value);
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
