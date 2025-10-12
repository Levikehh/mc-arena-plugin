package me.levikehh.arena.models;

import java.util.UUID;

import org.bukkit.entity.Player;

public class Match {
    private final Arena arena;
    private final Player player1;
    private final Player player2;
    private final long startTime;
    private final int durationSeconds;
    private int remainingSeconds;
    private MatchState state;

    public enum MatchState {
        WAITING, // waiting for player2 response (accept, decline)
        STARTING, // Countdown before match starts
        ACTIVE, // Currently fighting
        FINISHED // Either player win or draw (time ran out)
    }

    public Match(Arena arena, Player player1, Player player2, int durationSeconds) {
        this.arena = arena;
        this.player1 = player1;
        this.player2 = player2;
        this.startTime = System.currentTimeMillis();
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
        this.state = MatchState.WAITING;
    }

    public Arena getArena() {
        return this.arena;
    }

    public MatchState getState() {
        return this.state;
    }

    public void setState(MatchState newState) {
        this.state = newState;
    }

    public Player getPlayer1() {
        return this.player1;
    }

    public Player getPlayer2() {
        return this.player2;
    }

    public boolean isParticipant(Player player) {
        return player.equals(this.player1) || player.equals(this.player2);
    }

    public boolean isParticipant(UUID uuid) {
        return this.player1.getUniqueId().equals(uuid) || this.player2.getUniqueId().equals(uuid);
    }

    public Player getOpponent(Player player) {
        if (player.equals(this.player1)) {
            return this.player2;
        } else if (player.equals(this.player2)) {
            return this.player1;
        }

        return null;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getRemainingSeconds() {
        return this.remainingSeconds;
    }

    public void setRemainingSeconds(int seconds) {
        this.remainingSeconds = seconds;
    }

    public void decrementTime() {
        this.remainingSeconds--;
    }

    public int getElapsedSeconds() {
        return this.durationSeconds - this.remainingSeconds;
    }

    public boolean isTimeUp() {
        return this.remainingSeconds <= 0;
    }
}
