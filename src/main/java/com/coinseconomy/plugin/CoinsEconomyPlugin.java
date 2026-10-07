package com.coinseconomy.plugin;

import com.coinseconomy.plugin.commands.CobrarCommand;
import com.coinseconomy.plugin.commands.CoinsCommand;
import com.coinseconomy.plugin.commands.PagarCommand;
import com.coinseconomy.plugin.commands.TopCoinsCommand;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.api.EconomyApi;
import org.bukkit.plugin.ServicePriority;
import com.coinseconomy.plugin.gui.TopCoinsGUIListener;
import com.coinseconomy.plugin.gui.CoinsWalletGUIListener;
import com.coinseconomy.plugin.listeners.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin de economia independente, com API própria registrada no
 * ServicesManager do Bukkit. A arquitetura segue o padrão provider/service
 * popularizado pelo Vault, mas sem depender do Vault.
 */
public final class CoinsEconomyPlugin extends JavaPlugin {

    private static CoinsEconomyPlugin instance;

    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;

        migrarPastaLegada();
        saveDefaultConfig();

        this.economyManager = new EconomyManager(this);
        this.economyManager.load();

        registrarApi();
        registrarComandos();
        registrarEventos();
        agendarSalvamentoAutomatico();

        getLogger().info("EconomiaPlus foi ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.save();
        }
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("EconomiaPlus foi desativado. Dados salvos em disco.");
    }

    private void migrarPastaLegada() {
        java.io.File novaPasta = getDataFolder();
        java.io.File pastaPlugins = novaPasta.getParentFile();
        if (pastaPlugins == null) return;

        java.io.File pastaAntiga = new java.io.File(pastaPlugins, "CoinsEconomy");
        if (!pastaAntiga.isDirectory()) return;

        if (!novaPasta.exists() && !novaPasta.mkdirs()) {
            getLogger().warning("Não foi possível criar a pasta EconomiaPlus para migrar os dados antigos.");
            return;
        }

        migrarArquivoLegado(pastaAntiga, novaPasta, "config.yml");
        migrarArquivoLegado(pastaAntiga, novaPasta, "data.yml");
    }

    private void migrarArquivoLegado(java.io.File origemPasta, java.io.File destinoPasta, String nome) {
        java.io.File origem = new java.io.File(origemPasta, nome);
        java.io.File destino = new java.io.File(destinoPasta, nome);
        if (!origem.isFile() || destino.exists()) return;

        try {
            java.nio.file.Files.copy(origem.toPath(), destino.toPath(),
                    java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
            getLogger().info("Arquivo legado migrado de CoinsEconomy para EconomiaPlus: " + nome);
        } catch (java.io.IOException exception) {
            getLogger().log(java.util.logging.Level.SEVERE,
                    "Não foi possível migrar " + nome + " de CoinsEconomy para EconomiaPlus.", exception);
        }
    }

    private void registrarApi() {
        Bukkit.getServicesManager().register(
                EconomyApi.class,
                economyManager,
                this,
                ServicePriority.Highest
        );
        getLogger().info("API própria do EconomiaPlus registrada com sucesso.");
    }

    private void registrarComandos() {
        CoinsCommand coinsCommand = new CoinsCommand(this, economyManager);
        getCommand("coins").setExecutor(coinsCommand);
        getCommand("coins").setTabCompleter(coinsCommand);

        PagarCommand pagarCommand = new PagarCommand(this, economyManager);
        getCommand("pagar").setExecutor(pagarCommand);
        getCommand("pagar").setTabCompleter(pagarCommand);

        CobrarCommand cobrarCommand = new CobrarCommand(this, economyManager);
        getCommand("cobrar").setExecutor(cobrarCommand);
        getCommand("cobrar").setTabCompleter(cobrarCommand);

        TopCoinsCommand topCoinsCommand = new TopCoinsCommand(this, economyManager);
        getCommand("topcoins").setExecutor(topCoinsCommand);
    }

    private void registrarEventos() {
        Bukkit.getPluginManager().registerEvents(new TopCoinsGUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new CoinsWalletGUIListener(this, economyManager), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(economyManager), this);
    }

    private void agendarSalvamentoAutomatico() {
        long intervalo = getConfig().getLong("salvamento-automatico-ticks", 6000L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                economyManager::save,
                intervalo,
                intervalo
        );
    }

    public static CoinsEconomyPlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
