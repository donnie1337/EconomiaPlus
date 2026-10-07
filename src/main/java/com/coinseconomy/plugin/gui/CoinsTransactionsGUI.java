package com.coinseconomy.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Histórico visual de Coins com tamanho dinâmico e paginação. */
public final class CoinsTransactionsGUI {

    public static final int REGISTROS_POR_LINHA = 7;
    public static final int MAX_REGISTROS_POR_PAGINA = 28;

    private CoinsTransactionsGUI() {
    }

    public static Inventory construir() {
        return construir(List.of(), 0);
    }

    public static Inventory construir(List<ItemStack> transacoes, int pagina) {
        List<ItemStack> registros = transacoes == null ? List.of() : transacoes;
        int totalPaginas = Math.max(1, (int) Math.ceil(registros.size() / (double) MAX_REGISTROS_POR_PAGINA));
        int paginaValida = Math.max(0, Math.min(pagina, totalPaginas - 1));

        int inicio = paginaValida * MAX_REGISTROS_POR_PAGINA;
        int quantidadePagina = Math.min(MAX_REGISTROS_POR_PAGINA, Math.max(0, registros.size() - inicio));
        int tamanho = calcularTamanho(quantidadePagina);

        CoinsTransactionsGUIHolder holder = new CoinsTransactionsGUIHolder();
        Inventory inventory = Bukkit.createInventory(holder, tamanho, "Coins > Transações");
        holder.setInventory(inventory);

        if (quantidadePagina == 0) {
            inventory.setItem(slotCentralConteudo(tamanho), item(
                    Material.GRAY_DYE,
                    "&7Nenhuma transação registrada",
                    List.of(
                            "",
                            "&7Seu extrato ainda está vazio.",
                            "",
                            "&8Quando houver movimentações,",
                            "&8elas aparecerão organizadas aqui."
                    )
            ));
        } else {
            int[] slots = slotsDeConteudo(tamanho);
            for (int i = 0; i < quantidadePagina; i++) {
                inventory.setItem(slots[i], registros.get(inicio + i));
            }
        }

        int base = tamanho - 9;
        if (paginaValida > 0) {
            inventory.setItem(base + 3, item(
                    Material.ARROW,
                    "&aAnterior",
                    List.of("", "&7Clique para voltar à página anterior.")
            ));
        }

        inventory.setItem(base + 4, item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar ao menu de Coins.")
        ));

        if (paginaValida + 1 < totalPaginas) {
            inventory.setItem(base + 5, item(
                    Material.ARROW,
                    "&aPróxima",
                    List.of("", "&7Clique para avançar para a próxima página.")
            ));
        }

        return inventory;
    }

    public static int slotVoltar(int tamanhoInventario) {
        return tamanhoInventario - 5;
    }

    private static int calcularTamanho(int quantidade) {
        int linhasConteudo = Math.max(1, (int) Math.ceil(Math.max(1, quantidade) / (double) REGISTROS_POR_LINHA));
        return Math.min(54, (linhasConteudo + 2) * 9);
    }

    private static int slotCentralConteudo(int tamanho) {
        int linhasConteudo = (tamanho / 9) - 2;
        int linha = 1 + Math.max(0, (linhasConteudo - 1) / 2);
        return linha * 9 + 4;
    }

    private static int[] slotsDeConteudo(int tamanho) {
        int linhasConteudo = (tamanho / 9) - 2;
        int[] slots = new int[linhasConteudo * REGISTROS_POR_LINHA];
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
            meta.setLore(lore.stream().map(CoinsTransactionsGUI::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
