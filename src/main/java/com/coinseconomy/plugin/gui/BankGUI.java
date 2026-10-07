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
    public static final int SLOT_VOLTAR = 22;

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
                        "&fEconomia: &aCoins",
                        "",
                        "&fSaldo no banco: &a" + economia.formatar(0.0),
                        "&fRendimento diário: &b" + formatPercent(diaria) + "%",
                        "&fRendimento mensal: &b" + formatPercent(mensal) + "%",
                        "",
                        "&fSaldo em mãos: &a" + economia.formatar(saldoCarteira)
                )
        ));

        inv.setItem(SLOT_OPERACOES, item(
                Material.CRAFTING_TABLE,
                "&bOPERAÇÕES",
                List.of(
                        "",
                        "&fRealize depósitos, saques",
                        "&fou consulte operações bancárias",
                        "",
                        "&aClique para acessar"
                )
        ));

        inv.setItem(SLOT_HISTORICO, item(
                Material.PAPER,
                "&bHISTÓRICO",
                List.of(
                        "",
                        "&fHistórico de movimentações",
                        "&frealizadas no banco",
                        "",
                        "&aClique para visualizar"
                )
        ));

        return inv;
    }

    public static Inventory operacoes(CoinsEconomyPlugin plugin) {
        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.OPERATIONS);
        Inventory inv = Bukkit.createInventory(holder, 27, "Banco > Operações");
        holder.setInventory(inv);

        double minimo = plugin.getConfig().getDouble("banco.operacoes.minimo", 1000.0);
        int limite = plugin.getConfig().getInt("banco.operacoes.limite-saques-diarios", 10);

        inv.setItem(SLOT_DEPOSITAR, item(
                Material.ENDER_CHEST,
                "&bDepositar",
                List.of(
                        "",
                        "&fGuardar coins das mãos",
                        "&fno banco com segurança",
                        "",
                        "&fMínimo: &a" + compact(minimo),
                        "",
                        "&aClique para depositar"
                )
        ));

        inv.setItem(SLOT_SACAR, item(
                Material.CHEST,
                "&bSacar",
                List.of(
                        "",
                        "&fRetirar coins do banco",
                        "&fpara as suas mãos",
                        "",
                        "&fMínimo: &a" + compact(minimo),
                        "&fLimite diário usado: &c0&f/&b" + limite,
                        "",
                        "&aClique para sacar"
                )
        ));

        inv.setItem(SLOT_VOLTAR, item(
                Material.ARROW,
                "&fVoltar",
                List.of("", "&7Clique para voltar ao Banco.")
        ));

        return inv;
    }

    public static Inventory historico() {
        BankGUIHolder holder = new BankGUIHolder(BankGUIHolder.Screen.HISTORY);
        Inventory inv = Bukkit.createInventory(holder, 27, "Banco > Histórico");
        holder.setInventory(inv);

        inv.setItem(13, item(
                Material.GRAY_DYE,
                "&7Nenhuma movimentação bancária",
                List.of(
                        "",
                        "&8Depósitos, saques e rendimentos",
                        "&8serão exibidos aqui."
                )
        ));

        inv.setItem(SLOT_VOLTAR, item(
                Material.ARROW,
                "&fVoltar",
                List.of("", "&7Clique para voltar ao Banco.")
        ));

        return inv;
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
