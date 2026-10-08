package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.shop.ShopCategory;
import com.coinseconomy.plugin.shop.ShopItem;
import com.coinseconomy.plugin.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopGUI {

    // Layout principal em 5 linhas para manter as categorias organizadas e o perfil centralizado no slot 40.
    private static final int MAIN_ROWS = 5;
    public static final int MAIN_SIZE = MAIN_ROWS * 9;
    public static final int CATEGORY_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 21;

    // Mantém a organização visual do menu principal:
    // categorias em duas linhas centrais e perfil centralizado na última linha.
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22};
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static final int SLOT_PROFILE = 40;
    public static final int SLOT_PREVIOUS = 47;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 51;

    private ShopGUI() {
    }

    public static Inventory main(Player player, EconomyManager economy, ShopManager shop) {
        ShopGUIHolder holder = new ShopGUIHolder(null, 0);
        Inventory inventory = Bukkit.createInventory(holder, MAIN_SIZE, "Loja de Coins");
        holder.setInventory(inventory);

        inventory.setItem(SLOT_PROFILE, profile(player, economy, shop));

        List<ShopCategory> categories = shop.getCategories();
        for (int i = 0; i < Math.min(CATEGORY_SLOTS.length, categories.size()); i++) {
            ShopCategory category = categories.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(category.description().stream().map(line -> "&f" + line).toList());
            lore.add("");
            lore.add(category.color() + "Clique para acessar");
            inventory.setItem(CATEGORY_SLOTS[i], item(
                    category.icon(),
                    category.color() + category.displayName().toUpperCase(Locale.ROOT),
                    lore
            ));
        }

        return inventory;
    }

    public static Inventory category(Player player, EconomyManager economy, ShopManager shop, String categoryId, int page) {
        ShopCategory category = shop.getCategory(categoryId);
        if (category == null) return main(player, economy, shop);

        int totalPages = Math.max(1, (int) Math.ceil(category.items().size() / (double) ITEMS_PER_PAGE));
        int validPage = Math.max(0, Math.min(page, totalPages - 1));
        String suffix = totalPages > 1 ? " (" + (validPage + 1) + "/" + totalPages + ")" : "";

        ShopGUIHolder holder = new ShopGUIHolder(category.id(), validPage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                CATEGORY_SIZE,
                "Loja • " + category.displayName() + suffix
        );
        holder.setInventory(inventory);

        int start = validPage * ITEMS_PER_PAGE;
        int quantity = Math.min(ITEMS_PER_PAGE, Math.max(0, category.items().size() - start));
        for (int i = 0; i < quantity; i++) {
            ShopItem shopItem = category.items().get(start + i);
            inventory.setItem(ITEM_SLOTS[i], shopItem(shop, shopItem));
        }

        if (validPage > 0) {
            inventory.setItem(SLOT_PREVIOUS, item(
                    Material.ARROW,
                    "&aAnterior",
                    List.of("", "&7Clique para voltar à página anterior.")
            ));
        }

        inventory.setItem(SLOT_BACK, item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Clique para voltar à Loja de Coins.")
        ));

        if (validPage + 1 < totalPages) {
            inventory.setItem(SLOT_NEXT, item(
                    Material.ARROW,
                    "&aPróxima",
                    List.of("", "&7Clique para avançar para a próxima página.")
            ));
        }

        return inventory;
    }

    public static int itemIndexFromSlot(int slot) {
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    public static int categoryIndexFromSlot(int slot) {
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (CATEGORY_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private static ItemStack profile(Player player, EconomyManager economy, ShopManager shop) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(color("&b" + player.getName()));
            meta.setLore(List.of(
                    "",
                    color("&7Informações da sua conta"),
                    "",
                    color("&f• Saldo atual: &a" + compact(economy.getSaldo(player.getUniqueId()))),
                    "",
                    color("&8Compras e vendas usam sua carteira de Coins.")
            ));
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack shopItem(ShopManager shop, ShopItem shopItem) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (shopItem.canBuy()) {
            lore.add("&fPreço de compra: &a$" + compactNumber(shop.finalBuyPrice(shopItem)));
        } else {
            lore.add("&fPreço de compra: &8Indisponível");
        }

        if (shopItem.canSell()) {
            lore.add("&fPreço de venda: &c$" + compactNumber(shop.finalSellPrice(shopItem)));
        }

        lore.add("");
        lore.add("&7Opções disponíveis:");
        if (shopItem.canBuy()) {
            lore.add("&a• &fClique esquerdo: &aComprar");
        }
        if (shopItem.canSell()) {
            lore.add("&c• &fClique direito: &cVender");
        }

        return item(shopItem.material(), "&b" + shopItem.name(), lore);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(ShopGUI::color).toList());
            hideAdditionalTooltip(meta);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void hideAdditionalTooltip(ItemMeta meta) {
        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
        } catch (IllegalArgumentException ignored) {
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ITEM_SPECIFICS"));
            } catch (IllegalArgumentException ignoredToo) {
                // Compatibilidade com APIs que não expõem flags de tooltip adicional.
            }
        }
    }

    private static String percent(double value) {
        return String.format(Locale.of("pt", "BR"), "%.1f", value);
    }

    private static String compact(double value) {
        return "$" + compactNumber(value) + " Coins";
    }

    private static String compactNumber(double value) {
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
        return number + suffix;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
