package me.levikehh.arena;

import me.levikehh.arena.commands.ArenaCommand;
import me.levikehh.arena.commands.ArenaTabCompleter;
import me.levikehh.arena.database.DatabaseManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenaPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        this.databaseManager = DatabaseManager.getInstance(this);
        this.databaseManager.initialize();

        registerCommands();

        getLogger().info("Arena plugin enabled");
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }

        getLogger().info("Arena plugin disabled");
    }

    private void registerCommands() {
        ArenaCommand arenaCommand = new ArenaCommand(this);
        ArenaTabCompleter arenaTabCompleter = new ArenaTabCompleter();

        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaTabCompleter);
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }
}

