package com.coinseconomy.plugin.api;

import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API pública do EconomiaPlus.
 *
 * Outros plugins podem obter esta implementação pelo ServicesManager:
 *
 * EconomyApi api = Bukkit.getServicesManager().load(EconomyApi.class);
 */
public interface EconomyApi {

    boolean temConta(UUID uuid);

    void criarConta(OfflinePlayer jogador);

    double getSaldo(UUID uuid);

    boolean tem(UUID uuid, double quantidade);

    void depositar(OfflinePlayer jogador, double quantidade);

    boolean sacar(OfflinePlayer jogador, double quantidade);

    void definirSaldo(OfflinePlayer jogador, double quantidade);

    String getNomeConhecido(UUID uuid);

    List<Map.Entry<UUID, Double>> getTop(int quantidade);

    String formatar(double valor);
}
