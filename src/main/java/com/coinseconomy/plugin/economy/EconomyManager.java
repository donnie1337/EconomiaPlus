package com.coinseconomy.plugin.economy;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.api.EconomyApi;
import com.coinseconomy.plugin.util.AtomicYamlSaver;
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
public class EconomyManager implements EconomyApi {

    private final CoinsEconomyPlugin plugin;
    private final File dataFile;

    private final Map<UUID, Double> saldos = new ConcurrentHashMap<>();
    private final Map<UUID, String> nomesConhecidos = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private double saldoInicial;
    private volatile UUID magnataId;
    private volatile boolean magnataDirty = true;

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
                        double saldo = normalize(data.getDouble("jogadores." + uuidTexto + ".saldo", saldoInicial));
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

        magnataDirty = true;
    }

    /**
     * Salva todos os saldos em disco. Seguro para ser chamado de forma
     * assíncrona (usa apenas as próprias estruturas em memória).
     */
    public synchronized void save() {
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
            AtomicYamlSaver.save(data, dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar o arquivo data.yml", e);
        }
    }

    private boolean garantirConta(UUID uuid, String nomeAtual) {
        boolean criada = saldos.putIfAbsent(uuid, saldoInicial) == null;
        if (nomeAtual != null && !nomeAtual.isEmpty()) {
            nomesConhecidos.put(uuid, nomeAtual);
        } else {
            nomesConhecidos.putIfAbsent(uuid, "Desconhecido");
        }
        return criada;
    }

    public boolean temConta(UUID uuid) {
        return saldos.containsKey(uuid);
    }

    public void criarConta(OfflinePlayer jogador) {
        if (garantirConta(jogador.getUniqueId(), jogador.getName()) && saldoInicial > 0.0D) {
            magnataDirty = true;
        }
    }

    public double getSaldo(UUID uuid) {
        return normalize(saldos.getOrDefault(uuid, saldoInicial));
    }

    public synchronized boolean tem(UUID uuid, double quantidade) {
        return Double.isFinite(quantidade) && quantidade >= 0.0D
                && getSaldo(uuid) + 0.0000001D >= normalize(quantidade);
    }

    public synchronized boolean canDeposit(UUID uuid, double quantidade) {
        if (uuid == null || !Double.isFinite(quantidade) || quantidade <= 0.0D) return false;
        return Double.isFinite(getSaldo(uuid) + quantidade);
    }

    public synchronized void depositar(OfflinePlayer jogador, double quantidade) {
        if (jogador == null || !Double.isFinite(quantidade) || quantidade <= 0.0D) return;
        UUID uuid = jogador.getUniqueId();
        garantirConta(uuid, jogador.getName());
        double bruto = getSaldo(uuid) + quantidade;
        if (!Double.isFinite(bruto)) return;
        saldos.put(uuid, normalize(bruto));
        magnataDirty = true;
    }

    /**
     * Tenta sacar uma quantidade do saldo do jogador.
     *
     * @return true se havia saldo suficiente e o saque foi feito.
     */
    public synchronized boolean sacar(OfflinePlayer jogador, double quantidade) {
        if (jogador == null || !Double.isFinite(quantidade) || quantidade <= 0.0D) return false;
        UUID uuid = jogador.getUniqueId();
        garantirConta(uuid, jogador.getName());

        double valor = normalize(quantidade);
        double atual = getSaldo(uuid);
        if (atual + 0.0000001D < valor) return false;

        saldos.put(uuid, normalize(atual - valor));
        magnataDirty = true;
        return true;
    }

    public synchronized boolean transferir(OfflinePlayer origem, OfflinePlayer destino, double quantidade) {
        if (origem == null || destino == null || origem.getUniqueId().equals(destino.getUniqueId())
                || !Double.isFinite(quantidade) || quantidade <= 0.0D) return false;

        garantirConta(origem.getUniqueId(), origem.getName());
        garantirConta(destino.getUniqueId(), destino.getName());

        double valor = normalize(quantidade);
        double saldoOrigem = getSaldo(origem.getUniqueId());
        if (saldoOrigem + 0.0000001D < valor) return false;

        double brutoDestino = getSaldo(destino.getUniqueId()) + valor;
        if (!Double.isFinite(brutoDestino)) return false;

        saldos.put(origem.getUniqueId(), normalize(saldoOrigem - valor));
        saldos.put(destino.getUniqueId(), normalize(brutoDestino));
        magnataDirty = true;
        return true;
    }

    public synchronized void definirSaldo(OfflinePlayer jogador, double quantidade) {
        if (jogador == null || !Double.isFinite(quantidade)) return;
        UUID uuid = jogador.getUniqueId();
        garantirConta(uuid, jogador.getName());
        saldos.put(uuid, normalize(quantidade));
        magnataDirty = true;
    }

    public String getNomeConhecido(UUID uuid) {
        return nomesConhecidos.getOrDefault(uuid, "Desconhecido");
    }

    /**
     * Retorna os N jogadores com mais coins, do maior para o menor saldo.
     */
    public List<Map.Entry<UUID, Double>> getTop(int quantidade) {
        if (quantidade <= 0) return List.of();

        List<Map.Entry<UUID, Double>> lista = new ArrayList<>();
        for (Map.Entry<UUID, Double> entrada : saldos.entrySet()) {
            if (entrada.getValue() != null && entrada.getValue() > 0.0D) {
                lista.add(Map.entry(entrada.getKey(), entrada.getValue()));
            }
        }

        lista.sort((a, b) -> {
            int saldo = Double.compare(b.getValue(), a.getValue());
            if (saldo != 0) return saldo;

            String nomeA = nomesConhecidos.getOrDefault(a.getKey(), "");
            String nomeB = nomesConhecidos.getOrDefault(b.getKey(), "");
            int nome = nomeA.compareToIgnoreCase(nomeB);
            if (nome != 0) return nome;
            return a.getKey().compareTo(b.getKey());
        });

        if (lista.size() > quantidade) {
            return new ArrayList<>(lista.subList(0, quantidade));
        }
        return lista;
    }

    public UUID getMagnataId() {
        if (magnataDirty) atualizarMagnata();
        return magnataId;
    }

    public boolean isMagnata(UUID uuid) {
        if (magnataDirty) atualizarMagnata();
        return uuid != null && uuid.equals(magnataId);
    }

    private void atualizarMagnata() {
        UUID melhor = null;
        double maiorSaldo = 0.0D;
        String melhorNome = "";

        for (Map.Entry<UUID, Double> entrada : saldos.entrySet()) {
            double saldo = entrada.getValue() == null ? 0.0D : entrada.getValue();
            if (saldo <= 0.0D) continue;

            String nome = nomesConhecidos.getOrDefault(entrada.getKey(), "");
            if (melhor == null
                    || saldo > maiorSaldo
                    || (Double.compare(saldo, maiorSaldo) == 0 && nome.compareToIgnoreCase(melhorNome) < 0)
                    || (Double.compare(saldo, maiorSaldo) == 0
                        && nome.equalsIgnoreCase(melhorNome)
                        && entrada.getKey().compareTo(melhor) < 0)) {
                melhor = entrada.getKey();
                maiorSaldo = saldo;
                melhorNome = nome;
            }
        }

        magnataId = melhor;
        magnataDirty = false;
    }

    private static double normalize(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.round(value * 100.0D) / 100.0D;
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
