package me.levikehh.arena.models;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class PendingMatch {
    private final String id;
    private Player initiator;
    private Player target;
    private BukkitTask task;

    public PendingMatch(Player initiator, Player target) {
        this.id = initiator.getUniqueId().toString() + "_" + target.getUniqueId().toString();
        this.initiator = initiator;
        this.target = target;
    }

    public String getId() {
        return this.id;
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
