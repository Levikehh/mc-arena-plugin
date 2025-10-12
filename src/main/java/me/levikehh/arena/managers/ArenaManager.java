package me.levikehh.arena.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.database.ArenaRepository;
import me.levikehh.arena.models.Arena;
import me.levikehh.arena.utils.Result;

public class ArenaManager {
    private static ArenaManager instance;
    private final ArenaPlugin plugin;
    private final ArenaRepository repository;
    private final Map<String, Arena> arenas;

    private ArenaManager(ArenaPlugin plugin, ArenaRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.arenas = new HashMap<>();
        this.loadArenas();
    }

    public static ArenaManager getInstnace(ArenaPlugin plugin, ArenaRepository repository) {
        if (instance == null) {
            instance = new ArenaManager(plugin, repository);
        }

        return instance;
    }

    private void loadArenas() {
        Result<List<Arena>> result = this.repository.loadAllArenas();

        result.ifSuccess(arenaList -> {
            for (Arena arena : arenaList) {
                this.arenas.put(arena.getName(), arena);
                this.plugin.getLogger().info("Loaded arena " + arena.getName());
            }
        }).ifFailure(error -> {
            plugin.getLogger().severe("Failed to load arenas: " + error);
        });
    }

    public Result<Void> createArena(String name, Location spawn1, Location spawn2) {
        if (this.arenas.containsKey(name)) {
            return Result.failure("Arena '" + name + "' already exists");
        }
        
        Arena arena = new Arena(name, spawn1, spawn2);
        
        return this.repository.saveArena(arena)
            .map(arenaId -> {
                this.arenas.put(name, arena);
                return null;
            });
    }

    public Result<Void> deleteArena(String name) {
        Arena arena = this.arenas.get(name);
        
        if (arena == null) {
            return Result.failure("Arena '" + name + "' not found");
        }
        
        if (arena.isOccupied()) {
            return Result.failure("Cannot delete arena while a match is in progress");
        }
        
        return this.repository.deleteArena(name)
            .map(v -> {
                this.arenas.remove(name);
                return null;
            });
    }

    public Arena getArena(String name) {
        return this.arenas.get(name);
    }

    public Collection<Arena> getAllArenas() {
        return this.arenas.values();
    }

    public List<Arena> getAvailableArenas() {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : this.arenas.values()) {
            if (!arena.isOccupied()) {
                available.add(arena);
            }
        }
        return available;
    }

    public boolean arenaExists(String name) {
        return this.arenas.containsKey(name);
    }

    public void reloadArenas() {
        this.arenas.clear();
        this.loadArenas();
    }
}
