package me.levikehh.arena.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "challange"
            );

    public ArenaTabCompleter() {
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        } 

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(this.SUBCOMMANDS);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "challange":
                    break;
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
