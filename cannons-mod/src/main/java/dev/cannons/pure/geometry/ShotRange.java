package dev.cannons.pure.geometry;

/** Range validation and limiting for a cannon's straight-line target distance. */
public final class ShotRange {
    public static final int UNLIMITED = -1;

    private ShotRange() {
    }

    public static boolean allows(Vec3 origin, Vec3 target, int maxRangeBlocks) {
        validate(maxRangeBlocks);
        return maxRangeBlocks == UNLIMITED
                || origin.distanceTo(target) <= maxRangeBlocks;
    }

    public static Vec3 clamp(Vec3 origin, Vec3 target, int maxRangeBlocks) {
        validate(maxRangeBlocks);
        if (maxRangeBlocks == UNLIMITED) {
            return target;
        }
        Vec3 delta = target.subtract(origin);
        double distance = delta.length();
        if (distance <= maxRangeBlocks || distance == 0) {
            return target;
        }
        return origin.add(delta.multiply(maxRangeBlocks / distance));
    }

    private static void validate(int maxRangeBlocks) {
        if (maxRangeBlocks != UNLIMITED && maxRangeBlocks < 1) {
            throw new IllegalArgumentException("maxRangeBlocks must be positive or unlimited");
        }
    }
}
