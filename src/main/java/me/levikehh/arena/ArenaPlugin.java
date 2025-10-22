package me.levikehh.arena;

import me.levikehh.arena.commands.ArenaCommands;
import me.levikehh.arena.config.ArenaConfig;
import me.levikehh.arena.database.ArenaRepository;
import me.levikehh.arena.listeners.PlayerDeathListener;
import me.levikehh.arena.listeners.PlayerDisconnectListener;
import me.levikehh.arena.managers.ArenaManager;
import me.levikehh.arena.managers.MatchManager;

import org.bukkit.plugin.java.JavaPlugin;

import hu.nomindz.devkit.managers.DatabaseManager;
import hu.nomindz.devkit.managers.TimerManager;
import hu.nomindz.devkit.command.CommandRegistry;
import hu.nomindz.devkit.config.ConfigFactory;
import hu.nomindz.devkit.config.ConfigManager;

public class ArenaPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private MatchManager matchManager;
    private ArenaManager arenaManager;
    private TimerManager timer;
    private ConfigManager<ArenaConfig> configManager;
    private CommandRegistry commands;

    @Override
    public void onEnable() {
        this.configManager = ConfigFactory.create(
                this,
                ArenaConfig.class,
                "config.yml",
                java.util.List.of("devkit-config.yml",
                        "config.yml"),
                "config_version",
                1,
                null);
        this.configManager.loadOrCreate();

        this.databaseManager = DatabaseManager.getInstance(this, () -> configManager.get().database());
        this.databaseManager.initializeFromResource("schema.sql");

        this.timer = TimerManager.getInstance(this);
        this.matchManager = MatchManager.getInstance(this, this.timer);
        this.arenaManager = ArenaManager.getInstance(this, ArenaRepository.getInstance(this.databaseManager));

        this.registerCommands();
        this.registerListeners();

        getLogger().info("Arena plugin enabled");
    }

    @Override
    public void onDisable() {
        if (this.matchManager != null) {
            this.matchManager.endAllMatches();
        }

        if (this.timer != null) {
            this.timer.stopAll();
        }

        if (this.databaseManager != null) {
            this.databaseManager.close();
        }

        getLogger().info("Arena plugin disabled");
    }

    private void registerCommands() {
        this.commands = new CommandRegistry(this);

        this.commands.registerAll(new ArenaCommands());

        getLogger().info("Commands registered.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerDeathListener(this, this.matchManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerDisconnectListener(this, this.matchManager), this);
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public MatchManager getMatchManager() {
        return this.matchManager;
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    public ArenaConfig config() {
        return this.configManager.get();
    }
}
