package me.levikehh.arena.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import hu.nomindz.devkit.managers.DatabaseManager;
import hu.nomindz.devkit.utils.Result;
import me.levikehh.arena.models.MatchResult;

public class MatchResultRepository {
    private static MatchResultRepository instance;
    private final DatabaseManager databaseManager;

    private MatchResultRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public static MatchResultRepository getInstance(DatabaseManager databaseManager) {
        if (instance == null) {
            instance = new MatchResultRepository(databaseManager);
        }

        return instance;
    }

    public Result<Integer> saveMatchResult(int arenaId, Player player1, Player player2,
            MatchResult matchResult, Timestamp startedAt) {
        String sql = "INSERT INTO match_results (arena_id, player1_uuid, player1_name, " +
                "player2_uuid, player2_name, winner_uuid, result_type, duration_seconds, started_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = this.databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            conn.setAutoCommit(false);

            pstmt.setInt(1, arenaId);
            pstmt.setString(2, player1.getUniqueId().toString());
            pstmt.setString(3, player1.getName());
            pstmt.setString(4, player2.getUniqueId().toString());
            pstmt.setString(5, player2.getName());

            // Winner UUID (null for draw)
            if (matchResult.getWinner() != null) {
                pstmt.setString(6, matchResult.getWinner().getUniqueId().toString());
            } else {
                pstmt.setNull(6, java.sql.Types.VARCHAR);
            }

            pstmt.setString(7, matchResult.getType().name());
            pstmt.setInt(8, matchResult.getDuration());
            pstmt.setTimestamp(9, startedAt);

            pstmt.executeUpdate();

            // Get generated ID
            int generatedId;
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                } else {
                    return Result.failure("Failed to get generated ID");
                }
            }

            // Update player stats
            this.updatePlayerStats(conn, player1, matchResult);
            this.updatePlayerStats(conn, player2, matchResult);

            conn.commit();

            return Result.success(generatedId);

        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }

    /**
     * Update player statistics after a match
     */
    private void updatePlayerStats(Connection conn, Player player, MatchResult matchResult) throws SQLException {
        // Ensure row exists & keep latest name
        String upsert = "INSERT INTO player_stats (player_uuid, player_name) VALUES (?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name";
        try (PreparedStatement ps = conn.prepareStatement(upsert)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.executeUpdate();
        }

        boolean isWinner = matchResult.getWinner() != null && matchResult.getWinner().equals(player);
        boolean isLoser = matchResult.getLoser() != null && matchResult.getLoser().equals(player);
        boolean isDraw = matchResult.isDraw();
        int killsInc = (isWinner && matchResult.getType() == MatchResult.ResultType.KILL) ? 1 : 0;

        String update = "UPDATE player_stats SET " +
                " total_matches = total_matches + 1," +
                " wins = wins + ?," +
                " losses = losses + ?," +
                " draws = draws + ?," +
                " kills = kills + ?," +
                " deaths = deaths + ?," +
                " total_playtime_seconds = total_playtime_seconds + ?," +
                " updated_at = CURRENT_TIMESTAMP " +
                "WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, isWinner ? 1 : 0);
            ps.setInt(2, isLoser ? 1 : 0);
            ps.setInt(3, isDraw ? 1 : 0);
            ps.setInt(4, killsInc);
            ps.setInt(5, isLoser ? 1 : 0);
            ps.setInt(6, matchResult.getDuration());
            ps.setString(7, player.getUniqueId().toString());
            ps.executeUpdate();
        }
    }

    /**
     * Get recent match results (last N matches)
     */
    public Result<List<MatchResultData>> getRecentMatches(int limit) {
        List<MatchResultData> results = new ArrayList<>();
        String sql = "SELECT mr.*, a.name as arena_name " +
                "FROM match_results mr " +
                "JOIN arenas a ON mr.arena_id = a.id " +
                "ORDER BY mr.ended_at DESC " +
                "LIMIT ?";

        try (Connection conn = this.databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(new MatchResultData(
                        rs.getInt("id"),
                        rs.getString("arena_name"),
                        rs.getString("player1_uuid"),
                        rs.getString("player1_name"),
                        rs.getString("player2_uuid"),
                        rs.getString("player2_name"),
                        rs.getString("winner_uuid"),
                        rs.getString("result_type"),
                        rs.getInt("duration_seconds"),
                        rs.getTimestamp("started_at"),
                        rs.getTimestamp("ended_at")));
            }

            return Result.success(results);

        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }

    /**
     * Get match history for a specific player
     */
    public Result<List<MatchResultData>> getPlayerMatchHistory(UUID playerUuid, int limit) {
        List<MatchResultData> results = new ArrayList<>();
        String sql = "SELECT mr.*, a.name as arena_name " +
                "FROM match_results mr " +
                "JOIN arenas a ON mr.arena_id = a.id " +
                "WHERE mr.player1_uuid = ? OR mr.player2_uuid = ? " +
                "ORDER BY mr.ended_at DESC " +
                "LIMIT ?";

        try (Connection conn = this.databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String uuidStr = playerUuid.toString();
            pstmt.setString(1, uuidStr);
            pstmt.setString(2, uuidStr);
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(new MatchResultData(
                        rs.getInt("id"),
                        rs.getString("arena_name"),
                        rs.getString("player1_uuid"),
                        rs.getString("player1_name"),
                        rs.getString("player2_uuid"),
                        rs.getString("player2_name"),
                        rs.getString("winner_uuid"),
                        rs.getString("result_type"),
                        rs.getInt("duration_seconds"),
                        rs.getTimestamp("started_at"),
                        rs.getTimestamp("ended_at")));
            }

            return Result.success(results);

        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }

    /**
     * Get player statistics
     */
    public Result<PlayerStats> getPlayerStats(UUID playerUuid) {
        String sql = "SELECT * FROM player_stats WHERE player_uuid = ?";

        try (Connection conn = this.databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Result.success(new PlayerStats(
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getInt("total_matches"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("draws"),
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("total_playtime_seconds")));
            }

            // No stats yet - return empty stats
            return Result.success(new PlayerStats(
                    playerUuid.toString(),
                    Bukkit.getPlayer(playerUuid).getName(),
                    0, 0, 0, 0, 0, 0, 0));

        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }

    /**
     * Get leaderboard (top players by wins)
     */
    public Result<List<PlayerStats>> getLeaderboard(int limit) {
        List<PlayerStats> leaderboard = new ArrayList<>();
        String sql = "SELECT * FROM player_stats " +
                "ORDER BY wins DESC, total_matches ASC " +
                "LIMIT ?";

        try (Connection conn = this.databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                leaderboard.add(new PlayerStats(
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getInt("total_matches"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("draws"),
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("total_playtime_seconds")));
            }

            return Result.success(leaderboard);

        } catch (SQLException e) {
            return Result.failure("Database error: " + e.getMessage());
        }
    }

    // Data classes for returning results

    public static class MatchResultData {
        public final int id;
        public final String arenaName;
        public final String player1Uuid;
        public final String player1Name;
        public final String player2Uuid;
        public final String player2Name;
        public final String winnerUuid; // null for draw
        public final String resultType;
        public final int durationSeconds;
        public final Timestamp startedAt;
        public final Timestamp endedAt;

        public MatchResultData(int id, String arenaName, String player1Uuid, String player1Name,
                String player2Uuid, String player2Name, String winnerUuid,
                String resultType, int durationSeconds, Timestamp startedAt, Timestamp endedAt) {
            this.id = id;
            this.arenaName = arenaName;
            this.player1Uuid = player1Uuid;
            this.player1Name = player1Name;
            this.player2Uuid = player2Uuid;
            this.player2Name = player2Name;
            this.winnerUuid = winnerUuid;
            this.resultType = resultType;
            this.durationSeconds = durationSeconds;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
        }
    }

    public static class PlayerStats {
        public final String playerUuid;
        public final String playerName;
        public final int totalMatches;
        public final int wins;
        public final int losses;
        public final int draws;
        public final int kills;
        public final int deaths;
        public final int totalPlaytimeSeconds;

        public PlayerStats(String playerUuid, String playerName, int totalMatches,
                int wins, int losses, int draws, int kills, int deaths, int totalPlaytimeSeconds) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.totalMatches = totalMatches;
            this.wins = wins;
            this.losses = losses;
            this.draws = draws;
            this.kills = kills;
            this.deaths = deaths;
            this.totalPlaytimeSeconds = totalPlaytimeSeconds;
        }

        public double getWinRate() {
            if (this.totalMatches == 0)
                return 0.0;
            return (double) this.wins / this.totalMatches * 100.0;
        }

        public double getKDRatio() {
            if (this.deaths == 0)
                return this.kills;
            return (double) this.kills / this.deaths;
        }
    }
}
