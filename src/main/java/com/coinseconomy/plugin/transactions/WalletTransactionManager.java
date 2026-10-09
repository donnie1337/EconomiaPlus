package com.coinseconomy.plugin.transactions;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.util.AtomicYamlSaver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class WalletTransactionManager {

    private final CoinsEconomyPlugin plugin;
    private final File dataFile;
    private final Map<UUID, List<WalletTransaction>> history = new ConcurrentHashMap<>();

    public WalletTransactionManager(CoinsEconomyPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "wallet-transactions.yml");
    }

    public void load() {
        if (!dataFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = data.getConfigurationSection("jogadores");
        if (players == null) return;

        for (String raw : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                List<?> saved = data.getList("jogadores." + raw, List.of());
                List<WalletTransaction> parsed = new ArrayList<>();

                for (Object value : saved) {
                    if (!(value instanceof Map<?, ?> map)) continue;
                    try {
                        WalletTransaction.Type type = WalletTransaction.Type.valueOf(String.valueOf(map.get("tipo")));
                        double amount = map.get("valor") instanceof Number number
                                ? number.doubleValue()
                                : Double.parseDouble(String.valueOf(map.get("valor")));
                        long timestamp = map.get("data") instanceof Number number
                                ? number.longValue()
                                : Long.parseLong(String.valueOf(map.get("data")));
                        Object detailValue = map.get("detalhe");
                        String detail = detailValue == null ? "" : String.valueOf(detailValue);

                        if (Double.isFinite(amount) && amount > 0.0D) {
                            parsed.add(new WalletTransaction(type, amount, timestamp, detail));
                        }
                    } catch (RuntimeException ignored) {
                    }
                }

                parsed.sort(Comparator.comparingLong(WalletTransaction::timestamp).reversed());
                history.put(uuid, java.util.Collections.synchronizedList(parsed));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("UUID inválido em wallet-transactions.yml: " + raw);
            }
        }
    }

    public synchronized void save() {
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, List<WalletTransaction>> playerEntry : history.entrySet()) {
            List<Map<String, Object>> saved = new ArrayList<>();
            List<WalletTransaction> list = playerEntry.getValue();

            synchronized (list) {
                for (WalletTransaction transaction : list) {
                    Map<String, Object> transactionEntry = new LinkedHashMap<>();
                    transactionEntry.put("tipo", transaction.type().name());
                    transactionEntry.put("valor", transaction.amount());
                    transactionEntry.put("data", transaction.timestamp());
                    transactionEntry.put("detalhe", transaction.detail());
                    saved.add(transactionEntry);
                }
            }

            data.set("jogadores." + playerEntry.getKey(), saved);
        }

        try {
            File parent = dataFile.getParentFile();
            if (parent != null) parent.mkdirs();
            AtomicYamlSaver.save(data, dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar wallet-transactions.yml", exception);
        }
    }

    public void record(UUID uuid, WalletTransaction.Type type, double amount, String detail) {
        if (uuid == null || type == null || !Double.isFinite(amount) || amount <= 0.0D) return;

        List<WalletTransaction> list = history.computeIfAbsent(
                uuid,
                ignored -> java.util.Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (list) {
            list.add(0, new WalletTransaction(
                    type,
                    amount,
                    System.currentTimeMillis(),
                    detail == null ? "" : detail
            ));

            if (list.size() > 500) {
                list.subList(500, list.size()).clear();
            }
        }

    }

    public List<WalletTransaction> getHistory(UUID uuid) {
        List<WalletTransaction> list = history.get(uuid);
        if (list == null || list.isEmpty()) return List.of();

        synchronized (list) {
            return new ArrayList<>(list);
        }
    }
}
