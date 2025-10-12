package me.levikehh.arena.models;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class PendingMatch {
    private Player initiator;
    private Player target;
    private BukkitTask task;

    public PendingMatch(Player initiator, Player target) {
        this.initiator = initiator;
        this.target = target;
    }

    public Player getInitiator() {
        return this.initiator;
    }

    public Player getTarget() {
        return this.target;
    }

    public BukkitTask getTask() {
        return this.task;
    }
}
