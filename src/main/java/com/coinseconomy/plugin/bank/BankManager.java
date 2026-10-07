package com.coinseconomy.plugin.bank;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Armazena saldo bancário e controle diário de saques. */
public final class BankManager {

    private final CoinsEconomyPlugin plugin;
    private final File dataFile;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> withdrawCounts = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDate> withdrawDates = new ConcurrentHashMap<>();
    private final Map<UUID, List<BankTransaction>> history = new ConcurrentHashMap<>();

    public BankManager(CoinsEconomyPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "bank.yml");
    }

    public void load() {
        if (!dataFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = data.getConfigurationSection("jogadores");
        if (players == null) return;

        for (String raw : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                balances.put(uuid, Math.max(0.0D, data.getDouble("jogadores." + raw + ".saldo", 0.0D)));
                withdrawCounts.put(uuid, Math.max(0, data.getInt("jogadores." + raw + ".saques-dia", 0)));

                String date = data.getString("jogadores." + raw + ".data-saques", "");
                if (date != null && !date.isBlank()) {
                    try {
                        withdrawDates.put(uuid, LocalDate.parse(date));
                    } catch (RuntimeException ignored) {
                    }
                }

                List<?> savedHistory = data.getList("jogadores." + raw + ".historico", List.of());
                List<BankTransaction> parsedHistory = new ArrayList<>();
                for (Object value : savedHistory) {
                    if (!(value instanceof Map<?, ?> map)) continue;
                    try {
                        BankTransaction.Type type = BankTransaction.Type.valueOf(String.valueOf(map.get("tipo")));
                        double amount = map.get("valor") instanceof Number number
                                ? number.doubleValue()
                                : Double.parseDouble(String.valueOf(map.get("valor")));
                        long timestamp = map.get("data") instanceof Number number
                                ? number.longValue()
                                : Long.parseLong(String.valueOf(map.get("data")));
                        if (Double.isFinite(amount) && amount > 0.0D) {
                            parsedHistory.add(new BankTransaction(type, amount, timestamp));
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
                parsedHistory.sort(Comparator.comparingLong(BankTransaction::timestamp).reversed());
                history.put(uuid, parsedHistory);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("UUID inválido em bank.yml: " + raw);
            }
        }
    }

    public synchronized void save() {
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            UUID uuid = entry.getKey();
            String path = "jogadores." + uuid;
            data.set(path + ".saldo", entry.getValue());
            data.set(path + ".saques-dia", getWithdrawCount(uuid));
            LocalDate date = withdrawDates.get(uuid);
            data.set(path + ".data-saques", date == null ? null : date.toString());

            List<Map<String, Object>> savedHistory = new ArrayList<>();
            for (BankTransaction transaction : history.getOrDefault(uuid, List.of())) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("tipo", transaction.type().name());
                entry.put("valor", transaction.amount());
                entry.put("data", transaction.timestamp());
                savedHistory.add(entry);
            }
            data.set(path + ".historico", savedHistory);
        }

        try {
            File parent = dataFile.getParentFile();
            if (parent != null) parent.mkdirs();
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar bank.yml", ex);
        }
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0D);
    }

    public synchronized void deposit(UUID uuid, double amount) {
        if (uuid == null || !Double.isFinite(amount) || amount <= 0.0D) return;
        balances.merge(uuid, amount, Double::sum);
        addHistory(uuid, new BankTransaction(BankTransaction.Type.DEPOSIT, amount, System.currentTimeMillis()));
        saveLater();
    }

    public synchronized boolean withdraw(UUID uuid, double amount) {
        if (uuid == null || !Double.isFinite(amount) || amount <= 0.0D) return false;
        double current = getBalance(uuid);
        if (current + 0.0000001D < amount) return false;

        balances.put(uuid, Math.max(0.0D, current - amount));
        incrementWithdraw(uuid);
        addHistory(uuid, new BankTransaction(BankTransaction.Type.WITHDRAW, amount, System.currentTimeMillis()));
        saveLater();
        return true;
    }

    public List<BankTransaction> getHistory(UUID uuid) {
        List<BankTransaction> list = history.get(uuid);
        if (list == null || list.isEmpty()) return List.of();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    private void addHistory(UUID uuid, BankTransaction transaction) {
        List<BankTransaction> list = history.computeIfAbsent(uuid, ignored -> java.util.Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(0, transaction);
            if (list.size() > 500) {
                list.subList(500, list.size()).clear();
            }
        }
    }

    public int getWithdrawCount(UUID uuid) {
        resetDailyCounterIfNeeded(uuid);
        return withdrawCounts.getOrDefault(uuid, 0);
    }

    public boolean canWithdraw(UUID uuid, int dailyLimit) {
        return dailyLimit <= 0 || getWithdrawCount(uuid) < dailyLimit;
    }

    private void incrementWithdraw(UUID uuid) {
        resetDailyCounterIfNeeded(uuid);
        withdrawCounts.merge(uuid, 1, Integer::sum);
        withdrawDates.put(uuid, LocalDate.now());
    }

    private void resetDailyCounterIfNeeded(UUID uuid) {
        LocalDate today = LocalDate.now();
        LocalDate stored = withdrawDates.get(uuid);
        if (!today.equals(stored)) {
            withdrawCounts.put(uuid, 0);
            withdrawDates.put(uuid, today);
        }
    }

    private void saveLater() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }
}
