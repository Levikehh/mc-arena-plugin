package me.levikehh.arena.commands;

import me.levikehh.arena.utils.CoordinateParser;
import me.levikehh.arena.utils.MessageBuilder;
import me.levikehh.arena.utils.MessageFormatter;
import me.levikehh.arena.utils.Result;
import me.levikehh.arena.ArenaPlugin;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.ChatColor;

import net.md_5.bungee.api.chat.ClickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ArenaCommand implements CommandExecutor {
    private final ArenaPlugin plugin;

    public ArenaCommand(ArenaPlugin plugin) {
        this.plugin = plugin;
    }

    // If onCommand returns false it will display the plugin.yml command block's 'usage' property
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageFormatter.error("Only players can run this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("waypoint")) {
            if (args.length == 0) {
                return false;
            }

            switch (args[0].toLowerCase()) {
                case "add":
                    break;
                case "remove":
                    break;
                case "list":
                    break;
                default:
                    return false;
            }
        }

        return true;
    }
}
