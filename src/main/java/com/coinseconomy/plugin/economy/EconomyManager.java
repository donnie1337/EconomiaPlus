package com.coinseconomy.plugin.economy;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Responsável por guardar e manipular o saldo de coins de cada jogador.
 * O armazenamento é feito em um único arquivo "data.yml", de forma
 * simples, inspirado no handler "file" do BetterEconomy.
 */
public class EconomyManager {

    private final CoinsEconomyPlugin plugin;
    private final File dataFile;

    private final Map<UUID, Double> saldos = new ConcurrentHashMap<>();
    private final Map<UUID, String> nomesConhecidos = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private double saldoInicial;

    public EconomyManager(CoinsEconomyPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    /**
     * Carrega os saldos salvos em disco. Deve ser chamado uma vez no onEnable.
     */
    public void load() {
        this.saldoInicial = plugin.getConfig().getDouble("saldo-inicial", 0.0);

        if (!dataFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                dataFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Não foi possível criar o arquivo data.yml", e);
            }
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        lock.writeLock().lock();
        try {
            saldos.clear();
            nomesConhecidos.clear();

            ConfigurationSection secaoJogadores = data.getConfigurationSection("jogadores");
            if (secaoJogadores != null) {
                for (String uuidTexto : secaoJogadores.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidTexto);
                        double saldo = data.getDouble("jogadores." + uuidTexto + ".saldo", saldoInicial);
                        String nome = data.getString("jogadores." + uuidTexto + ".nome", "Desconhecido");
                        saldos.put(uuid, saldo);
                        nomesConhecidos.put(uuid, Objects.requireNonNullElse(nome, "Desconhecido"));
                    } catch (IllegalArgumentException ignorado) {
                        plugin.getLogger().warning("UUID inválido encontrado em data.yml: " + uuidTexto);
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Salva todos os saldos em disco. Seguro para ser chamado de forma
     * assíncrona (usa apenas as próprias estruturas em memória).
     */
    public void save() {
        FileConfiguration data = new YamlConfiguration();

        lock.readLock().lock();
        try {
            for (Map.Entry<UUID, Double> entrada : saldos.entrySet()) {
                String caminho = "jogadores." + entrada.getKey();
                data.set(caminho + ".saldo", entrada.getValue());
                data.set(caminho + ".nome", nomesConhecidos.getOrDefault(entrada.getKey(), "Desconhecido"));
            }
        } finally {
            lock.readLock().unlock();
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar o arquivo data.yml", e);
        }
    }

    private void garantirConta(UUID uuid, String nomeAtual) {
        saldos.putIfAbsent(uuid, saldoInicial);
        if (nomeAtual != null && !nomeAtual.isEmpty()) {
            nomesConhecidos.put(uuid, nomeAtual);
        } else {
            nomesConhecidos.putIfAbsent(uuid, "Desconhecido");
        }
    }

    public boolean temConta(UUID uuid) {
        return saldos.containsKey(uuid);
    }

    public void criarConta(OfflinePlayer jogador) {
        garantirConta(jogador.getUniqueId(), jogador.getName());
    }

    public double getSaldo(UUID uuid) {
        return saldos.getOrDefault(uuid, saldoInicial);
    }

    public boolean tem(UUID uuid, double quantidade) {
        return getSaldo(uuid) >= quantidade;
    }

    public void depositar(OfflinePlayer jogador, double quantidade) {
        UUID uuid = jogador.getUniqueId();
        garantirConta(uuid, jogador.getName());
        saldos.merge(uuid, quantidade, Double::sum);
    }

    /**
     * Tenta sacar uma quantidade do saldo do jogador.
     *
     * @return true se havia saldo suficiente e o saque foi feito.
     */
    public boolean sacar(OfflinePlayer jogador, double quantidade) {
        UUID uuid = jogador.getUniqueId();
        garantirConta(uuid, jogador.getName());

        double atual = getSaldo(uuid);
        if (atual < quantidade) {
            return false;
        }

        saldos.put(uuid, atual - quantidade);
        return true;
    }

    public void definirSaldo(OfflinePlayer jogador, double quantidade) {
        UUID uuid = jogador.getUniqueId();
        garantirConta(uuid, jogador.getName());
        saldos.put(uuid, quantidade);
    }

    public String getNomeConhecido(UUID uuid) {
        return nomesConhecidos.getOrDefault(uuid, "Desconhecido");
    }

    /**
     * Retorna os N jogadores com mais coins, do maior para o menor saldo.
     */
    public List<Map.Entry<UUID, Double>> getTop(int quantidade) {
        List<Map.Entry<UUID, Double>> lista = new ArrayList<>(saldos.entrySet());
        lista.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        if (lista.size() > quantidade) {
            return new ArrayList<>(lista.subList(0, quantidade));
        }
        return lista;
    }

    /**
     * Formata um valor no padrão "1.234,56 coins".
     */
    public String formatar(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);

        String sufixo = Math.abs(valor - 1.0) < 0.0001
                ? plugin.getConfig().getString("moeda.singular", "coin")
                : plugin.getConfig().getString("moeda.plural", "coins");

        return formato.format(valor) + " " + sufixo;
    }
}
