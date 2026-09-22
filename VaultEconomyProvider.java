package com.coinseconomy.plugin.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

/**
 * Implementação da interface {@link Economy} do Vault, delegando todas as
 * operações para o {@link EconomyManager}. É esta classe que é registrada
 * no ServicesManager do Bukkit, permitindo que qualquer plugin compatível
 * com o Vault (lojas, ranks, minigames, etc.) use os coins deste plugin.
 */
public class VaultEconomyProvider implements Economy {

    private final EconomyManager manager;

    public VaultEconomyProvider(EconomyManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "CoinsEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return manager.formatar(amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public String currencyNameSingular() {
        return "Coin";
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return manager.temConta(player.getUniqueId());
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return manager.getSaldo(player.getUniqueId());
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return manager.tem(player.getUniqueId(), amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, manager.getSaldo(player.getUniqueId()),
                    EconomyResponse.ResponseType.FAILURE, "Não é possível sacar uma quantidade negativa.");
        }

        boolean sucesso = manager.sacar(player, amount);
        double saldoAtual = manager.getSaldo(player.getUniqueId());

        if (!sucesso) {
            return new EconomyResponse(0, saldoAtual, EconomyResponse.ResponseType.FAILURE, "Saldo insuficiente.");
        }
        return new EconomyResponse(amount, saldoAtual, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, manager.getSaldo(player.getUniqueId()),
                    EconomyResponse.ResponseType.FAILURE, "Não é possível depositar uma quantidade negativa.");
        }

        manager.depositar(player, amount);
        double saldoAtual = manager.getSaldo(player.getUniqueId());
        return new EconomyResponse(amount, saldoAtual, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    // ---------------------------------------------------------------
    // Bancos: este plugin não implementa contas bancárias compartilhadas.
    // ---------------------------------------------------------------

    private EconomyResponse naoSuportado() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "CoinsEconomy não possui suporte a bancos.");
    }

    @Override
    @Deprecated
    public EconomyResponse createBank(String name, String player) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return naoSuportado();
    }

    @Override
    @Deprecated
    public EconomyResponse isBankOwner(String name, String playerName) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return naoSuportado();
    }

    @Override
    @Deprecated
    public EconomyResponse isBankMember(String name, String playerName) {
        return naoSuportado();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return naoSuportado();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (manager.temConta(player.getUniqueId())) {
            return false;
        }
        manager.criarConta(player);
        return true;
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
