package me.levikehh.arena.managers;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.database.ArenaRepository;
import me.levikehh.arena.database.MatchResultRepository;
import me.levikehh.arena.models.Arena;
import me.levikehh.arena.models.Match;
import me.levikehh.arena.models.MatchResult;
import me.levikehh.arena.models.PendingMatch;
import me.levikehh.arena.models.Match.MatchState;
import me.levikehh.arena.models.MatchResult.ResultType;
import me.levikehh.arena.utils.MessageBuilder;
import me.levikehh.arena.utils.MessageFormatter;
import me.levikehh.arena.utils.TimeFormatter;
import me.levikehh.arena.utils.TimedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.util.*;

public class MatchManager {
    private static MatchManager instance;
    private final ArenaPlugin plugin;
    private final TimerManager timer;
    private final Map<UUID, Match> activeMatches;
    private final List<TimedTask<PendingMatch>> pendingMatches;

    private MatchManager(ArenaPlugin plugin, TimerManager timer) {
        this.plugin = plugin;
        this.timer = timer;
        this.activeMatches = new HashMap<>();
        this.pendingMatches = new ArrayList<>();
    }

    public static MatchManager getInstance(ArenaPlugin plugin, TimerManager timer) {
        if (instance == null) {
            instance = new MatchManager(plugin, timer);
        }

        return instance;
    }

    public boolean removePendingMatch(Player player1, Player player2) {
        // TODO: could be done by checking id, but wanna make sure the player objects are the same so no MITM can be achieved.
        return this.pendingMatches.removeIf(pendingMatch -> {
            return pendingMatch.getData().getInitiator().equals(player1) &&
                    pendingMatch.getData().getTarget().equals(player2);
        });
    }

    public List<PendingMatch> getPendingMatches() {
        List<PendingMatch> result = new ArrayList<>();
        for (TimedTask<PendingMatch> pendingMatchTask : this.pendingMatches) {
            result.add(pendingMatchTask.getData());
        }
        return result;
    }

    public boolean setPendingMatch(PendingMatch pendingMatch) {
        TimedTask<PendingMatch> task = this.timer.getTask("pending_" + pendingMatch.getId());

        if (task != null) {
            pendingMatch.getInitiator().spigot()
                    .sendMessage(new MessageBuilder()
                            .addError("You already challenged this player. You can challenge them again in ")
                            .addVariable(TimeFormatter.formatTimeReadable(task.getRemainingSeconds())).build());
            return false;
        }

        if (this.isInMatch(pendingMatch.getInitiator())) {
            pendingMatch.getInitiator().spigot().sendMessage(
                    new MessageBuilder().addError("You can't start a new fight while participating in one").build());
            return false;
        }

        if (this.isInMatch(pendingMatch.getTarget())) {
            pendingMatch.getInitiator().spigot().sendMessage(
                    new MessageBuilder().addError("You can't challenge a player whos already in a fight").build());
            return false;
        }

        this.timer.startTimer(
                "pending_" + pendingMatch.getId(),
                pendingMatch,
                60,
                null,
                () -> {
                    this.pendingMatches.remove(task);
                });

        this.pendingMatches.add(task);

        return true;
    }

    public void startMatch(Arena arena, Player player1, Player player2) {
        int duration = 3 * 60;
        Match match = new Match(arena, player1, player2, duration);

        this.removePendingMatch(player1, player2);
        arena.setOccupied(true);

        this.activeMatches.put(player1.getUniqueId(), match);
        this.activeMatches.put(player2.getUniqueId(), match);

        this.preparePlayer(player1);
        this.preparePlayer(player2);

        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        String announcement = ChatColor.GOLD + player1.getName() + ChatColor.GRAY + " vs " +
                ChatColor.GOLD + player2.getName() + ChatColor.GRAY +
                " in arena " + ChatColor.YELLOW + arena.getName();

        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (!player.equals(player1) && !player.equals(player2)) {
                player.sendMessage(MessageFormatter.header("Arena Match"));
                player.sendMessage(announcement);
            }
        }

