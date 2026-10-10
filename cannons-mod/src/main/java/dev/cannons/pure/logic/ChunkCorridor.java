package dev.cannons.pure.logic;

import dev.cannons.pure.geometry.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Horizontal capsule-to-chunk intersection for cannon ticket corridors. */
public final class ChunkCorridor {
    private ChunkCorridor() {
    }

    public static Set<ChunkPos> calculate(Vec3 start, Vec3 end, double padding) {
        if (!Double.isFinite(padding) || padding < 0) {
            throw new IllegalArgumentException("padding must be finite and non-negative");
        }
        if (!Double.isFinite(start.x()) || !Double.isFinite(start.z())
                || !Double.isFinite(end.x()) || !Double.isFinite(end.z())) {
            throw new IllegalArgumentException("horizontal coordinates must be finite");
        }

        double deltaX = end.x() - start.x();
        double deltaZ = end.z() - start.z();
        boolean xIsMajor = Math.abs(deltaX) >= Math.abs(deltaZ);
        double majorStart = xIsMajor ? start.x() : start.z();
        double majorEnd = xIsMajor ? end.x() : end.z();
        double minorStart = xIsMajor ? start.z() : start.x();
        double minorEnd = xIsMajor ? end.z() : end.x();
        double majorDelta = majorEnd - majorStart;
        double minorDelta = minorEnd - minorStart;
        int minMajorChunk = floorChunkLower(Math.min(majorStart, majorEnd) - padding);
        int maxMajorChunk = floorChunk(Math.max(majorStart, majorEnd) + padding);
        Set<ChunkPos> result = new HashSet<>();
        for (int majorChunk = minMajorChunk; majorChunk <= maxMajorChunk; majorChunk++) {
            double lowMajor = majorChunk * 16.0;
            double highMajor = lowMajor + 16;
            double firstParameter;
            double lastParameter;
            if (majorDelta == 0) {
                if (majorStart < lowMajor - padding || majorStart > highMajor + padding) {
                    continue;
                }
                firstParameter = 0;
                lastParameter = 1;
            } else {
                double parameterA = (lowMajor - padding - majorStart) / majorDelta;
                double parameterB = (highMajor + padding - majorStart) / majorDelta;
                firstParameter = Math.max(0, Math.min(parameterA, parameterB));
                lastParameter = Math.min(1, Math.max(parameterA, parameterB));
                if (firstParameter > lastParameter) {
                    continue;
                }
            }
            double minorAtFirst = minorStart + minorDelta * firstParameter;
            double minorAtLast = minorStart + minorDelta * lastParameter;
            int minMinorChunk = floorChunkLower(
                    Math.min(minorAtFirst, minorAtLast) - padding);
            int maxMinorChunk = floorChunk(
                    Math.max(minorAtFirst, minorAtLast) + padding);

            for (int minorChunk = minMinorChunk; minorChunk <= maxMinorChunk; minorChunk++) {
                int chunkX = xIsMajor ? majorChunk : minorChunk;
                int chunkZ = xIsMajor ? minorChunk : majorChunk;
                double lowX = chunkX * 16.0;
                double lowZ = chunkZ * 16.0;
                double highX = lowX + 16;
                double highZ = lowZ + 16;
                if (segmentIntersectsRect(start.x(), start.z(), end.x(), end.z(),
                        lowX, lowZ, highX, highZ)
                        || segmentRectangleDistanceSquared(start.x(), start.z(), end.x(), end.z(),
                        lowX, lowZ, highX, highZ) <= padding * padding) {
                    result.add(new ChunkPos(chunkX, chunkZ));
                }
            }
        }
        return Set.copyOf(result);
    }

    public static int floorChunk(double coordinate) {
        return (int) Math.floor(coordinate / 16.0);
    }

    private static int floorChunkLower(double coordinate) {
        return (int) Math.floor(Math.nextDown(coordinate / 16.0));
    }

    private static boolean segmentIntersectsRect(
            double x0, double z0, double x1, double z1,
            double minX, double minZ, double maxX, double maxZ
    ) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double[] interval = {0, 1};
        return clip(-dx, x0 - minX, interval)
                && clip(dx, maxX - x0, interval)
                && clip(-dz, z0 - minZ, interval)
                && clip(dz, maxZ - z0, interval);
    }

    private static boolean clip(double p, double q, double[] interval) {
        if (p == 0) {
            return q >= 0;
        }
        double ratio = q / p;
        if (p < 0) {
            if (ratio > interval[1]) {
                return false;
            }
            interval[0] = Math.max(interval[0], ratio);
        } else {
            if (ratio < interval[0]) {
                return false;
            }
            interval[1] = Math.min(interval[1], ratio);
        }
        return true;
    }

    private static double segmentRectangleDistanceSquared(
            double x0, double z0, double x1, double z1,
            double minX, double minZ, double maxX, double maxZ
    ) {
        double best = Math.min(
                pointRectangleDistanceSquared(x0, z0, minX, minZ, maxX, maxZ),
                pointRectangleDistanceSquared(x1, z1, minX, minZ, maxX, maxZ)
        );
        best = Math.min(best, pointSegmentDistanceSquared(minX, minZ, x0, z0, x1, z1));
        best = Math.min(best, pointSegmentDistanceSquared(minX, maxZ, x0, z0, x1, z1));
        best = Math.min(best, pointSegmentDistanceSquared(maxX, minZ, x0, z0, x1, z1));
        return Math.min(best, pointSegmentDistanceSquared(maxX, maxZ, x0, z0, x1, z1));
    }

    private static double pointRectangleDistanceSquared(
            double x, double z, double minX, double minZ, double maxX, double maxZ
    ) {
        double dx = Math.max(Math.max(minX - x, 0), x - maxX);
        double dz = Math.max(Math.max(minZ - z, 0), z - maxZ);
        return dx * dx + dz * dz;
    }

    private static double pointSegmentDistanceSquared(
            double px, double pz, double x0, double z0, double x1, double z1
    ) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0) {
            dx = px - x0;
            dz = pz - z0;
            return dx * dx + dz * dz;
        }
        double t = Math.max(0, Math.min(1, ((px - x0) * dx + (pz - z0) * dz) / lengthSquared));
        double nearestX = x0 + t * dx;
        double nearestZ = z0 + t * dz;
        dx = px - nearestX;
        dz = pz - nearestZ;
        return dx * dx + dz * dz;
    }
}
