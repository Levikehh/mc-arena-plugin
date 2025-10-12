package me.levikehh.arena.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.managers.MatchManager;

public class PlayerDisconnectListener implements Listener {
    private final ArenaPlugin plugin;
    private final MatchManager matchManager;

    public PlayerDisconnectListener(ArenaPlugin plugin, MatchManager matchManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in a match
        if (this.matchManager.isInMatch(player)) {
            this.matchManager.handleDisconnect(player);
        }
    }
}
