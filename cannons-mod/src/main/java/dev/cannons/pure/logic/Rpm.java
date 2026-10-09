package dev.cannons.pure.logic;

public final class Rpm {
    private Rpm() {
    }

    public static int clamp(int emeraldCount, int maxRpm) {
        int maximum = Math.max(1, maxRpm);
        return Math.min(maximum, Math.max(1, emeraldCount));
    }
}
