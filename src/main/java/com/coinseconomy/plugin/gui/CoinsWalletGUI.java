package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Hub visual principal aberto por /coins, baseado no layout de 27 slots. */
public final class CoinsWalletGUI {

    public static final int TAMANHO = 27;
    public static final int SLOT_INFORMACOES = 4;
    public static final int SLOT_TRANSACOES = 11;
    public static final int SLOT_TOP = 15;
    public static final int SLOT_MAGNATA = 23;

    private CoinsWalletGUI() {
    }

    public static Inventory construir(Player player, EconomyManager economia) {
        economia.criarConta(player);

        CoinsWalletGUIHolder holder = new CoinsWalletGUIHolder();
        Inventory inventory = Bukkit.createInventory(holder, TAMANHO, "Coins");
        holder.setInventory(inventory);

        double saldo = economia.getSaldo(player.getUniqueId());

        inventory.setItem(SLOT_INFORMACOES, item(
                Material.NETHER_BRICKS,
                "&b&lSUAS INFORMAÇÕES",
                List.of(
                        "",
                        "&fSaldo atual: &a" + economia.formatar(saldo),
                        "&fTransações: &b0",
                        "&fRecebimento: &aON",
                        "",
                        "&aClique para alternar o status"
                )
        ));

        inventory.setItem(SLOT_TRANSACOES, item(
                Material.PAPER,
                "&b&lTRANSAÇÕES",
                List.of(
                        "",
                        "&fHistórico de movimentações",
                        "&frealizadas no servidor",
                        "",
                        "&aClique para visualizar"
                )
        ));

        inventory.setItem(SLOT_TOP, item(
                Material.BOOK,
                "&b&lTOP JOGADORES",
                List.of(
                        "",
                        "&fRanking dos jogadores",
                        "&fmais ricos do servidor",
                        "",
                        "&aClique para visualizar"
                )
        ));

        inventory.setItem(SLOT_MAGNATA, magnata(economia));

        return inventory;
    }

    private static ItemStack magnata(EconomyManager economia) {
        List<Map.Entry<UUID, Double>> top = economia.getTop(1);
        if (top.isEmpty()) {
            return item(
                    Material.EMERALD_BLOCK,
                    "&b&lMAGNATA",
                    List.of("", "&7Nenhum jogador no ranking ainda.")
            );
        }

        Map.Entry<UUID, Double> first = top.get(0);
        OfflinePlayer player = Bukkit.getOfflinePlayer(first.getKey());
        String name = player.getName() != null ? player.getName() : economia.getNomeConhecido(first.getKey());

        ItemStack stack = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&b&lMAGNATA"));
            meta.setLore(List.of(
                    "",
                    color("&fJogador: &a" + name),
                    "",
                    color("&fFortuna: &a" + economia.formatar(first.getValue()))
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(CoinsWalletGUI::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
