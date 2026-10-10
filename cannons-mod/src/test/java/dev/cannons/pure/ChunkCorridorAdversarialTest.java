package dev.cannons.pure;

import dev.cannons.pure.geometry.Vec3;
import dev.cannons.pure.logic.ChunkCorridor;
import dev.cannons.pure.logic.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChunkCorridorAdversarialTest {
    @Test
    void optimizedCapsuleMatchesBruteForceReferenceAcrossRandomSegments() {
        Random random = new Random(0xCA770);
        for (int trial = 0; trial < 1_000; trial++) {
            Vec3 start = new Vec3(random.nextDouble(-160, 160), 0,
                    random.nextDouble(-160, 160));
            Vec3 end = new Vec3(random.nextDouble(-160, 160), 0,
                    random.nextDouble(-160, 160));
            double padding = random.nextDouble(0, 32);
            assertEquals(reference(start, end, padding),
                    ChunkCorridor.calculate(start, end, padding),
                    "trial=" + trial + ", start=" + start + ", end=" + end
                            + ", padding=" + padding);
        }
    }

    @Test
    void axisAlignedPointAndNegativeChunkBoundariesAreIncluded() {
        assertEquals(reference(new Vec3(0, 0, 0), new Vec3(0, 0, 0), 0),
                ChunkCorridor.calculate(new Vec3(0, 0, 0), new Vec3(0, 0, 0), 0));
        assertEquals(reference(new Vec3(-16, 0, 16), new Vec3(-16, 0, -16), 0),
                ChunkCorridor.calculate(new Vec3(-16, 0, 16), new Vec3(-16, 0, -16), 0));
        assertTrue(ChunkCorridor.calculate(
                new Vec3(-16, 0, 16), new Vec3(-16, 0, -16), 0)
                .contains(new ChunkPos(-2, 0)));
        assertTrue(ChunkCorridor.calculate(
                new Vec3(-16, 0, 16), new Vec3(-16, 0, -16), 0)
                .contains(new ChunkPos(-1, 0)));
    }

    @Test
    void millionBlockDiagonalStaysProportionalToTheCorridorNotItsBoundingSquare() {
        Vec3 start = new Vec3(-10.25, 0, -4.75);
        Vec3 end = new Vec3(999_989.75, 0, 599_995.25);
        Set<ChunkPos> corridor = ChunkCorridor.calculate(start, end, 0);
        assertTrue(corridor.size() > 50_000, "long path must cover its traversed chunks");
        assertTrue(corridor.size() < 300_000,
                "the corridor must not contain the full rectangular bounding area");
        assertTrue(corridor.contains(new ChunkPos(
                ChunkCorridor.floorChunk(start.x()), ChunkCorridor.floorChunk(start.z()))));
        assertTrue(corridor.contains(new ChunkPos(
                ChunkCorridor.floorChunk(end.x()), ChunkCorridor.floorChunk(end.z()))));
    }

    @Test
    void rejectsNonFiniteHorizontalEndpointsAndInvalidPadding() {
        assertThrows(IllegalArgumentException.class, () -> ChunkCorridor.calculate(
                new Vec3(Double.NaN, 0, 0), new Vec3(1, 0, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> ChunkCorridor.calculate(
                new Vec3(0, 0, 0), new Vec3(1, 0, Double.POSITIVE_INFINITY), 0));
        assertThrows(IllegalArgumentException.class, () -> ChunkCorridor.calculate(
                new Vec3(0, 0, 0), new Vec3(1, 0, 1), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> ChunkCorridor.calculate(
                new Vec3(0, 0, 0), new Vec3(1, 0, 1), -0.01));
    }

    private static Set<ChunkPos> reference(Vec3 start, Vec3 end, double padding) {
        int minX = floorLower(Math.min(start.x(), end.x()) - padding);
        int maxX = ChunkCorridor.floorChunk(Math.max(start.x(), end.x()) + padding);
        int minZ = floorLower(Math.min(start.z(), end.z()) - padding);
        int maxZ = ChunkCorridor.floorChunk(Math.max(start.z(), end.z()) + padding);
        Set<ChunkPos> result = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double minRectX = x * 16.0;
                double minRectZ = z * 16.0;
                if (segmentIntersectsRect(start.x(), start.z(), end.x(), end.z(),
                        minRectX, minRectZ, minRectX + 16, minRectZ + 16)
                        || segmentRectangleDistanceSquared(start.x(), start.z(),
                        end.x(), end.z(), minRectX, minRectZ, minRectX + 16, minRectZ + 16)
                        <= padding * padding) {
                    result.add(new ChunkPos(x, z));
                }
            }
        }
        return Set.copyOf(result);
    }

    private static int floorLower(double coordinate) {
        return (int) Math.floor(Math.nextDown(coordinate / 16.0));
    }

    private static boolean segmentIntersectsRect(double x0, double z0, double x1, double z1,
                                                  double minX, double minZ,
                                                  double maxX, double maxZ) {
        double[] interval = {0, 1};
        return clip(-(x1 - x0), x0 - minX, interval)
                && clip(x1 - x0, maxX - x0, interval)
                && clip(-(z1 - z0), z0 - minZ, interval)
                && clip(z1 - z0, maxZ - z0, interval);
    }

    private static boolean clip(double p, double q, double[] interval) {
        if (p == 0) return q >= 0;
        double ratio = q / p;
        if (p < 0) {
            if (ratio > interval[1]) return false;
            interval[0] = Math.max(interval[0], ratio);
        } else {
            if (ratio < interval[0]) return false;
            interval[1] = Math.min(interval[1], ratio);
        }
        return true;
    }

    private static double segmentRectangleDistanceSquared(
            double x0, double z0, double x1, double z1,
            double minX, double minZ, double maxX, double maxZ) {
        double best = Math.min(pointRectangleDistanceSquared(x0, z0,
                        minX, minZ, maxX, maxZ),
                pointRectangleDistanceSquared(x1, z1, minX, minZ, maxX, maxZ));
        best = Math.min(best, pointSegmentDistanceSquared(minX, minZ, x0, z0, x1, z1));
        best = Math.min(best, pointSegmentDistanceSquared(minX, maxZ, x0, z0, x1, z1));
        best = Math.min(best, pointSegmentDistanceSquared(maxX, minZ, x0, z0, x1, z1));
        return Math.min(best, pointSegmentDistanceSquared(maxX, maxZ, x0, z0, x1, z1));
    }

    private static double pointRectangleDistanceSquared(
            double x, double z, double minX, double minZ, double maxX, double maxZ) {
        double dx = Math.max(Math.max(minX - x, 0), x - maxX);
        double dz = Math.max(Math.max(minZ - z, 0), z - maxZ);
        return dx * dx + dz * dz;
    }

    private static double pointSegmentDistanceSquared(
            double px, double pz, double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0) {
            dx = px - x0;
            dz = pz - z0;
            return dx * dx + dz * dz;
        }
        double t = Math.max(0, Math.min(1,
                ((px - x0) * dx + (pz - z0) * dz) / lengthSquared));
        dx = px - (x0 + t * dx);
        dz = pz - (z0 + t * dz);
        return dx * dx + dz * dz;
    }
}
