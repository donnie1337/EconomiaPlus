package com.coinseconomy.plugin.wallet;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Preferências persistentes da carteira de Coins. */
public final class WalletSettingsManager {

    private final CoinsEconomyPlugin plugin;
    private final File dataFile;
    private final Map<UUID, Boolean> receivingEnabled = new ConcurrentHashMap<>();

    public WalletSettingsManager(CoinsEconomyPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "wallet-settings.yml");
    }

    public void load() {
        if (!dataFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        for (String raw : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                receivingEnabled.put(uuid, data.getBoolean(raw + ".receber-pagamentos", true));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("UUID inválido em wallet-settings.yml: " + raw);
            }
        }
    }

    public synchronized void save() {
        FileConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : receivingEnabled.entrySet()) {
            data.set(entry.getKey() + ".receber-pagamentos", entry.getValue());
        }

        try {
            File parent = dataFile.getParentFile();
            if (parent != null) parent.mkdirs();
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar wallet-settings.yml", exception);
        }
    }

    public boolean canReceive(UUID uuid) {
        return receivingEnabled.getOrDefault(uuid, true);
    }

    public boolean toggleReceiving(UUID uuid) {
        boolean enabled = !canReceive(uuid);
        receivingEnabled.put(uuid, enabled);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
        return enabled;
    }
}
