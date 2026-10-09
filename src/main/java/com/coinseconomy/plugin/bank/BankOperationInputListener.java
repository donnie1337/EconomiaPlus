package com.coinseconomy.plugin.bank;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.economy.MoneyParser;
import com.coinseconomy.plugin.transactions.WalletTransaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Captura a próxima entrada do jogador para depósito/saque. */
public final class BankOperationInputListener implements Listener {

    public enum Operation { DEPOSIT, WITHDRAW }

    private static final String CANCEL_WORD = "cancelar";

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economy;
    private final BankManager bank;
    private final Map<UUID, PendingOperation> pending = new ConcurrentHashMap<>();

    public BankOperationInputListener(CoinsEconomyPlugin plugin, EconomyManager economy, BankManager bank) {
        this.plugin = plugin;
        this.economy = economy;
        this.bank = bank;
    }

    public void start(Player player, Operation operation) {
        cancelPending(player.getUniqueId(), false);

        int timeoutSeconds = Math.max(5, plugin.getConfig().getInt("banco.operacoes.tempo-limite-segundos", 30));
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingOperation current = pending.remove(player.getUniqueId());
            if (current != null && player.isOnline()) {
                player.sendMessage(color("&5[Banco] &8• &fTempo esgotado. A operação bancária foi cancelada."));
            }
        }, timeoutSeconds * 20L);

        pending.put(player.getUniqueId(), new PendingOperation(operation, timeout));

        player.closeInventory();
        player.sendMessage(Component.empty());

        if (operation == Operation.DEPOSIT) {
            player.sendMessage(color("&5[Banco] &8• &fDepósito bancário"));
            player.sendMessage(Component.empty());
            player.sendMessage(color("&fDigite no chat o valor que deseja &adepositar&f."));
        } else {
            player.sendMessage(color("&5[Banco] &8• &fSaque"));
            player.sendMessage(Component.empty());
            player.sendMessage(color("&fDigite no chat o valor que deseja &csacar&f."));
        }

        player.sendMessage(color("&7Exemplos: &f1.000&7, &f10K&7, &f10.000 &7ou &fTUDO&7."));
        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("Clique ", NamedTextColor.GRAY)
                        .append(Component.text("AQUI", NamedTextColor.RED)
                                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                                .clickEvent(ClickEvent.callback((Audience audience) -> {
                                    if (audience instanceof Player clickedPlayer) {
                                        if (!cancelFromCommand(clickedPlayer)) {
                                            clickedPlayer.sendMessage(color(
                                                    "&5[Banco] &8• &fNão há nenhuma operação bancária pendente para cancelar."
                                            ));
                                        }
                                    }
                                }))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("Cancelar operação bancária", NamedTextColor.RED)
                                )))
                        .append(Component.text(" para cancelar a operação, ou digite ", NamedTextColor.GRAY))
                        .append(Component.text("cancelar", NamedTextColor.WHITE)
                                .decorate(net.kyori.adventure.text.format.TextDecoration.UNDERLINED))
                        .append(Component.text(".", NamedTextColor.GRAY))
        );
        player.sendMessage(Component.empty());
    }

    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public boolean cancelFromCommand(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pending.containsKey(uuid)) return false;

        cancelPending(uuid, true);
        player.sendMessage(color("&5[Banco] &8• &fOperação bancária cancelada."));
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pending.containsKey(uuid)) return;

        event.setCancelled(true);
        String input = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handleInput(event.getPlayer(), input));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pending.containsKey(uuid)) return;

        event.setCancelled(true);

        if (event.getMessage().trim().equalsIgnoreCase("/" + CANCEL_WORD)) {
            cancelFromCommand(event.getPlayer());
            return;
        }

        sendInvalid(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelPending(event.getPlayer().getUniqueId(), false);
    }

    private void handleInput(Player player, String rawInput) {
        PendingOperation current = pending.get(player.getUniqueId());
        if (current == null) return;

        String input = rawInput == null ? "" : rawInput.trim();
        if (CANCEL_WORD.equalsIgnoreCase(input)) {
            cancelPending(player.getUniqueId(), true);
            player.sendMessage(color("&5[Banco] &8• &fOperação bancária cancelada."));
            return;
        }

        boolean all = "tudo".equalsIgnoreCase(input);
        double amount;
        if (all) {
            amount = current.operation() == Operation.DEPOSIT
                    ? economy.getSaldo(player.getUniqueId())
                    : bank.getBalance(player.getUniqueId());
        } else {
            amount = parseAmount(input);
        }

        if (!Double.isFinite(amount) || amount <= 0.0D) {
            sendInvalid(player);
            return;
        }

        double minimum = Math.max(0.0D, plugin.getConfig().getDouble("banco.operacoes.minimo", 1000.0D));
        if (amount + 0.0000001D < minimum) {
            player.sendMessage(color("&5[Banco] &8• &fO valor mínimo para esta operação é &a"
                    + compact(minimum) + " Coins&f."));
            return;
        }

        if (current.operation() == Operation.DEPOSIT) {
            synchronized (plugin.getPersistenceLock()) {
                if (!bank.canDeposit(player.getUniqueId(), amount)) {
                    player.sendMessage(color("&5[Banco] &8• &fO banco não pode receber esse valor com segurança."));
                    return;
                }
                if (!economy.sacar(player, amount)) {
                    player.sendMessage(color("&5[Banco] &8• &fVocê não possui Coins suficientes na carteira."));
                    return;
                }

                // Primeiro persiste o débito da carteira; assim uma queda nunca duplica Coins.
                economy.save();
                bank.deposit(player.getUniqueId(), amount);
                bank.save();
            }

            plugin.getWalletTransactionManager().record(
                    player.getUniqueId(),
                    WalletTransaction.Type.BANK_DEPOSIT,
                    amount,
                    "Banco"
            );
            finish(player.getUniqueId());
            player.sendMessage(color("&5[Banco] &8• &fVocê depositou &a" + compact(amount) + " Coins &fcom sucesso."));
            return;
        }

        int dailyLimit = plugin.getConfig().getInt("banco.operacoes.limite-saques-diarios", 10);
        if (!bank.canWithdraw(player.getUniqueId(), dailyLimit)) {
            player.sendMessage(color("&5[Banco] &8• &fVocê atingiu o limite diário de saques."));
            return;
        }

        synchronized (plugin.getPersistenceLock()) {
            if (!economy.canDeposit(player.getUniqueId(), amount)) {
                player.sendMessage(color("&5[Banco] &8• &fSua carteira não pode receber esse valor com segurança."));
                return;
            }
            if (!bank.withdraw(player.getUniqueId(), amount)) {
                player.sendMessage(color("&5[Banco] &8• &fVocê não possui Coins suficientes no banco."));
                return;
            }

            // Primeiro persiste o débito bancário; só depois credita a carteira.
            bank.save();
            economy.depositar(player, amount);
            economy.save();
        }
        plugin.getWalletTransactionManager().record(
                player.getUniqueId(),
                WalletTransaction.Type.BANK_WITHDRAW,
                amount,
                "Banco"
        );
        finish(player.getUniqueId());
        player.sendMessage(color("&5[Banco] &8• &fVocê sacou &a" + compact(amount) + " Coins &fcom sucesso."));
    }

    private void sendInvalid(Player player) {
        player.sendMessage(color("&5[Banco] &8• &fAção não cancelada. Digite uma quantidade ou &7cancelar &fpara cancelar!"));
    }

    private double parseAmount(String raw) {
        return MoneyParser.parse(raw);
    }

    private void finish(UUID uuid) {
        PendingOperation operation = pending.remove(uuid);
        if (operation != null) operation.timeout().cancel();
    }

    private void cancelPending(UUID uuid, boolean notify) {
        PendingOperation operation = pending.remove(uuid);
        if (operation != null) operation.timeout().cancel();
    }

    private String compact(double value) {
        double abs = Math.abs(value);
        String suffix = "";
        double divisor = 1.0D;

        if (abs >= 1_000_000_000_000.0D) { suffix = "T"; divisor = 1_000_000_000_000.0D; }
        else if (abs >= 1_000_000_000.0D) { suffix = "B"; divisor = 1_000_000_000.0D; }
        else if (abs >= 1_000_000.0D) { suffix = "M"; divisor = 1_000_000.0D; }
        else if (abs >= 1_000.0D) { suffix = "K"; divisor = 1_000.0D; }

        double shown = value / divisor;
        String number = String.format(Locale.of("pt", "BR"), "%.2f", shown)
                .replaceAll("0+$", "")
                .replaceAll(",$", "");
        return number + suffix;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record PendingOperation(Operation operation, BukkitTask timeout) { }
}
