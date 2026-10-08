package com.coinseconomy.plugin.shop;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopManager {

    private final CoinsEconomyPlugin plugin;
    private final File file;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private double discountPercent;
    private double sellBonusPercent;

    public ShopManager(CoinsEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        categories.clear();
        discountPercent = Math.max(0.0D, config.getDouble("perfil.desconto-percentual", 0.0D));
        sellBonusPercent = Math.max(0.0D, config.getDouble("perfil.bonus-venda-percentual", 0.0D));

        ConfigurationSection section = config.getConfigurationSection("categorias");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String path = "categorias." + id;
            String displayName = config.getString(path + ".nome", id);
            String iconName = config.getString(path + ".icone", "CHEST");
            Material icon = Material.matchMaterial(iconName == null ? "CHEST" : iconName);
            if (icon == null) icon = Material.CHEST;

            String color = config.getString(path + ".cor", "&b");
            List<String> description = config.getStringList(path + ".descricao");
            List<ShopItem> items = new ArrayList<>();

            ConfigurationSection itemSection = config.getConfigurationSection(path + ".itens");
            if (itemSection != null) {
                for (String materialName : itemSection.getKeys(false)) {
                    Material material = Material.matchMaterial(materialName);
                    if (material == null || material.isAir()) {
                        plugin.getLogger().warning("Material inválido em shop.yml: " + materialName);
                        continue;
                    }

                    String itemPath = path + ".itens." + materialName;
                    String name = config.getString(itemPath + ".nome", prettify(material.name()));
                    double buy = config.getDouble(itemPath + ".compra", -1.0D);
                    double sell = config.getDouble(itemPath + ".venda", -1.0D);
                    items.add(new ShopItem(material, name, buy, sell));
                }
            }

            categories.put(id.toLowerCase(java.util.Locale.ROOT),
                    new ShopCategory(id, displayName, icon, color, List.copyOf(description), List.copyOf(items)));
        }
    }

    public List<ShopCategory> getCategories() {
        return List.copyOf(categories.values());
    }

    public ShopCategory getCategory(String id) {
        if (id == null) return null;
        return categories.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public double getSellBonusPercent() {
        return sellBonusPercent;
    }

    public double finalBuyPrice(ShopItem item) {
        double factor = Math.max(0.0D, 1.0D - (getDiscountPercent() / 100.0D));
        return Math.ceil(item.buyPrice() * factor);
    }

    public double finalSellPrice(ShopItem item) {
        double factor = 1.0D + (getSellBonusPercent() / 100.0D);
        return item.sellPrice() * factor;
    }

    private static String prettify(String raw) {
        String[] parts = raw.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
