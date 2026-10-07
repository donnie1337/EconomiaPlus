package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.transactions.WalletTransaction;
import com.coinseconomy.plugin.transactions.WalletTransactionManager;
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
import java.util.List;
import java.util.Locale;

public final class CoinsTransactionsGUI {

    public static final int REGISTROS_POR_LINHA = 7;
    public static final int MAX_REGISTROS_POR_PAGINA = 21;

    private CoinsTransactionsGUI() {
    }

    public static Inventory construir(Player player, WalletTransactionManager manager) {
        return construir(player, manager, 0);
    }

    public static Inventory construir(Player player, WalletTransactionManager manager, int pagina) {
        List<WalletTransaction> registros = manager.getHistory(player.getUniqueId());
        int totalPaginas = Math.max(1, (int) Math.ceil(registros.size() / (double) MAX_REGISTROS_POR_PAGINA));
        int paginaValida = Math.max(0, Math.min(pagina, totalPaginas - 1));

        int inicio = paginaValida * MAX_REGISTROS_POR_PAGINA;
        int quantidadePagina = Math.min(MAX_REGISTROS_POR_PAGINA, Math.max(0, registros.size() - inicio));
        int tamanho = calcularTamanho(quantidadePagina);

        CoinsTransactionsGUIHolder holder = new CoinsTransactionsGUIHolder(paginaValida);
        Inventory inventory = Bukkit.createInventory(holder, tamanho, "Coins • Transações");
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
                inventory.setItem(slots[i], transactionItem(registros.get(inicio + i)));
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

    public static int slotAnterior(int tamanhoInventario) {
        return tamanhoInventario - 6;
    }

    public static int slotVoltar(int tamanhoInventario) {
        return tamanhoInventario - 5;
    }

    public static int slotProxima(int tamanhoInventario) {
        return tamanhoInventario - 4;
    }

    private static int calcularTamanho(int quantidade) {
        int linhasConteudo = Math.max(1, (int) Math.ceil(Math.max(1, quantidade) / (double) REGISTROS_POR_LINHA));
        // topo vazio + conteúdo + linha vazia de separação + navegação
        return Math.min(54, (linhasConteudo + 3) * 9);
    }

    private static int slotCentralConteudo(int tamanho) {
        int linhasConteudo = (tamanho / 9) - 3;
        int linha = 1 + Math.max(0, (linhasConteudo - 1) / 2);
        return linha * 9 + 4;
    }

    private static int[] slotsDeConteudo(int tamanho) {
        int linhasConteudo = (tamanho / 9) - 3;
        int[] slots = new int[linhasConteudo * REGISTROS_POR_LINHA];
        int indice = 0;

        for (int linha = 1; linha <= linhasConteudo; linha++) {
            for (int coluna = 1; coluna <= 7; coluna++) {
                slots[indice++] = linha * 9 + coluna;
            }
        }
        return slots;
    }

    private static ItemStack transactionItem(WalletTransaction transaction) {
        Material material;
        String prefix;
        String description;

        switch (transaction.type()) {
            case PAYMENT_SENT -> {
                material = Material.IRON_INGOT;
                prefix = "&c-";
                description = "&fEnviado para &a" + transaction.detail() + "&f: &c";
            }
            case PAYMENT_RECEIVED -> {
                material = Material.EMERALD;
                prefix = "&a+";
                description = "&fRecebido de &a" + transaction.detail() + "&f: &a";
            }
            case BANK_DEPOSIT -> {
                material = Material.ENDER_CHEST;
                prefix = "&c-";
                description = "&fDepositado no banco: &c";
            }
            case BANK_WITHDRAW -> {
                material = Material.CHEST;
                prefix = "&a+";
                description = "&fSacado do banco: &a";
            }
            case ADMIN_ADD -> {
                material = Material.EMERALD_BLOCK;
                prefix = "&a+";
                description = "&fQuantia adicionada: &a";
            }
            case ADMIN_REMOVE -> {
                material = Material.REDSTONE;
                prefix = "&c-";
                description = "&fQuantia removida: &c";
            }
            default -> throw new IllegalStateException("Tipo de transação desconhecido.");
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
            meta.setLore(lore.stream().map(CoinsTransactionsGUI::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatCompactCoins(double value) {
        double abs = Math.abs(value);
        String suffix = "";
        double divisor = 1.0D;

        if (abs >= 1_000_000_000_000.0D) { suffix = "T"; divisor = 1_000_000_000_000.0D; }
        else if (abs >= 1_000_000_000.0D) { suffix = "B"; divisor = 1_000_000_000.0D; }
        else if (abs >= 1_000_000.0D) { suffix = "M"; divisor = 1_000_000.0D; }
        else if (abs >= 1_000.0D) { suffix = "K"; divisor = 1_000.0D; }

        double shown = value / divisor;
        String number = String.format(Locale.of("pt", "BR"), "%.2f", shown)
                .replaceAll("0+$", "")
                .replaceAll(",$", "");
        return number + suffix + " Coins";
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
