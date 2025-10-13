package me.levikehh.arena.utils;

import java.util.function.Consumer;

public class TimedTask<T> {
    private final String id;
    private final T data;
    private final int initialDuration;
    private int remainingSeconds;
    private boolean cancelled;

    private Consumer<Integer> onTick;
    private Runnable onComplete;
    private Runnable onCancel;

    public TimedTask(String id, T data, int initialDuration) {
        this.id = id;
        this.data = data;
        this.initialDuration = initialDuration;
        this.remainingSeconds = initialDuration;
        this.cancelled = false;
    }

    public TimedTask<T> onTick(Consumer<Integer> onTick) {
        this.onTick = onTick;
        return this;
    }

    public TimedTask<T> onComplete(Runnable callback) {
        this.onComplete = callback;
        return this;
    }

    public TimedTask<T> onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    public boolean tick() {
        if (this.cancelled) {
            return false;
        }

        this.remainingSeconds--;

        if (this.onTick != null) {
            this.onTick.accept(this.remainingSeconds);
        }

        if (this.remainingSeconds <= 0) {
            if (this.onComplete != null) {
                this.onComplete.run();
            }
            return false;
        }

        return true;
    }

    public void cancel() {
        if (!this.cancelled) {
            this.cancelled = true;
            if (this.onCancel != null) {
                this.onCancel.run();
            }
        }
    }

    public String getId() {
        return this.id;
    }

    public T getData() {
        return this.data;
    }

    public int getInitialDuration() {
        return this.initialDuration;
    }

    public int getRemainingSeconds() {
        return this.remainingSeconds;
    }

    public int getElapsedSeconds() {
        return this.initialDuration - this.remainingSeconds;
    }

    public boolean isExpired() {
        return this.remainingSeconds <= 0;
    }

    public boolean isCancelled() {
        return !this.cancelled;
    }
}
