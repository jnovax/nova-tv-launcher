package com.appletv.contract;

public final class TimeFormatter {
    private TimeFormatter() {}

    public static String formatShortRemaining(long seconds) {
        if (seconds <= 0) return "0s";
        if (seconds < 60) return seconds + "s";
        long minutes = (seconds + 59) / 60;
        return minutes + "m";
    }
}
