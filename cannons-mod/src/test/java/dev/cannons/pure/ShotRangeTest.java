package dev.cannons.pure;

import dev.cannons.pure.geometry.ShotRange;
import dev.cannons.pure.geometry.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShotRangeTest {
    @Test
    void rangeLimitUsesThreeDimensionalEuclideanDistanceAndIncludesBoundary() {
        Vec3 origin = new Vec3(0, 0, 0);

        assertTrue(ShotRange.allows(origin, new Vec3(1000, 0, 0), 1000));
        assertFalse(ShotRange.allows(origin, new Vec3(1000.01, 0, 0), 1000));
        assertFalse(ShotRange.allows(origin, new Vec3(800, 800, 0), 1000));
    }

    @Test
    void unlimitedRangeAllowsTargetsAndLeavesTheyUnchanged() {
        Vec3 origin = new Vec3(1, 2, 3);
        Vec3 target = new Vec3(1_000_000, -500, 900_000);

        assertTrue(ShotRange.allows(origin, target, ShotRange.UNLIMITED));
        assertEquals(target, ShotRange.clamp(origin, target, ShotRange.UNLIMITED));
    }

    @Test
    void clampPlacesAnAimExactlyOnTheConfiguredSphere() {
        Vec3 origin = new Vec3(2, 3, 4);
        Vec3 clamped = ShotRange.clamp(origin, new Vec3(2000, 3, 4), 1000);

        assertEquals(1000, origin.distanceTo(clamped), 1e-10);
    }

    @Test
    void rejectsInvalidNegativeRangeSentinels() {
        assertThrows(IllegalArgumentException.class,
                () -> ShotRange.allows(new Vec3(0, 0, 0), new Vec3(1, 0, 0), -2));
    }
}
