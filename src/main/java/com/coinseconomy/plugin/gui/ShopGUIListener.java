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
            buyUpTo(player, item, holder, 64);
        } else if (event.getClick() == ClickType.RIGHT) {
            sell(player, item, holder, 1);
        } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
            sellUpTo(player, item, holder, 64);
        }
    }

    private void buy(Player player, ShopItem item, ShopGUIHolder holder, int amount) {
        if (!item.canBuy() || amount <= 0) return;
        int space = getAvailableSpace(player, item.material(), amount);
        if (space < amount) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui espaço suficiente no inventário."));
            return;
        }

        double price = shop.finalBuyPrice(item) * amount;
        if (!Double.isFinite(price) || price <= 0.0D) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fPreço inválido. Avise a administração."));
            return;
        }
        if (!economy.tem(player.getUniqueId(), price)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui Coins suficientes."));
            return;
        }

        completeBuy(player, item, holder, amount, price);
    }

    private void buyUpTo(Player player, ShopItem item, ShopGUIHolder holder, int limit) {
        if (!item.canBuy() || limit <= 0) return;

        double unitPrice = shop.finalBuyPrice(item);
        if (!Double.isFinite(unitPrice) || unitPrice <= 0.0D) return;

        double balance = economy.getSaldo(player.getUniqueId());
        int affordable = (int) Math.min(limit, Math.floor((balance + 0.0000001D) / unitPrice));
        int space = getAvailableSpace(player, item.material(), limit);
        int amount = Math.min(affordable, space);

        if (amount <= 0) {
            if (affordable <= 0) {
                player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui Coins suficientes."));
            } else {
                player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui espaço suficiente no inventário."));
            }
            return;
        }

        completeBuy(player, item, holder, amount, unitPrice * amount);
    }

    private void completeBuy(Player player, ShopItem item, ShopGUIHolder holder, int amount, double price) {
        ItemStack[] inventoryBefore = cloneStorage(player.getInventory().getStorageContents());
        if (!economy.sacar(player, price)) return;

        // Persiste o débito antes de entregar o item. Em uma queda abrupta,
        // isso evita que o jogador fique com item e Coins restaurados.
        economy.save();

        int remaining = amount;
        int maxStack = item.material().getMaxStackSize();
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack(item.material(), stackAmount));
            if (!leftovers.isEmpty()) {
                player.getInventory().setStorageContents(inventoryBefore);
                economy.depositar(player, price);
                economy.save();
                player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fA compra não pôde ser concluída e foi revertida."));
                return;
            }
            remaining -= stackAmount;
        }

        player.saveData();
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
        if (countMaterial(player, item.material()) < amount) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui este item para vender."));
            return;
        }

        completeSell(player, item, holder, amount);
    }

    private void sellUpTo(Player player, ShopItem item, ShopGUIHolder holder, int limit) {
        if (!item.canSell() || limit <= 0) return;

        int amount = Math.min(limit, countMaterial(player, item.material()));
        if (amount <= 0) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fVocê não possui este item para vender."));
            return;
        }

        completeSell(player, item, holder, amount);
    }

    private void completeSell(Player player, ShopItem item, ShopGUIHolder holder, int amount) {
        double price = shop.finalSellPrice(item) * amount;
        if (!Double.isFinite(price) || price <= 0.0D || !economy.canDeposit(player.getUniqueId(), price)) {
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fA venda não pôde ser concluída com segurança."));
            return;
        }

        ItemStack[] inventoryBefore = cloneStorage(player.getInventory().getStorageContents());
        int removed = removeAmount(player, item.material(), amount);
        if (removed != amount) {
            player.getInventory().setStorageContents(inventoryBefore);
            player.sendMessage(color("&c&lʟᴏᴊᴀ &8• &fOs itens mudaram durante a venda. Operação cancelada."));
            return;
        }

        // Persiste a remoção antes de creditar Coins para impedir rollback lucrativo.
        player.saveData();
        economy.depositar(player, price);
        economy.save();

        plugin.getWalletTransactionManager().record(
                player.getUniqueId(),
                WalletTransaction.Type.SHOP_SELL,
                price,
                transactionDetail(item, amount)
        );
        player.openInventory(ShopGUI.category(player, economy, shop, holder.getCategoryId(), holder.getPage()));
    }

    private int getAvailableSpace(Player player, Material material, int limit) {
        int space = 0;
        int maxStack = material.getMaxStackSize();
        ItemStack probe = new ItemStack(material, 1);

        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current == null || current.getType().isAir()) {
                space += maxStack;
            } else if (current.isSimilar(probe) && current.getAmount() < current.getMaxStackSize()) {
                space += current.getMaxStackSize() - current.getAmount();
            }

            if (space >= limit) return limit;
        }
        return Math.min(space, limit);
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack current : player.getInventory().getStorageContents()) {
            if (current != null && current.getType() == material && isPlainItem(current)) {
                total += current.getAmount();
            }
        }
        return total;
    }

    private int removeAmount(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack current = contents[i];
            if (current == null || current.getType() != material || !isPlainItem(current)) continue;

            int removed = Math.min(current.getAmount(), remaining);
            int newAmount = current.getAmount() - removed;
            remaining -= removed;

            if (newAmount <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                current.setAmount(newAmount);
            }
        }
        return amount - remaining;
    }

    private ItemStack[] cloneStorage(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copy;
    }

    private boolean isPlainItem(ItemStack item) {
        return item != null && !item.hasItemMeta() && item.getEnchantments().isEmpty();
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
