package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    public static Inventory principal(CoinsEconomyPlugin plugin, Player player, EconomyManager economia) {
        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, "Banco");
        holder.setInventory(inv);

        double saldoCarteira = economia.getSaldo(player.getUniqueId());
        double diaria = plugin.getConfig().getDouble("banco.rendimento-diario-percentual", 0.035);
        double mensal = plugin.getConfig().getDouble("banco.rendimento-mensal-percentual", 1.05);

        inv.setItem(SLOT_INFO, item(
                Material.NETHER_BRICKS,
                "&bSUAS INFORMAÇÕES",
                List.of(
                        "",
                        "&7Resumo da sua vida financeira",
                        "",
                        "&fCarteira: &a" + economia.formatar(saldoCarteira),
                        "&fReserva bancária: &a" + economia.formatar(0.0),
                        "",
                        "&fProjeção diária: &b+" + formatPercent(diaria) + "%",
                        "&fProjeção mensal: &b+" + formatPercent(mensal) + "%",
                        "",
                        "&8Taxas exibidas são a referência atual do banco."
                )
        ));

        inv.setItem(SLOT_OPERACOES, item(
                Material.CRAFTING_TABLE,
                "&bOPERAÇÕES",
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
                "&bHISTÓRICO",
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

    public static Inventory operacoes(CoinsEconomyPlugin plugin) {
        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.OPERATIONS);
        Inventory inv = Bukkit.createInventory(holder, 36, "Banco > Operações");
        holder.setInventory(inv);

        double minimo = plugin.getConfig().getDouble("banco.operacoes.minimo", 1000.0);
        int limite = plugin.getConfig().getInt("banco.operacoes.limite-saques-diarios", 10);

        inv.setItem(SLOT_DEPOSITAR, item(
                Material.ENDER_CHEST,
                "&bDepositar",
                List.of(
                        "",
                        "&7Transfira parte da carteira",
                        "&7para sua reserva no banco.",
                        "",
                        "&fEntrada mínima: &a" + compact(minimo) + " coins",
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
                        "&fRetirada mínima: &a" + compact(minimo) + " coins",
                        "&fSaques de hoje: &c0&7/&b" + limite,
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

    public static Inventory historico() {
        return historico(List.of(), 0);
    }

    public static Inventory historico(List<ItemStack> movimentacoes, int pagina) {
        List<ItemStack> registros = movimentacoes == null ? List.of() : movimentacoes;
        final int maxPorPagina = 21;
        int totalPaginas = Math.max(1, (int) Math.ceil(registros.size() / (double) maxPorPagina));
        int paginaValida = Math.max(0, Math.min(pagina, totalPaginas - 1));

        int inicio = paginaValida * maxPorPagina;
        int quantidadePagina = Math.min(maxPorPagina, Math.max(0, registros.size() - inicio));
        int tamanho = tamanhoHistorico(quantidadePagina);

        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.HISTORY);
        Inventory inv = Bukkit.createInventory(holder, tamanho, "Banco > Histórico");
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
                inv.setItem(slots[i], registros.get(inicio + i));
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
