package me.levikehh.arena.commands;

import me.levikehh.arena.managers.ArenaManager;
import me.levikehh.arena.managers.MatchManager;
import me.levikehh.arena.models.Arena;
import me.levikehh.arena.models.PendingMatch;
import me.levikehh.arena.utils.MessageBuilder;
import me.levikehh.arena.utils.MessageFormatter;
import me.levikehh.arena.utils.TimeFormatter;
import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.database.MatchResultRepository;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaCommand implements CommandExecutor {
    private final ArenaPlugin plugin;
    private final MatchManager matchManager;
    private final ArenaManager arenaManager;
    private final Map<UUID, Location> pendingArenas = new HashMap<>();

    public ArenaCommand(ArenaPlugin plugin, MatchManager matchManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageFormatter.error("Only players can run this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("arena")) {
            if (args.length == 0) {
                return false;
            }

            switch (args[0].toLowerCase()) {
                // Arena related
                case "create":
                    this.createArena(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "remove":
                    this.removeArena(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "list":
                    this.listArenas(player);
                    break;
                // Match related
                case "challange":
                    this.challengePlayer(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "accept":
                    this.acceptChallenge(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "decline":
                    this.declineChallenge(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                // Stats related
                case "stats":
                    this.showPlayerStats(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "history":
                    this.showHistory(player, Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "leaderboard":
                    this.showLeaderboard(player);
                    break;
                default:
                    return false;
            }
        }

        return true;
    }

    private void createArena(Player player, String[] args) {
        if (args.length < 1) {
            player.spigot().sendMessage(new MessageBuilder().addError("Usage: /arena create <name>").build());
            return;
        }

        String name = args[0];

        if (this.arenaManager.arenaExists(name)) {
            player.spigot().sendMessage(
                    new MessageBuilder().addError("Arena ").addVariable(name).addError(" already exists").build());
            return;
        }

        UUID playerId = player.getUniqueId();

        if (!this.pendingArenas.containsKey(playerId)) {
            this.pendingArenas.put(playerId, player.getLocation());
            player.spigot().sendMessage(
                    new MessageBuilder().addSuccess("Spawn point 1 set for arena ").addVariable(name).build());
            player.spigot()
                    .sendMessage(new MessageBuilder().addSuccess("Run this command again at spawn point 2").build());
        } else {
            Location spawn1 = this.pendingArenas.remove(playerId);
            Location spawn2 = player.getLocation();

            this.arenaManager.createArena(name, spawn1, spawn2).ifSuccess(v -> {
                player.spigot().sendMessage(
                        new MessageBuilder().addSuccess("Arena ").addVariable(name).addSuccess(" created").build());
            }).ifFailure(error -> {
                player.spigot().sendMessage(new MessageBuilder().addError("Failed to create arena: " + error).build());
            });
        }
    }

    private void removeArena(Player player, String[] args) {
        if (args.length < 1) {
            player.spigot().sendMessage(new MessageBuilder().addError("Usage: /arena remove <name>").build());
            return;
        }

        String name = args[0];

        if (this.arenaManager.arenaExists(name)) {
            player.spigot().sendMessage(
                    new MessageBuilder().addError("Arena ").addVariable(name).addError(" doesn't exists").build());
            return;
        }

        this.arenaManager.deleteArena(name).ifSuccess(v -> {
            player.spigot().sendMessage(
                    new MessageBuilder().addSuccess("Arena ").addVariable(name).addSuccess(" deleted").build());
        }).ifFailure(error -> {
            player.spigot().sendMessage(new MessageBuilder().addError("Failed to remove arena: " + error).build());
        });
    }

    private void listArenas(Player player) {
        List<Arena> arenas = (List<Arena>) this.arenaManager.getAllArenas();

        if (arenas.isEmpty()) {
            player.sendMessage(MessageFormatter.error("No arenas have been created yet!"));
            return;
        }

        player.sendMessage(MessageFormatter.header("Arenas (" + arenas.size() + ")"));

        for (Arena arena : arenas) {
            String status = arena.isOccupied() ? "[OCCUPIED]" : "[AVAILABLE]";

            player.spigot().sendMessage(new MessageBuilder().addSuccess("• ").addVariable(arena.getName())
                    .addSuccess(" ").addVariable(status).build());
        }
    }

    private void challengePlayer(Player player, String[] args) {
        String name = args[0];

        if (this.matchManager.isInMatch(player)) {
            player.spigot()
                    .sendMessage(new MessageBuilder().addError("You can't start a new fight while participating in one")
                            .build());
            return;
        }

        Player target = this.plugin.getServer().getPlayer(name);
        if (target == null) {
            player.spigot()
                    .sendMessage(new MessageBuilder().addError("Player not found. Maybe they are offline?").build());
            return;
        }

        if (player.equals(target)) {
            player.spigot()
                    .sendMessage(new MessageBuilder().addError("You can't challenge yourself").build());
            return;
        }

        if (this.matchManager.isInMatch(target)) {
            player.spigot()
                    .sendMessage(new MessageBuilder().addError("You can't challenge a player whos already in a fight")
                            .build());
            return;
        }

        PendingMatch pendingMatch = new PendingMatch(player, target);
        if (!this.matchManager.setPendingMatch(pendingMatch)) {
            return;
        }

        MessageBuilder message = new MessageBuilder();

        message
                .addVariable(player.getDisplayName())
                .addSuccess(" challenged you for a PvP Arena match.\n")
                .addSuccess("Do you ")
                .addClickable("accept",
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arena accept " + player.getDisplayName()), null)
                .addSuccess(" or ")
                .addClickable("decline",
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/arena decline " + player.getDisplayName()),
                        null)
                .addSuccess(" it?");

        target.spigot().sendMessage(message.build());
        player.spigot().sendMessage(new MessageBuilder().addSuccess("You successfully challenged ").addVariable(name)
                .addSuccess("!").build());
    }

    private void acceptChallenge(Player player, String[] args) {
        String name = args[0];
        Player target = this.plugin.getServer().getPlayer(name);
        if (target == null) {
            player.spigot()
                    .sendMessage(new MessageBuilder().addError("Player not found. Maybe they are offline?").build());
            return;
        }

        List<Arena> availableArenas = this.arenaManager.getAvailableArenas();
        if (availableArenas.isEmpty()) {
            player.spigot().sendMessage(new MessageBuilder().addError("All the arenas are occupied").build());
            return;
        }

        Arena arena = availableArenas.get(0);
        this.matchManager.startMatch(arena, player, target);
    }

    private void declineChallenge(Player player, String[] args) {
        String name = args[0];
        Player target = this.plugin.getServer().getPlayer(name);
        if (target == null) {
            player.spigot()
                    .sendMessage(new MessageBuilder().addError("Player not found. Maybe they are offline?").build());
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Match against " + ChatColor.GRAY + name + " has been declined");
        target.sendMessage(ChatColor.GRAY + player.getDisplayName() + " declined the match");

        this.matchManager.removePendingMatch(player, target);
    }

    private void showPlayerStats(Player player, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length == 1) {
            // View another player's stats
            Player target = this.plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(MessageFormatter.error("Player '" + args[0] + "' not found!"));
                return;
            }
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            // View own stats
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        }

        MatchResultRepository.getInstance(this.plugin.getDatabaseManager()).getPlayerStats(targetUuid)
                .ifSuccess(stats -> {
                    player.sendMessage(MessageFormatter.header(targetName + "'s Statistics"));
                    player.sendMessage(ChatColor.YELLOW + "Total Matches: " + ChatColor.WHITE + stats.totalMatches);
                    player.sendMessage(ChatColor.GREEN + "Wins: " + ChatColor.WHITE + stats.wins +
                            ChatColor.GRAY + " (" + String.format("%.1f%%", stats.getWinRate()) + ")");
                    player.sendMessage(ChatColor.RED + "Losses: " + ChatColor.WHITE + stats.losses);
                    player.sendMessage(ChatColor.YELLOW + "Draws: " + ChatColor.WHITE + stats.draws);
                    player.sendMessage(ChatColor.AQUA + "K/D Ratio: " + ChatColor.WHITE +
                            String.format("%.2f", stats.getKDRatio()));
                    player.sendMessage(ChatColor.GRAY + "Total Playtime: " +
                            TimeFormatter.formatTimeReadable(stats.totalPlaytimeSeconds));
                })
                .ifFailure(error -> {
                    player.sendMessage(MessageFormatter.error("Failed to load stats: " + error));
                });
    }

    private void showHistory(Player player, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length == 1) {
            Player target = this.plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(MessageFormatter.error("Player '" + args[0] + "' not found!"));
                return;
            }
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        }

        MatchResultRepository.getInstance(this.plugin.getDatabaseManager()).getPlayerMatchHistory(targetUuid, 10)
                .ifSuccess(matches -> {
                    if (matches.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + targetName + " has no match history yet!");
                        return;
                    }

                    player.sendMessage(MessageFormatter.header(targetName + "'s Recent Matches"));

                    for (MatchResultRepository.MatchResultData match : matches) {
                        String opponent;
                        boolean isPlayer1 = match.player1Uuid.equals(targetUuid.toString());

                        if (isPlayer1) {
                            opponent = match.player2Name;
                        } else {
                            opponent = match.player1Name;
                        }

                        // Determine result for this player
                        String resultStr;
                        if (match.winnerUuid == null) {
                            resultStr = ChatColor.YELLOW + "DRAW";
                        } else if (match.winnerUuid.equals(targetUuid.toString())) {
                            resultStr = ChatColor.GREEN + "WIN";
                        } else {
                            resultStr = ChatColor.RED + "LOSS";
                        }

                        player.sendMessage(resultStr + ChatColor.GRAY + " vs " + ChatColor.WHITE + opponent +
                                ChatColor.GRAY + " in " + match.arenaName +
                                " (" + TimeFormatter.formatTime(match.durationSeconds) + ")");
                    }
                })
                .ifFailure(error -> {
                    player.sendMessage(MessageFormatter.error("Failed to load history: " + error));
                });
    }

    private void showLeaderboard(Player player) {
        MatchResultRepository.getInstance(this.plugin.getDatabaseManager()).getLeaderboard(10)
                .ifSuccess(leaderboard -> {
                    if (leaderboard.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "No players have competed yet!");
                        return;
                    }

                    player.sendMessage(MessageFormatter.header("Top 10 Players"));

                    int rank = 1;
                    for (MatchResultRepository.PlayerStats stats : leaderboard) {
                        String rankStr = ChatColor.GOLD + "#" + rank + ". ";
                        String nameStr = ChatColor.WHITE + stats.playerName;
                        String statsStr = ChatColor.GREEN + " " + stats.wins + "W " +
                                ChatColor.RED + stats.losses + "L " +
                                ChatColor.YELLOW + stats.draws + "D " +
                                ChatColor.GRAY + "(" + String.format("%.1f%%", stats.getWinRate()) + ")";

                        player.sendMessage(rankStr + nameStr + statsStr);
                        rank++;
                    }
                })
                .ifFailure(error -> {
                    player.sendMessage(MessageFormatter.error("Failed to load leaderboard: " + error));
                });
    }
}
