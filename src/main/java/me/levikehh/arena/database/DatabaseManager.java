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
            // this.createTables();
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
        String waypointsTable =
            "CREATE TABLE IF NOT EXISTS waypoints (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "player_uuid TEXT NOT NULL," +
            "name TEXT NOT NULL," +
            "world TEXT NOT NULL," +
            "x REAL NOT NULL," +
            "y REAL NOT NULL," +
            "z REAL NOT NULL," +
            "color TEXT DEFAULT 'WHITE'," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "UNIQUE(player_uuid, name)" +
            ");";

        try (Statement stmt = this.db.createStatement()) {
            stmt.execute(waypointsTable);
            plugin.getLogger().info("DB table created");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create 'waypoints' table!");
            e.printStackTrace();
        }
    }
}
