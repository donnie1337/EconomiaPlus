package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Menu principal de carteira aberto por /coins. */
public final class CoinsWalletGUI {

    public static final int TAMANHO = 27;
    public static final int SLOT_SALDO = 11;
    public static final int SLOT_PAGAR = 13;
    public static final int SLOT_RANKING = 15;
    public static final int SLOT_AJUDA = 22;
    public static final int SLOT_FECHAR = 26;

    private CoinsWalletGUI() {
    }

    public static Inventory construir(Player player, EconomyManager economia) {
        economia.criarConta(player);
        double saldo = economia.getSaldo(player.getUniqueId());

        CoinsWalletGUIHolder holder = new CoinsWalletGUIHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                TAMANHO,
                color("&0&lCoins &8• &fMinha carteira")
        );
        holder.setInventory(inventory);

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < TAMANHO; slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(SLOT_SALDO, item(
                Material.EMERALD,
                "&a&lMeu saldo",
                List.of(
                        "",
                        "&7Você possui:",
                        "&f" + economia.formatar(saldo),
                        "",
                        "&8Seu saldo atual de Coins."
                )
        ));

        inventory.setItem(SLOT_PAGAR, item(
                Material.PAPER,
                "&a&lPagar jogador",
                List.of(
                        "",
                        "&7Envie Coins para outro jogador.",
                        "",
                        "&eClique para ver como pagar."
                )
        ));

        inventory.setItem(SLOT_RANKING, item(
                Material.GOLD_INGOT,
                "&6&lRanking de Coins",
                List.of(
                        "",
                        "&7Veja os jogadores com",
                        "&7mais Coins do servidor.",
                        "",
                        "&eClique para abrir."
                )
        ));

        inventory.setItem(SLOT_AJUDA, item(
                Material.BOOK,
                "&b&lAjuda",
                List.of(
                        "",
                        "&7Veja todos os comandos",
                        "&7públicos da economia.",
                        "",
                        "&eClique para visualizar."
                )
        ));

        inventory.setItem(SLOT_FECHAR, item(
                Material.BARRIER,
                "&c&lFechar",
                List.of("", "&7Clique para fechar o menu.")
        ));

        return inventory;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(CoinsWalletGUI::color).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
