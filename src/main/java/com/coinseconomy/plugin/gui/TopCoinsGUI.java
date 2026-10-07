package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ranking paginado de jogadores, seguindo o layout visual do menu de Coins. */
public final class TopCoinsGUI {

    private static final int TAMANHO = 36;
    private static final int POR_PAGINA = 7;
    private static final int[] SLOTS_JOGADORES = {10, 11, 12, 13, 14, 15, 16};

    public static final int SLOT_ANTERIOR = 30;
    public static final int SLOT_VOLTAR = 31;
    public static final int SLOT_PROXIMA = 32;

    private TopCoinsGUI() {
    }

    public static Inventory construir(EconomyManager economia) {
        return construir(economia, 0);
    }

    public static Inventory construir(EconomyManager economia, int pagina) {
        List<Map.Entry<UUID, Double>> ranking = economia.getTop(Integer.MAX_VALUE);
        int totalPaginas = Math.max(1, (int) Math.ceil(ranking.size() / (double) POR_PAGINA));
        int paginaValida = Math.max(0, Math.min(pagina, totalPaginas - 1));

        TopCoinsGUIHolder holder = new TopCoinsGUIHolder(paginaValida);
        Inventory inventario = Bukkit.createInventory(holder, TAMANHO, "Coins • Top Jogadores");
        holder.setInventory(inventario);

        int inicio = paginaValida * POR_PAGINA;
        for (int i = 0; i < POR_PAGINA; i++) {
            int indice = inicio + i;
            if (indice >= ranking.size()) break;

            Map.Entry<UUID, Double> entrada = ranking.get(indice);
            inventario.setItem(
                    SLOTS_JOGADORES[i],
                    criarCabeca(economia, entrada.getKey(), entrada.getValue(), indice + 1)
            );
        }

        if (paginaValida > 0) {
            inventario.setItem(SLOT_ANTERIOR, criarBotao(
                    Material.ARROW,
                    "&aAnterior",
                    List.of("", "&8Página anterior")
            ));
        }

        inventario.setItem(SLOT_VOLTAR, criarBotao(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar ao menu de Coins.")
        ));

        if (paginaValida + 1 < totalPaginas) {
            inventario.setItem(SLOT_PROXIMA, criarBotao(
                    Material.ARROW,
                    "&aPróxima",
                    List.of("", "&8Próxima página")
            ));
        }

        return inventario;
    }

    private static ItemStack criarCabeca(EconomyManager economia, UUID uuid, double saldo, int posicao) {
        OfflinePlayer jogador = Bukkit.getOfflinePlayer(uuid);
        ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) cabeca.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(jogador);
            String nome = jogador.getName() != null
                    ? jogador.getName()
                    : economia.getNomeConhecido(uuid);

            meta.setDisplayName(color("&b" + posicao + "º &8- &a" + nome));
            meta.setLore(List.of(
                    "",
                    color("&7Patrimônio contabilizado:"),
                    color("&a" + formatCompactCoins(saldo)),
                    "",
                    color("&8Posição geral: &f#" + posicao)
            ));
            cabeca.setItemMeta(meta);
        }

        return cabeca;
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

        if (suffix.isEmpty()) {
            return String.format(java.util.Locale.of("pt", "BR"), "%.2f Coins", value);
        }

        double compact = value / divisor;
        String number = String.format(java.util.Locale.of("pt", "BR"), "%.2f", compact)
                .replaceAll("0+$", "")
                .replaceAll(",$", "");
        return number + suffix + " Coins";
    }

    private static ItemStack criarBotao(Material material, String nome, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(nome));
            meta.setLore(lore.stream().map(TopCoinsGUI::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
