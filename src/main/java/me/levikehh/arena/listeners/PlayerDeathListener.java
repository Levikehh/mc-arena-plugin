package me.levikehh.arena.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.managers.MatchManager;
import me.levikehh.arena.models.Match;
import me.levikehh.arena.models.Match.MatchState;

public class PlayerDeathListener implements Listener {
    private final ArenaPlugin plugin;
    private final MatchManager matchManager;

    public PlayerDeathListener(ArenaPlugin plugin, MatchManager matchManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        Match match = this.matchManager.getMatch(dead);
        if (match != null && match.getState() == MatchState.ACTIVE) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            event.setKeepInventory(true);

            if (killer != null && this.matchManager.isInMatch(killer)) {
                matchManager.handleDeath(dead, killer);
            } else {
                Player opponent = match.getOpponent(dead);
                this.matchManager.handleDeath(dead, opponent);
            }
        }
    }
}
