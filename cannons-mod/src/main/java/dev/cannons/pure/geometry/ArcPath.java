package dev.cannons.pure.geometry;

import java.util.Arrays;

/** A numerically integrated, arc-length-parameterized parabolic path. */
public final class ArcPath {
    private static final int TABLE_SEGMENTS = 16_384;

    private final Vec3 start;
    private final Vec3 end;
    private final double height;
    private final double[] lengths;
    private final double totalLength;

    public ArcPath(Vec3 start, Vec3 end, double arcHeightRatio) {
        if (!Double.isFinite(arcHeightRatio) || arcHeightRatio < 0) {
            throw new IllegalArgumentException("arcHeightRatio must be finite and non-negative");
        }
        this.start = start;
        this.end = end;
        this.height = arcHeightRatio * start.distanceTo(end);
        this.lengths = new double[TABLE_SEGMENTS + 1];
        Vec3 previous = pointAtParameter(0);
        for (int i = 1; i <= TABLE_SEGMENTS; i++) {
            Vec3 current = pointAtParameter((double) i / TABLE_SEGMENTS);
            lengths[i] = lengths[i - 1] + previous.distanceTo(current);
            previous = current;
        }
        totalLength = lengths[TABLE_SEGMENTS];
    }

    public Vec3 start() {
        return start;
    }

    public Vec3 end() {
        return end;
    }

    public double height() {
        return height;
    }

    public double totalLength() {
        return totalLength;
    }

    public boolean isDegenerate() {
        return totalLength == 0;
    }

    public Vec3 pointAtParameter(double u) {
        double t = Math.max(0, Math.min(1, u));
        Vec3 linear = Vec3.lerp(start, end, t);
        return new Vec3(linear.x(), linear.y() + 4 * height * t * (1 - t), linear.z());
    }

    public Vec3 pointAtDistance(double distance) {
        if (distance <= 0 || isDegenerate()) {
            return start;
        }
        if (distance >= totalLength) {
            return end;
        }
        int index = Arrays.binarySearch(lengths, distance);
        if (index >= 0) {
            return pointAtParameter((double) index / TABLE_SEGMENTS);
        }
        int upper = -index - 1;
        int lower = upper - 1;
        double segmentLength = lengths[upper] - lengths[lower];
        double fraction = segmentLength == 0 ? 0 : (distance - lengths[lower]) / segmentLength;
        double parameter = (lower + fraction) / TABLE_SEGMENTS;
        return pointAtParameter(parameter);
    }

    public Vec3 pointAtFraction(double fraction) {
        return pointAtDistance(Math.max(0, Math.min(1, fraction)) * totalLength);
    }
}
