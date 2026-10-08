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

import java.util.HashMap;
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
            buy(player, item, holder);
        } else if (event.getClick() == ClickType.RIGHT) {
            sell(player, item, holder);
        }
    }

    private void buy(Player player, ShopItem item, ShopGUIHolder holder) {
        if (!item.canBuy()) return;
        double price = shop.finalBuyPrice(item);

        if (!economy.tem(player.getUniqueId(), price)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui Coins suficientes."));
            return;
        }

        ItemStack stack = new ItemStack(item.material(), 1);
        if (!canFit(player, stack)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fSeu inventário está cheio."));
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
                item.name()
        );
        player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage()));
    }

    private void sell(Player player, ShopItem item, ShopGUIHolder holder) {
        if (!item.canSell()) return;
        if (!player.getInventory().containsAtLeast(new ItemStack(item.material()), 1)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui este item para vender."));
            return;
        }

        removeOne(player, item.material());
        double price = shop.finalSellPrice(item);
        economy.depositar(player, price);

        plugin.getWalletTransactionManager().record(
                player.getUniqueId(),
                WalletTransaction.Type.SHOP_SELL,
                price,
                item.name()
        );
        player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage()));
    }

    private boolean canFit(Player player, ItemStack item) {
        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current == null || current.getType().isAir()) return true;
            if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) return true;
        }
        return false;
    }

    private void removeOne(Player player, Material material) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack current = contents[i];
            if (current == null || current.getType() != material) continue;

            if (current.getAmount() <= 1) {
                player.getInventory().setItem(i, null);
            } else {
                current.setAmount(current.getAmount() - 1);
            }
            return;
        }
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
