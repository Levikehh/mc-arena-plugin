package me.levikehh.arena;

import me.levikehh.arena.commands.ArenaCommand;
import me.levikehh.arena.commands.ArenaTabCompleter;
import me.levikehh.arena.database.ArenaRepository;
import me.levikehh.arena.database.DatabaseManager;
import me.levikehh.arena.listeners.PlayerDeathListener;
import me.levikehh.arena.listeners.PlayerDisconnectListener;
import me.levikehh.arena.managers.ArenaManager;
import me.levikehh.arena.managers.MatchManager;

import org.bukkit.plugin.java.JavaPlugin;

import hu.nomindz.devkit.managers.TimerManager;

public class ArenaPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private MatchManager matchManager;
    private ArenaManager arenaManager;
    private TimerManager timer;

    @Override
    public void onEnable() {
        this.databaseManager = DatabaseManager.getInstance(this);
        this.databaseManager.initialize();

        this.timer = TimerManager.getInstance(this);
        this.matchManager = MatchManager.getInstance(this, this.timer);
        this.arenaManager = ArenaManager.getInstnace(this, ArenaRepository.getInstance(this.databaseManager));

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
        ArenaCommand arenaCommand = new ArenaCommand(this, this.matchManager, this.arenaManager);
        ArenaTabCompleter arenaTabCompleter = new ArenaTabCompleter(this,
                ArenaRepository.getInstance(this.databaseManager));

        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaTabCompleter);
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
}
