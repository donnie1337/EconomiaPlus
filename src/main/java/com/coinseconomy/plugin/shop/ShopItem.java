package com.coinseconomy.plugin.shop;

import org.bukkit.Material;

public record ShopItem(Material material, String name, double buyPrice, double sellPrice) {
    public boolean canBuy() {
        return Double.isFinite(buyPrice) && buyPrice > 0.0D;
    }

    public boolean canSell() {
        return Double.isFinite(sellPrice) && sellPrice > 0.0D;
    }
}
