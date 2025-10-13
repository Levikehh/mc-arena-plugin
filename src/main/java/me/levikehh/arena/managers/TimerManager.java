package me.levikehh.arena.managers;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.utils.TimedTask;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TimerManager {
    private static TimerManager instance;

    private final ArenaPlugin plugin;
    private final Map<String, TimedTask<?>> activeTasks;
    private final Map<String, BukkitTask> runningTasks;

    private TimerManager(ArenaPlugin plugin) {
        this.plugin = plugin;
        this.activeTasks = new ConcurrentHashMap<>();
        this.runningTasks = new ConcurrentHashMap<>();
    }

    public static TimerManager getInstance(ArenaPlugin plugin) {
        if (instance == null) {
            instance = new TimerManager(plugin);
        }

        return instance;
    }

    public <T> TimedTask<T> startTimer(String id, T data, int durationSeconds, Consumer<Integer> onTick, Runnable onComplete) {
        this.stopTimer(id);

        TimedTask<T> task = new TimedTask<T>(id, data, durationSeconds);

        if (onTick != null) {
            task.onTick(onTick);
        }

        if (onComplete != null) {
            task.onComplete(onComplete);
        }

        this.activeTasks.put(id, task);

        BukkitTask bukkitTask = new BukkitRunnable() {
            @Override
            public void run() {
                boolean isRunning = task.tick();

                if (!isRunning) {
                    cancel();
                    activeTasks.remove(id);
                    runningTasks.remove(id);
                }
            }
        }.runTaskTimer(this.plugin, 20L, 20L);

        this.runningTasks.put(id, bukkitTask);

        return task;
    }

    public boolean stopTimer(String id) {
        TimedTask<?> task = this.activeTasks.remove(id);
        BukkitTask bukkitTask = this.runningTasks.remove(id);

        if (task != null) {
            task.cancel();
        }

        if (bukkitTask != null) {
            bukkitTask.cancel();
            return true;
        }

        return false;
    }

    public void stopAll() {
        for (TimedTask<?> task : this.activeTasks.values()) {
            task.cancel();
        }

        for (BukkitTask task : this.runningTasks.values()) {
            task.cancel();
        }

        this.activeTasks.clear();
        this.runningTasks.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> TimedTask<T> getTask(String id) {
        return (TimedTask<T>) this.activeTasks.get(id);
    }

    public boolean isRunning(String id) {
        /* 
         * TODO
         * shouldn't it be runningTasks instead? currently my thought behind this is that:
         * when we call startTimer then the timer gonna be set in activeTasks first so the next call
         * on isRunning is possibly better to return what got added to activeTasks, not runningTasks.
         * Could be I'm all wrong here, will see what fits best.
        */
        return this.activeTasks.containsKey(id);
    }

    public int getRemainingSeconds(String id) {
        TimedTask<?> task = this.activeTasks.get(id);
        return task != null ? task.getRemainingSeconds() : -1;
    }

    public int getActiveTimerCount() {
        return this.activeTasks.size();
    }
}
