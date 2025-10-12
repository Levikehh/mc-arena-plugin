package me.levikehh.arena.utils;

public class TimeFormatter {
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    public static String formatTimeReadable(int seconds) {
        if (seconds < 60) {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        String result = minutes + (minutes == 1 ? " minute" : " minutes");
        if (remainingSeconds > 0) {
            result += " " + remainingSeconds + (remainingSeconds == 1 ? " second" : " seconds");
        }

        return result;
    }
}
