package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.bank.BankManager;
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
    public static final int SLOT_INFORMACOES = 10;
    public static final int SLOT_TRANSACOES = 12;
    public static final int SLOT_TOP = 14;
    public static final int SLOT_MAGNATA = 16;

    private CoinsWalletGUI() {
    }

    public static Inventory construir(Player player, EconomyManager economia) {
        economia.criarConta(player);

        CoinsWalletGUIHolder holder = new CoinsWalletGUIHolder();
        Inventory inventory = Bukkit.createInventory(holder, TAMANHO, "Coins");
        holder.setInventory(inventory);

        double saldo = economia.getSaldo(player.getUniqueId());
        BankManager banco = CoinsEconomyPlugin.getInstance().getBankManager();
        double reservaBancaria = banco == null ? 0.0D : banco.getBalance(player.getUniqueId());
        boolean recebimentosAtivados = CoinsEconomyPlugin.getInstance()
                .getWalletSettingsManager()
                .canReceive(player.getUniqueId());

        inventory.setItem(SLOT_INFORMACOES, item(
                Material.NETHER_BRICKS,
                "&bSUAS INFORMAÇÕES",
                List.of(
                        "",
                        "&7Sua carteira está com",
                        "&a" + formatCompactCoins(saldo),
                        "",
                        "&fReserva bancária: &a" + formatCompactCoins(reservaBancaria),
                        "",
                        "&7Receber pagamentos: " + (recebimentosAtivados ? "&aSim" : "&cNão"),
                        "",
                        "&eClique para " + (recebimentosAtivados ? "desativar" : "ativar") + " recebimentos"
                )
        ));

        inventory.setItem(SLOT_TRANSACOES, item(
                Material.PAPER,
                "&bTRANSAÇÕES",
                List.of(
                        "",
                        "&7Acompanhe tudo que entrou",
                        "&7e saiu da sua carteira.",
                        "",
                        "&8Envios, recebimentos e ajustes.",
                        "",
                        "&eClique para abrir o extrato"
                )
        ));

        inventory.setItem(SLOT_TOP, item(
                Material.BOOK,
                "&bTOP JOGADORES",
                List.of(
                        "",
                        "&7Veja quem domina a economia",
                        "&7e ocupa as maiores fortunas.",
                        "",
                        "&8Somente saldos acima de zero entram.",
                        "",
                        "&eClique para ver o ranking"
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
                    "&bMAGNATA",
                    List.of(
                            "",
                            "&7Ainda não existe um Magnata.",
                            "&8É necessário possuir saldo positivo."
                    )
            );
        }

        Map.Entry<UUID, Double> first = top.get(0);
        OfflinePlayer player = Bukkit.getOfflinePlayer(first.getKey());
        String name = player.getName() != null ? player.getName() : economia.getNomeConhecido(first.getKey());

        ItemStack stack = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&bMAGNATA"));
            meta.setLore(List.of(
                    "",
                    color("&7O jogador no topo da economia."),
                    "",
                    color("&fMagnata atual: &a" + name),
                    color("&fPatrimônio: &a" + formatCompactCoins(first.getValue())),
                    "",
                    color("&8Quem assumir o 1º lugar recebe a tag [$].")
            ));
            stack.setItemMeta(meta);
        }
        return stack;
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
