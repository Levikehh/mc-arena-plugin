package me.levikehh.arena.managers;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.models.Match;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TimerManager {
    private static TimerManager instance;

    private final ArenaPlugin plugin;
    private final Map<Match, BukkitTask> activeTimers;

    private TimerManager(ArenaPlugin plugin) {
        this.plugin = plugin;
        this.activeTimers = new HashMap<>();
    }

    public static TimerManager getInstance(ArenaPlugin plugin) {
        if (instance == null) {
            instance = new TimerManager(plugin);
        }

        return instance;
    }

    public void startTimer(Match match, Consumer<Integer> onTick, Runnable onComplete) {
        this.stopTimer(match);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (match.getState() != Match.MatchState.ACTIVE) {
                    cancel();
                    activeTimers.remove(match);
                    return;
                }

                match.decrementTime();
                int remainingSeconds = match.getRemainingSeconds();

                if (onTick != null) {
                    onTick.accept(remainingSeconds);
                }

                if (remainingSeconds <= 0) {
                    cancel();
                    activeTimers.remove(match);

                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }.runTaskTimer(this.plugin, 20L, 20L);

        this.activeTimers.put(match, task);
    }

    public void stopTimer(Match match) {
        BukkitTask task = this.activeTimers.remove(match);
        if (task != null) {
            task.cancel();
        }
    }

    public void stopAll() {
        for (BukkitTask task : this.activeTimers.values()) {
            task.cancel();
        }

        this.activeTimers.clear();
    }
}
