package me.levikehh.arena.models;

import org.bukkit.entity.Player;

public class MatchResult {
    private final ResultType type;
    private final Player winner;
    private final Player loser;
    private final int duration;

    public enum ResultType {
        KILL,
        TIMEOUT,
        DISCONNECT,
        FORFEIT
    }

    public MatchResult(ResultType type, Player winner, Player loser, int duration) {
        this.type = type;
        this.winner = winner;
        this.loser = loser;
        this.duration = duration;
    }

    public ResultType getType() {
        return this.type;
    }

    public Player getWinner() {
        return this.winner;
    }

    public Player getLoser() {
        return this.loser;
    }

    public int getDuration() {
        return this.duration;
    }

    public boolean isDraw() {
        return this.type == ResultType.TIMEOUT;
    }

    @Override
    public String toString() {
        if (this.isDraw()) {
            return String.format("Draw after %d seconds", this.duration);
        }

        return String.format("%s won by %s in %d seconds", winner.getDisplayName(), this.type, this.duration);
    }
}
