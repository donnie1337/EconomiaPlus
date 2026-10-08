package com.coinseconomy.plugin.gui;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.shop.ShopCategory;
import com.coinseconomy.plugin.shop.ShopItem;
import com.coinseconomy.plugin.shop.ShopManager;
import com.coinseconomy.plugin.transactions.WalletTransaction;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class ShopGUIListener implements Listener {

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economy;
    private final ShopManager shop;

    public ShopGUIListener(CoinsEconomyPlugin plugin, EconomyManager economy, ShopManager shop) {
        this.plugin = plugin;
        this.economy = economy;
        this.shop = shop;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopGUIHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder.isMain()) {
            int index = ShopGUI.categoryIndexFromSlot(slot);
            if (index < 0) return;

            var categories = shop.getCategories();
            if (index >= categories.size()) return;
            player.openInventory(ShopGUI.category(player, economy, shop, categories.get(index).id(), 0));
            return;
        }

        if (slot == ShopGUI.SLOT_BACK) {
            player.openInventory(ShopGUI.main(player, economy, shop));
            return;
        }

        if (slot == ShopGUI.SLOT_PREVIOUS && holder.getPage() > 0) {
            player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage() - 1));
            return;
        }

        if (slot == ShopGUI.SLOT_NEXT) {
            player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage() + 1));
            return;
        }

        int localIndex = ShopGUI.itemIndexFromSlot(slot);
        if (localIndex < 0) return;

        ShopCategory category = shop.getCategory(holder.getCategoryId());
        if (category == null) return;

        int index = holder.getPage() * ShopGUI.ITEMS_PER_PAGE + localIndex;
        if (index < 0 || index >= category.items().size()) return;

        ShopItem item = category.items().get(index);
        if (event.getClick() == ClickType.LEFT) {
            buy(player, item, holder, 1);
        } else if (event.getClick() == ClickType.SHIFT_LEFT) {
            buy(player, item, holder, 64);
        } else if (event.getClick() == ClickType.RIGHT) {
            sell(player, item, holder, 1);
        } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
            sell(player, item, holder, 64);
        }
    }

    private void buy(Player player, ShopItem item, ShopGUIHolder holder, int amount) {
        if (!item.canBuy() || amount <= 0) return;
        double price = shop.finalBuyPrice(item) * amount;

        if (!economy.tem(player.getUniqueId(), price)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui Coins suficientes."));
            return;
        }

        ItemStack stack = new ItemStack(item.material(), amount);
        if (!canFit(player, stack)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui espaço suficiente no inventário."));
            return;
        }

        if (!economy.sacar(player, price)) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            economy.depositar(player, price);
            return;
        }

        plugin.getWalletTransactionManager().record(
                player.getUniqueId(),
                WalletTransaction.Type.SHOP_BUY,
                price,
                transactionDetail(item, amount)
        );
        player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage()));
    }

    private void sell(Player player, ShopItem item, ShopGUIHolder holder, int amount) {
        if (!item.canSell() || amount <= 0) return;
        if (!player.getInventory().containsAtLeast(new ItemStack(item.material()), amount)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui " + amount + "x deste item para vender."));
            return;
        }

        removeAmount(player, item.material(), amount);
        double price = shop.finalSellPrice(item) * amount;
        economy.depositar(player, price);

        plugin.getWalletTransactionManager().record(
                player.getUniqueId(),
                WalletTransaction.Type.SHOP_SELL,
                price,
                transactionDetail(item, amount)
        );
        player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage()));
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();

        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current == null || current.getType().isAir()) {
                remaining -= maxStack;
            } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                remaining -= current.getMaxStackSize() - current.getAmount();
            }

            if (remaining <= 0) return true;
        }
        return false;
    }

    private void removeAmount(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack current = contents[i];
            if (current == null || current.getType() != material) continue;

            int removed = Math.min(current.getAmount(), remaining);
            int newAmount = current.getAmount() - removed;
            remaining -= removed;

            if (newAmount <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                current.setAmount(newAmount);
            }
        }
    }

    private String transactionDetail(ShopItem item, int amount) {
        return amount > 1 ? item.name() + " x" + amount : item.name();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
