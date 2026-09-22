package com.coinseconomy.plugin;

import com.coinseconomy.plugin.commands.CobrarCommand;
import com.coinseconomy.plugin.commands.CoinsCommand;
import com.coinseconomy.plugin.commands.PagarCommand;
import com.coinseconomy.plugin.commands.TopCoinsCommand;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.economy.VaultEconomyProvider;
import com.coinseconomy.plugin.gui.TopCoinsGUIListener;
import com.coinseconomy.plugin.listeners.JoinListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin de economia baseado na API do Vault (MilkBowl/Vault), com uma
 * implementação de armazenamento simples em arquivo, inspirada no
 * HSGamer/BetterEconomy.
 */
public final class CoinsEconomyPlugin extends JavaPlugin {

    private static CoinsEconomyPlugin instance;

    private EconomyManager economyManager;
    private VaultEconomyProvider vaultProvider;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.economyManager = new EconomyManager(this);
        this.economyManager.load();

        registrarNoVault();
        registrarComandos();
        registrarEventos();
        agendarSalvamentoAutomatico();

        getLogger().info("CoinsEconomy foi ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.save();
        }
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("CoinsEconomy foi desativado. Dados salvos em disco.");
    }

    private void registrarNoVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("O plugin Vault não foi encontrado! O CoinsEconomy vai continuar " +
                    "funcionando normalmente, mas nenhum outro plugin conseguirá acessar os coins " +
                    "através da API do Vault até que o Vault seja instalado.");
        }

        this.vaultProvider = new VaultEconomyProvider(economyManager);
        Bukkit.getServicesManager().register(
                Economy.class,
                vaultProvider,
                this,
                ServicePriority.Highest
        );
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
