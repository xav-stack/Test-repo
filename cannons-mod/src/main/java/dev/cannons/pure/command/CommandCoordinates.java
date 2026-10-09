package dev.cannons.pure.command;

public final class CommandCoordinates {
    private CommandCoordinates() {
    }

    public static boolean areValid(
            double x,
            double y,
            double z,
            double borderMinX,
            double borderMaxX,
            double borderMinZ,
            double borderMaxZ,
            int bottomY,
            int topY
    ) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && x >= borderMinX && x <= borderMaxX
                && z >= borderMinZ && z <= borderMaxZ
                && y >= bottomY && y < topY;
    }
}
