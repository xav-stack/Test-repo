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

    public double distanceAtParameter(double parameter) {
        double t = Math.max(0, Math.min(1, parameter));
        if (t == 1) {
            return totalLength;
        }
        double scaled = t * TABLE_SEGMENTS;
        int lower = (int) scaled;
        double fraction = scaled - lower;
        return lengths[lower] + (lengths[lower + 1] - lengths[lower]) * fraction;
    }

    /**
     * Returns the one interval in which this concave-down arc is above the given height,
     * or {@code null} if it never rises above it. Endpoints are the exact arc intersections.
     */
    public AboveHeightInterval intervalAbove(double yLimit) {
        if (!Double.isFinite(yLimit)) {
            throw new IllegalArgumentException("height limit must be finite");
        }
        double slope = end.y() - start.y();
        double peakParameter;
        if (height == 0) {
            peakParameter = slope >= 0 ? 1 : 0;
        } else {
            peakParameter = clamp01((slope + 4 * height) / (8 * height));
        }
        if (pointAtParameter(peakParameter).y() <= yLimit) {
            return null;
        }

        double startParameter = start.y() > yLimit
                ? 0 : risingIntersection(0, peakParameter, yLimit);
        double endParameter = end.y() > yLimit
                ? 1 : fallingIntersection(peakParameter, 1, yLimit);
        double startDistance = distanceAtParameter(startParameter);
        double endDistance = distanceAtParameter(endParameter);
        return new AboveHeightInterval(
                startParameter,
                endParameter,
                startDistance,
                endDistance,
                pointAtParameter(startParameter),
                pointAtParameter(endParameter));
    }

    private double risingIntersection(double below, double above, double yLimit) {
        for (int i = 0; i < 64; i++) {
            double middle = (below + above) * 0.5;
            if (pointAtParameter(middle).y() > yLimit) {
                above = middle;
            } else {
                below = middle;
            }
        }
        return (below + above) * 0.5;
    }

    private double fallingIntersection(double above, double below, double yLimit) {
        for (int i = 0; i < 64; i++) {
            double middle = (above + below) * 0.5;
            if (pointAtParameter(middle).y() > yLimit) {
                above = middle;
            } else {
                below = middle;
            }
        }
        return (above + below) * 0.5;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    public Vec3 pointAtFraction(double fraction) {
        return pointAtDistance(Math.max(0, Math.min(1, fraction)) * totalLength);
    }

    public record AboveHeightInterval(
            double startParameter,
            double endParameter,
            double startDistance,
            double endDistance,
            Vec3 startPoint,
            Vec3 endPoint
    ) {
    }
}