        this.startCountdown(match);
    }

    private void startCountdown(Match match) {
        match.setState(Match.MatchState.STARTING);

        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        this.timer.startTimer(
                "countdown_" + match.getId(),
                null,
                3,
                (remainingSeconds) -> {
                    if (remainingSeconds > 0) {
                        String message = ChatColor.GOLD + "" + ChatColor.BOLD + remainingSeconds;
                        p1.sendTitle(message, "", 0, 20, 10);
                        p2.sendTitle(message, "", 0, 20, 10);

                        p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                        p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    }
                }, () -> {
                    beginMatch(match);
                });
    }

    private void beginMatch(Match match) {
        match.setState(Match.MatchState.ACTIVE);

        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        String startTitle = ChatColor.GREEN + "" + ChatColor.BOLD + "FIGHT!";
        p1.sendTitle(startTitle, ChatColor.GRAY + "vs " + p2.getName(), 0, 40, 20);
        p2.sendTitle(startTitle, ChatColor.GRAY + "vs " + p1.getName(), 0, 40, 20);

        p1.playSound(p1.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        p2.playSound(p2.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        this.timer.startTimer(
                match.getId(),
                match,
                match.getDuration(),
                (remainingSeconds) -> {
                    updateMatchDisplay(match, remainingSeconds);
                },
                () -> {
                    endMatch(match, MatchResult.ResultType.TIMEOUT, null, null);
                });
    }

    private void updateMatchDisplay(Match match, int remainingSeconds) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        String timerStr = TimeFormatter.formatTime(remainingSeconds);

        String actionBar1 = String.format(ChatColor.YELLOW + "⏱ %s" + ChatColor.GRAY + " | " + ChatColor.RED + "vs %s",
                timerStr, p2.getDisplayName());
        String actionBar2 = String.format(ChatColor.YELLOW + "⏱ %s" + ChatColor.GRAY + " | " + ChatColor.RED + "vs %s",
                timerStr, p1.getDisplayName());

        p1.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar1));
        p2.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar2));

    }

    private void endMatch(Match match, MatchResult.ResultType resultType, Player winner, Player loser) {
        this.timer.stopTimer(match.getId());

        match.setState(Match.MatchState.FINISHED);

        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        int duration = match.getElapsedSeconds();
        MatchResult result = new MatchResult(resultType, winner, loser, duration);

        this.announceResult(match, result);

        this.saveMatchResult(match, result);

        this.giveRewards(match, result);

        this.resetPlayer(p1);
        this.resetPlayer(p2);

        this.activeMatches.remove(p1.getUniqueId());
        this.activeMatches.remove(p2.getUniqueId());

        match.getArena().setOccupied(false);
    }

    private void preparePlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }

    private void resetPlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }

    private void saveMatchResult(Match match, MatchResult result) {
        ArenaRepository.getInstance(this.plugin.getDatabaseManager()).getArenaId(match.getArena().getName())
                .ifSuccess(arenaId -> {
                    Timestamp startedAt = new Timestamp(match.getStartTime());

                    MatchResultRepository.getInstance(this.plugin.getDatabaseManager())
                            .saveMatchResult(arenaId, match.getPlayer1(), match.getPlayer2(), result, startedAt)
                            .ifFailure(error -> {
                                this.plugin.getLogger().severe("Failed to save match result: " + error);
                            });
                }).ifFailure(error -> {
                    this.plugin.getLogger().severe("Failed to get arena by name: " + error);
                });
    }

    private void giveRewards(Match match, MatchResult result) {
        if (result.isDraw()) {
            return;
        }

        if (result.getWinner() != null) {
            if (result.getType() == MatchResult.ResultType.KILL) {
                ItemStack reward = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
                result.getWinner().getInventory().addItem(reward);
            }
        }
    }

    private void announceResult(Match match, MatchResult result) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        if (result.isDraw()) {
            String title = ChatColor.YELLOW + "" + ChatColor.BOLD + "DRAW!";
            String subtitle = ChatColor.GRAY + "Time ran out";

            p1.sendTitle(title, subtitle, 10, 60, 20);
            p2.sendTitle(title, subtitle, 10, 60, 20);

            p1.sendMessage(MessageFormatter.header("Match Result"));
            p1.sendMessage(ChatColor.YELLOW + "The match ended in a draw!");
            p2.sendMessage(MessageFormatter.header("Match Result"));
            p2.sendMessage(ChatColor.YELLOW + "The match ended in a draw!");
        } else {
            String winTitle = ChatColor.GREEN + "" + ChatColor.BOLD + "VICTORY!";
            String loseTitle = ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT";

            result.getWinner().sendTitle(winTitle, "", 10, 60, 20);
            result.getLoser().sendTitle(loseTitle, "", 10, 60, 20);

            p1.sendMessage(MessageFormatter.header("Match Result"));
            p2.sendMessage(MessageFormatter.header("Match Result"));

            String resultMsg = ChatColor.GREEN + result.getWinner().getName() +
                    ChatColor.GRAY + " defeated " +
                    ChatColor.RED + result.getLoser().getName();
            String durationMsg = ChatColor.GRAY + "Duration: " +
                    TimeFormatter.formatTime(result.getDuration());

            p1.sendMessage(resultMsg);
            p1.sendMessage(durationMsg);
            p2.sendMessage(resultMsg);
            p2.sendMessage(durationMsg);

            // Play sounds
            result.getWinner().playSound(result.getWinner().getLocation(),
                    org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            result.getLoser().playSound(result.getLoser().getLocation(),
                    org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (player.equals(p1) || player.equals(p2)) {
                continue;
            }

            if (result.isDraw()) {
                player.sendMessage(ChatColor.YELLOW + "The match between " + ChatColor.GRAY + p1.getDisplayName()
                        + " and " + ChatColor.GRAY + p2.getDisplayName() + " ended in a draw!");
            } else {
                String resultMsg = ChatColor.GREEN + result.getWinner().getName() +
                        ChatColor.GRAY + " defeated " +
                        ChatColor.RED + result.getLoser().getName();
                String durationMsg = ChatColor.GRAY + "Duration: " +
                        TimeFormatter.formatTime(result.getDuration());

                player.sendMessage(resultMsg);
                player.sendMessage(durationMsg);
            }
        }
    }

    public Match getMatch(Player player) {
        return this.activeMatches.get(player.getUniqueId());
    }

    public boolean isInMatch(Player player) {
        return this.activeMatches.containsKey(player.getUniqueId());
    }

    public void handleDeath(Player dead, Player killer) {
        Match match = this.getMatch(dead);
        if (match != null && match.getState() == MatchState.ACTIVE) {
            this.endMatch(match, ResultType.KILL, killer, dead);
        }
    }

    public void handleDisconnect(Player player) {
        Match match = this.getMatch(player);
        if (match != null && match.getState() == MatchState.ACTIVE) {
            Player opponent = match.getOpponent(player);
            this.endMatch(match, ResultType.DISCONNECT, opponent, player);
        }
    }

    public void endAllMatches() {
        List<Match> matches = new ArrayList<>(new HashSet<>(activeMatches.values()));

        for (Match match : matches) {
            if (match.getState() == MatchState.ACTIVE) {
                this.endMatch(match, ResultType.TIMEOUT, null, null);
            }
        }

        this.activeMatches.clear();
    }

    public Collection<Match> getAllMatches() {
        return new HashSet<>(this.activeMatches.values());
    }

    public int getActiveMatchCount() {
        return new HashSet<>(this.activeMatches.values()).size();
    }
}
