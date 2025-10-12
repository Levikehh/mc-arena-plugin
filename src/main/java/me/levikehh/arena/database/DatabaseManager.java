package me.levikehh.arena.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final JavaPlugin plugin;
    private final String dbPath;
    private Connection db;

    private DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + "/arena.db";
    }

    public static DatabaseManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new DatabaseManager(plugin);
        }

        return instance;
    }

    public Connection getConnection() {
        try {
            if (this.db == null || this.db.isClosed()) {
                this.db = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get db connection!");
            e.printStackTrace();
        }

        return this.db;
    }

    public void initialize() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        try {
            this.db = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath);

            this.plugin.getLogger().info("Database connection successful");
            this.createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database!");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (this.db != null && !this.db.isClosed()) {
                this.db.close();
                plugin.getLogger().info("Database disconnected");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close db connection!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        String arenasTable =
            "CREATE TABLE IF NOT EXISTS arenas (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL UNIQUE," +
            "world TEXT NOT NULL," +
            "spawn1_x REAL NOT NULL," +
            "spawn1_y REAL NOT NULL," +
            "spawn1_z REAL NOT NULL," +
            "spawn1_yaw REAL DEFAULT 0," +
            "spawn1_pitch REAL DEFAULT 0," +
            "spawn2_x REAL NOT NULL," +
            "spawn2_y REAL NOT NULL," +
            "spawn2_z REAL NOT NULL," +
            "spawn2_yaw REAL DEFAULT 0," +
            "spawn2_pitch REAL DEFAULT 0," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");";

        String matchResultsTable = 
            "CREATE TABLE IF NOT EXISTS match_results (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "arena_id INTEGER NOT NULL," +
            "player1_uuid TEXT NOT NULL," +
            "player1_name TEXT NOT NULL," +
            "player2_uuid TEXT NOT NULL," +
            "player2_name TEXT NOT NULL," +
            "winner_uuid TEXT," +
            "result_type TEXT NOT NULL," +
            "duration_seconds INTEGER NOT NULL," +
            "started_at TIMESTAMP NOT NULL," +
            "ended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (arena_id) REFERENCES arenas(id)" +
            ");";

        String playerStatsTable = 
            "CREATE TABLE IF NOT EXISTS player_stats (" +
            "player_uuid TEXT PRIMARY KEY," +
            "player_name TEXT NOT NULL," +
            "total_matches INTEGER DEFAULT 0," +
            "wins INTEGER DEFAULT 0," +
            "losses INTEGER DEFAULT 0," +
            "draws INTEGER DEFAULT 0," +
            "kills INTEGER DEFAULT 0," +
            "deaths INTEGER DEFAULT 0," +
            "total_playtime_seconds INTEGER DEFAULT 0," +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");";

        try (Statement stmt = this.db.createStatement()) {
            stmt.execute(arenasTable);
            stmt.execute(matchResultsTable);
            stmt.execute(playerStatsTable);

            plugin.getLogger().info("Database tables created/verified");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables!");
            e.printStackTrace();
        }
    }
}
