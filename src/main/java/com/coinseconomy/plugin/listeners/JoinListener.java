package com.coinseconomy.plugin.listeners;

import com.coinseconomy.plugin.economy.EconomyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Garante que todo jogador já tenha uma conta de coins assim que entra no
 * servidor - assim ele aparece no /topcoins mesmo antes de usar qualquer
 * comando de economia.
 */
public class JoinListener implements Listener {

    private final EconomyManager economia;

    public JoinListener(EconomyManager economia) {
        this.economia = economia;
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent evento) {
        economia.criarConta(evento.getPlayer());
    }
}
