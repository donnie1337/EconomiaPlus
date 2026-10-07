package com.coinseconomy.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Histórico visual de Coins.
 * O ledger persistente será conectado nesta tela na próxima etapa.
 */
public final class CoinsTransactionsGUI {

    public static final int SLOT_VOLTAR = 22;

    private CoinsTransactionsGUI() {
    }

    public static Inventory construir() {
        CoinsTransactionsGUIHolder holder = new CoinsTransactionsGUIHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, "Coins > Transações");
        holder.setInventory(inventory);

        inventory.setItem(13, item(
                Material.GRAY_DYE,
                "&7Nenhuma transação registrada",
                List.of(
                        "",
                        "&7Seu extrato ainda está vazio.",
                        "",
                        "&8Quando o histórico for ativado,",
                        "&8suas movimentações aparecerão aqui."
                )
        ));

        inventory.setItem(SLOT_VOLTAR, item(
                Material.ARROW,
                "&fVoltar",
                List.of("", "&8Retornar à sua carteira.")
        ));

        return inventory;
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
