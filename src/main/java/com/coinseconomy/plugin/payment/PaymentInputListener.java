package com.coinseconomy.plugin.payment;

import com.coinseconomy.plugin.CoinsEconomyPlugin;
import com.coinseconomy.plugin.economy.EconomyManager;
import com.coinseconomy.plugin.transactions.WalletTransaction;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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

public final class PaymentInputListener implements Listener {

    private static final String CANCEL_WORD = "cancelar";

    private final CoinsEconomyPlugin plugin;
    private final EconomyManager economy;
    private final Map<UUID, PendingPayment> pending = new ConcurrentHashMap<>();

    public PaymentInputListener(CoinsEconomyPlugin plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void start(Player player, OfflinePlayer target) {
        cancel(player.getUniqueId(), false);

        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingPayment current = pending.remove(player.getUniqueId());
            if (current != null && player.isOnline()) {
                player.sendMessage(color("&c&lᴄᴏɪɴs &8• &fTempo esgotado. O pagamento foi cancelado."));
            }
        }, 30L * 20L);

        pending.put(player.getUniqueId(), new PendingPayment(target, timeout));

        player.sendMessage(Component.empty());
        player.sendMessage(color("&b&lᴄᴏɪɴs &8• &fPagamento"));
        player.sendMessage(Component.empty());
        player.sendMessage(color("&fDigite no chat o valor que deseja &apagar &fpara &a" + target.getName() + "&f."));
        player.sendMessage(color("&7Exemplos: &f1.000&7, &f10K&7, &f10.000 &7ou &fTUDO&7."));
        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("Clique ", NamedTextColor.GRAY)
                        .append(Component.text("AQUI", NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.callback((Audience audience) -> {
                                    if (audience instanceof Player clickedPlayer) {
                                        cancelFromClick(clickedPlayer);
                                    }
                                }))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("Cancelar pagamento", NamedTextColor.RED)
                                )))
                        .append(Component.text(" para cancelar a operação, ou digite ", NamedTextColor.GRAY))
                        .append(Component.text("cancelar", NamedTextColor.WHITE).decorate(TextDecoration.UNDERLINED))
                        .append(Component.text(".", NamedTextColor.GRAY))
        );
        player.sendMessage(Component.empty());
    }

    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public boolean cancelFromCommand(Player player) {
        if (!pending.containsKey(player.getUniqueId())) return false;
        cancel(player.getUniqueId(), true);
        player.sendMessage(color("&c&lᴄᴏɪɴs &8• &fPagamento cancelado."));
        return true;
    }

    private void cancelFromClick(Player player) {
        cancelFromCommand(player);
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
        if (!pending.containsKey(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        if (event.getMessage().trim().equalsIgnoreCase("/" + CANCEL_WORD)) {
            cancelFromCommand(event.getPlayer());
            return;
        }

        event.getPlayer().sendMessage(color("&c&lᴄᴏɪɴs &8• &fDigite um valor ou &7cancelar &fpara cancelar."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), false);
    }

    private void handleInput(Player player, String rawInput) {
        PendingPayment current = pending.get(player.getUniqueId());
        if (current == null) return;

        String input = rawInput == null ? "" : rawInput.trim();
        if (CANCEL_WORD.equalsIgnoreCase(input)) {
            cancelFromCommand(player);
            return;
        }

        double amount;
        if ("tudo".equalsIgnoreCase(input)) {
            amount = economy.getSaldo(player.getUniqueId());
        } else {
            amount = parseAmount(input);
        }

        if (!Double.isFinite(amount) || amount <= 0.0D) {
            player.sendMessage(color("&c&lᴄᴏɪɴs &8• &fValor inválido. Digite uma quantia válida ou &7cancelar&f."));
            return;
        }

        OfflinePlayer target = current.target();
        if (!plugin.getWalletSettingsManager().canReceive(target.getUniqueId())) {
            finish(player.getUniqueId());
            player.sendMessage(color("&c&lᴄᴏɪɴs &8• &fEsse jogador está com o recebimento de Coins desativado."));
            return;
        }

        if (!economy.tem(player.getUniqueId(), amount)) {
            player.sendMessage(color("&c&lᴄᴏɪɴs &8• &fVocê não possui Coins suficientes para esse pagamento."));
            return;
        }

        economy.sacar(player, amount);
        economy.depositar(target, amount);

        plugin.getWalletTransactionManager().record(
                player.getUniqueId(),
                WalletTransaction.Type.PAYMENT_SENT,
                amount,
                target.getName()
        );
        plugin.getWalletTransactionManager().record(
                target.getUniqueId(),
                WalletTransaction.Type.PAYMENT_RECEIVED,
                amount,
                player.getName()
        );

        finish(player.getUniqueId());

        player.sendMessage(color("&a&lSUCESSO &8• &fVocê pagou &a" + compact(amount)
                + " Coins &fpara &a" + target.getName() + "&f!"));

        if (target.isOnline()) {
            ((Player) target).sendMessage(color("&a&lSUCESSO &8• &a" + player.getName()
                    + " &fte pagou &a" + compact(amount) + " Coins&f!"));
        }
    }

    private double parseAmount(String raw) {
        if (raw == null) return Double.NaN;

        String value = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (value.isEmpty()) return Double.NaN;

        double multiplier = 1.0D;
        char last = value.charAt(value.length() - 1);
        switch (last) {
            case 'K' -> { multiplier = 1_000.0D; value = value.substring(0, value.length() - 1); }
            case 'M' -> { multiplier = 1_000_000.0D; value = value.substring(0, value.length() - 1); }
            case 'B' -> { multiplier = 1_000_000_000.0D; value = value.substring(0, value.length() - 1); }
            case 'T' -> { multiplier = 1_000_000_000_000.0D; value = value.substring(0, value.length() - 1); }
            default -> { }
        }

        if (value.isEmpty()) return Double.NaN;

        String normalized;
        if (value.contains(",")) {
            normalized = value.replace(".", "").replace(',', '.');
        } else if (value.matches("\\d{1,3}(\\.\\d{3})+")) {
            normalized = value.replace(".", "");
        } else {
            normalized = value;
        }

        try {
            double parsed = Double.parseDouble(normalized) * multiplier;
            return Double.isFinite(parsed) ? parsed : Double.NaN;
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
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
        return String.format(Locale.of("pt", "BR"), "%.2f", shown)
                .replaceAll("0+$", "")
                .replaceAll(",$", "") + suffix;
    }

    private void finish(UUID uuid) {
        PendingPayment current = pending.remove(uuid);
        if (current != null) current.timeout().cancel();
    }

    private void cancel(UUID uuid, boolean notify) {
        PendingPayment current = pending.remove(uuid);
        if (current != null) current.timeout().cancel();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record PendingPayment(OfflinePlayer target, BukkitTask timeout) { }
}
