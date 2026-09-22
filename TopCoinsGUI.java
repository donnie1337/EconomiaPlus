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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Monta a GUI de ranking (estilo "pódio") com as cabeças dos 10 jogadores
 * com mais coins do servidor.
 *
 * Layout de uma inventory de 27 slots (3 linhas):
 * - slot 4  -> 1º lugar, em destaque na linha de cima
 * - slots 9-17 -> 2º ao 10º lugar, na linha do meio
 * - linha de baixo e o resto da linha de cima -> vidro decorativo
 */
public final class TopCoinsGUI {

    private static final int TAMANHO = 27;
    private static final int SLOT_PRIMEIRO_LUGAR = 4;
    private static final int[] SLOTS_DEMAIS_LUGARES = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    private TopCoinsGUI() {
    }

    public static Inventory construir(EconomyManager economia) {
        TopCoinsGUIHolder holder = new TopCoinsGUIHolder();
        Inventory inventario = Bukkit.createInventory(
                holder, TAMANHO, ChatColor.GOLD + "" + ChatColor.BOLD + "Top 10 - Coins");
        holder.setInventory(inventario);

        preencherComVidro(inventario);

        List<Map.Entry<UUID, Double>> top = economia.getTop(10);

        // 1º lugar, em destaque
        if (!top.isEmpty()) {
            Map.Entry<UUID, Double> primeiro = top.get(0);
            inventario.setItem(SLOT_PRIMEIRO_LUGAR, criarCabeca(economia, primeiro.getKey(), primeiro.getValue(), 1));
        } else {
            inventario.setItem(SLOT_PRIMEIRO_LUGAR, criarSlotVazio(1));
        }

        // 2º ao 10º lugar
        for (int i = 0; i < SLOTS_DEMAIS_LUGARES.length; i++) {
            int posicao = i + 2;
            int slot = SLOTS_DEMAIS_LUGARES[i];

            if (i + 1 < top.size()) {
                Map.Entry<UUID, Double> entrada = top.get(i + 1);
                inventario.setItem(slot, criarCabeca(economia, entrada.getKey(), entrada.getValue(), posicao));
            } else {
                inventario.setItem(slot, criarSlotVazio(posicao));
            }
        }

        return inventario;
    }

    private static void preencherComVidro(Inventory inventario) {
        ItemStack vidroEscuro = criarVidro(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < TAMANHO; slot++) {
            inventario.setItem(slot, vidroEscuro);
        }
    }

    private static ItemStack criarVidro(Material material, String nome) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack criarSlotVazio(int posicao) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "#" + posicao + ChatColor.DARK_GRAY + " - Vago");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack criarCabeca(EconomyManager economia, UUID uuid, double saldo, int posicao) {
        OfflinePlayer jogador = Bukkit.getOfflinePlayer(uuid);
        ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) cabeca.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(jogador);

            ChatColor cor = corParaPosicao(posicao);
            String nome = jogador.getName() != null ? jogador.getName() : economia.getNomeConhecido(uuid);

            meta.setDisplayName(cor + "" + ChatColor.BOLD + "#" + posicao + ChatColor.RESET + " " + cor + nome);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Saldo: " + ChatColor.YELLOW + economia.formatar(saldo));
            meta.setLore(lore);

            cabeca.setItemMeta(meta);
        }

        return cabeca;
    }

    private static ChatColor corParaPosicao(int posicao) {
        switch (posicao) {
            case 1:
                return ChatColor.GOLD;
            case 2:
                return ChatColor.WHITE;
            case 3:
                return ChatColor.RED;
            default:
                return ChatColor.YELLOW;
        }
    }
}
