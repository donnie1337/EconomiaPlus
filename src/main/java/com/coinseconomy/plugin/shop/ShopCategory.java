package com.coinseconomy.plugin.shop;

import org.bukkit.Material;

import java.util.List;

public record ShopCategory(
        String id,
        String displayName,
        Material icon,
        String color,
        List<String> description,
        List<ShopItem> items
) {
}
