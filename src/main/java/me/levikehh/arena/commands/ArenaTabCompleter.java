package me.levikehh.arena.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.levikehh.arena.ArenaPlugin;
import me.levikehh.arena.database.ArenaRepository;
import me.levikehh.arena.models.Arena;
import me.levikehh.arena.models.PendingMatch;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaTabCompleter implements TabCompleter {
    private final ArenaPlugin plugin;
    private final ArenaRepository arenaRepository;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "remove", "list", "challange", "accept", "decline", "stats", "history", "leaderboard"
            );

    public ArenaTabCompleter(ArenaPlugin plugin, ArenaRepository arenaRepository) {
        this.plugin = plugin;
        this.arenaRepository = arenaRepository;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        } 

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(SUBCOMMANDS);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "stats":
                case "history":
                case "challange":
                    for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                        completions.add(player.getDisplayName());
                    }
                    break;
                case "accept":
                case "decline":
                    List<PendingMatch> pendingMatches = this.plugin.getMatchManager().getPendingMatches();
                    for (PendingMatch pendingMatch : pendingMatches) {
                        completions.add(pendingMatch.getInitiator().getDisplayName());
                    }
                    break;
                case "create":
                case "remove":
                    this.arenaRepository.loadAllArenas()
                        .ifSuccess(arenas -> {
                            for (Arena arena : arenas) {
                                completions.add(arena.getName());
                            }
                        });
                    break;
                case "list":
                default:
                    break;
            }
        }

        return filterCompletions(completions, args[args.length - 1]);
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        String lowerInput = input.toLowerCase();

        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(lowerInput))
            .collect(Collectors.toList());
    }
}
