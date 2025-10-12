package me.levikehh.arena.utils;

import org.bukkit.ChatColor;
import org.bukkit.Location;

public class MessageFormatter {
    public static String success(String message) {
        return ChatColor.GREEN + message + ChatColor.RESET;
    }

    public static String error(String message) {
        return ChatColor.RED + message + ChatColor.RESET;
    }

    public static String variable(String message) {
        return ChatColor.GOLD + message + ChatColor.RESET;
    }

    public static String clickable(String message) {
        return ChatColor.AQUA + "[" + message + "]" + ChatColor.RESET;
    }

    public static String header(String text) {
        return ChatColor.GOLD + "=== " + text + " ===";
    }

    public static String formatLocation(Location location) {
        return String.format(
                ChatColor.GREEN + "x: " +
                ChatColor.GOLD + "%.2f " +
                ChatColor.GREEN + "y: " +
                ChatColor.GOLD + "%.2f " +
                ChatColor.GREEN + "z: " +
                ChatColor.GOLD + "%.2f",
                location.getX(),
                location.getY(),
                location.getZ());
    }
}
