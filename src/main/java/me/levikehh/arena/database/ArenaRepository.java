package me.levikehh.arena.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import hu.nomindz.devkit.managers.DatabaseManager;
import hu.nomindz.devkit.utils.Result;
import me.levikehh.arena.models.Arena;

public class ArenaRepository {
    private static ArenaRepository instance;
    private final DatabaseManager databaseManager;

    private ArenaRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public static ArenaRepository getInstance(DatabaseManager databaseManager) {
        if (instance == null) {
            instance = new ArenaRepository(databaseManager);
        }

        return instance;
    }

    public Result<Integer> saveArena(Arena arena) {
        String sql = "INSERT INTO arenas (name, world, spawn1_x, spawn1_y, spawn1_z, spawn1_yaw, spawn1_pitch, " +
                    "spawn2_x, spawn2_y, spawn2_z, spawn2_yaw, spawn2_pitch) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = this.databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            Location spawn1 = arena.getSpawn1();
            Location spawn2 = arena.getSpawn2();
            
            pstmt.setString(1, arena.getName());
            pstmt.setString(2, spawn1.getWorld().getName());
            pstmt.setDouble(3, spawn1.getX());
            pstmt.setDouble(4, spawn1.getY());
            pstmt.setDouble(5, spawn1.getZ());
            pstmt.setFloat(6, spawn1.getYaw());
            pstmt.setFloat(7, spawn1.getPitch());
            pstmt.setDouble(8, spawn2.getX());
            pstmt.setDouble(9, spawn2.getY());
            pstmt.setDouble(10, spawn2.getZ());
            pstmt.setFloat(11, spawn2.getYaw());
            pstmt.setFloat(12, spawn2.getPitch());
            
            pstmt.executeUpdate();
            
            // Get generated ID
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return Result.success(rs.getInt(1));
            }
            
            return Result.failure("Failed to get generated ID");
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return Result.failure("Arena '" + arena.getName() + "' already exists");
            }
            return Result.failure("Database error: " + e.getMessage());
        }
    }

    public Result<List<Arena>> loadAllArenas() {
        List<Arena> arenas = new ArrayList<>();
        String sql = "SELECT name, world, spawn1_x, spawn1_y, spawn1_z, spawn1_yaw, spawn1_pitch, " +
                    "spawn2_x, spawn2_y, spawn2_z, spawn2_yaw, spawn2_pitch FROM arenas";
        
        try (Connection conn = this.databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                String worldName = rs.getString("world");
                
                // Check if world exists
                if (Bukkit.getWorld(worldName) == null) {
                    continue; // Skip arenas in unloaded worlds
                }
                
                Location spawn1 = new Location(
                    Bukkit.getWorld(worldName),
                    rs.getDouble("spawn1_x"),
                    rs.getDouble("spawn1_y"),
                    rs.getDouble("spawn1_z"),
                    rs.getFloat("spawn1_yaw"),
                    rs.getFloat("spawn1_pitch")
                );
                
                Location spawn2 = new Location(
                    Bukkit.getWorld(worldName),
                    rs.getDouble("spawn2_x"),
                    rs.getDouble("spawn2_y"),
                    rs.getDouble("spawn2_z"),
                    rs.getFloat("spawn2_yaw"),
                    rs.getFloat("spawn2_pitch")
                );
                
                arenas.add(new Arena(name, spawn1, spawn2));
            }
            
            return Result.success(arenas);
            
        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get arena by name
     */
    public Result<Arena> getArenaByName(String name) {
        String sql = "SELECT world, spawn1_x, spawn1_y, spawn1_z, spawn1_yaw, spawn1_pitch, " +
                    "spawn2_x, spawn2_y, spawn2_z, spawn2_yaw, spawn2_pitch FROM arenas WHERE name = ?";
        
        try (Connection conn = this.databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String worldName = rs.getString("world");
                
                if (Bukkit.getWorld(worldName) == null) {
                    return Result.failure("Arena world '" + worldName + "' is not loaded");
                }
                
                Location spawn1 = new Location(
                    Bukkit.getWorld(worldName),
                    rs.getDouble("spawn1_x"),
                    rs.getDouble("spawn1_y"),
                    rs.getDouble("spawn1_z"),
                    rs.getFloat("spawn1_yaw"),
                    rs.getFloat("spawn1_pitch")
                );
                
                Location spawn2 = new Location(
                    Bukkit.getWorld(worldName),
                    rs.getDouble("spawn2_x"),
                    rs.getDouble("spawn2_y"),
                    rs.getDouble("spawn2_z"),
                    rs.getFloat("spawn2_yaw"),
                    rs.getFloat("spawn2_pitch")
                );
                
                return Result.success(new Arena(name, spawn1, spawn2));
            }
            
            return Result.failure("Arena '" + name + "' not found");
            
        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Delete an arena
     */
    public Result<Void> deleteArena(String name) {
        String sql = "DELETE FROM arenas WHERE name = ?";
        
        try (Connection conn = this.databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                return Result.success();
            }
            
            return Result.failure("Arena '" + name + "' not found");
            
        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Check if arena exists
     */
    public Result<Boolean> arenaExists(String name) {
        String sql = "SELECT 1 FROM arenas WHERE name = ? LIMIT 1";
        
        try (Connection conn = this.databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            
            return Result.success(rs.next());
            
        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Get arena ID by name (needed for match results)
     */
    public Result<Integer> getArenaId(String name) {
        String sql = "SELECT id FROM arenas WHERE name = ?";
        
        try (Connection conn = this.databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Result.success(rs.getInt("id"));
            }
            
            return Result.failure("Arena '" + name + "' not found");
            
        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }
}
