package me.levikehh.arena.config;

import hu.nomindz.devkit.config.DatabaseConfig;
import jakarta.validation.constraints.*;

public record ArenaConfig(
        @Min(1) int config_version,
        DatabaseConfig database,
        Match match) {
    public record Match(
            @Min(1) int duration_seconds,
            @Min(0) @Max(10) int countdown_seconds,
            Rewards rewards) {
    }

    public record Rewards(
            boolean enchanted_golden_apple_on_kill) {
    }
}
