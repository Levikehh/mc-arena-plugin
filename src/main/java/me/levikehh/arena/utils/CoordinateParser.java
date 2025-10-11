package me.levikehh.arena.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CoordinateParser {
    public static Location parseCoordinates(Player player, String locX, String locY, String locZ) throws IllegalArgumentException {
        Location playerLocation = player.getLocation();

        double x = parseCoordinate(locX, playerLocation.getX());
        double y = parseCoordinate(locY, playerLocation.getY());
        double z = parseCoordinate(locZ, playerLocation.getZ());

        return new Location(player.getWorld(), x, y, z);
    }

    public static double parseCoordinate(String input, double defaultValue) throws IllegalArgumentException {
        input = input.trim();

        if (input.equals("~")) {
            return defaultValue;
        } else if (input.startsWith("~")) {
            try {
                String offsetStr = input.substring(1);
                double offset = Double.parseDouble(offsetStr);
                return defaultValue + offset;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid relative coordinate: " + input);
            }
        } else {
            try {
                return Double.parseDouble(input); 
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid coordinate: " + input);
            }
        }
    }
}
