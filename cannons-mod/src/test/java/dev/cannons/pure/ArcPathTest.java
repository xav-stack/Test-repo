package dev.cannons.pure;

import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.geometry.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcPathTest {
    @Test
    void endpointsAndApexMatchTheSpecifiedCurve() {
        Vec3 start = new Vec3(0, 2, 0);
        Vec3 end = new Vec3(40, 12, 0);
        ArcPath path = new ArcPath(start, end, 0.25);

        assertEquals(start, path.pointAtParameter(0));
        assertEquals(end, path.pointAtParameter(1));
        assertEquals(0.25 * start.distanceTo(end), path.height(), 1e-12);
        assertEquals((start.y() + end.y()) / 2 + path.height(),
                path.pointAtParameter(0.5).y(), 1e-12);
    }

    @Test
    void straightUpAndDownPathsRemainWellDefined() {
        ArcPath up = new ArcPath(new Vec3(0, 0, 0), new Vec3(0, 30, 0), 0.25);
        ArcPath down = new ArcPath(new Vec3(0, 30, 0), new Vec3(0, 0, 0), 0.25);

        assertEquals(new Vec3(0, 30, 0), up.pointAtParameter(1));
        assertEquals(new Vec3(0, 0, 0), down.pointAtParameter(1));
        assertEquals(22.5, up.pointAtParameter(0.5).y(), 1e-12);
        assertEquals(22.5, down.pointAtParameter(0.5).y(), 1e-12);
    }

    @Test
    void zeroLengthPathIsDegenerate() {
        ArcPath path = new ArcPath(new Vec3(1, 1, 1), new Vec3(1, 1, 1), 0.25);
        assertTrue(path.isDegenerate());
        assertEquals(0, path.totalLength());
        assertEquals(path.start(), path.pointAtDistance(50));
    }

    @Test
    void arcLengthSteppingKeepsLongArcDeltasWithinTwoPercent() {
        ArcPath path = new ArcPath(new Vec3(0, 0, 0), new Vec3(70, 10, 0), 0.25);
        assertTrue(path.totalLength() >= 50);
        Vec3 previous = path.pointAtDistance(0);
        for (double distance = 1.5; distance < path.totalLength(); distance += 1.5) {
            Vec3 current = path.pointAtDistance(distance);
            assertEquals(1.5, previous.distanceTo(current), 0.03);
            previous = current;
        }
    }

    @Test
    void parameterDistanceMappingIsMonotonicAndClamped() {
        ArcPath path = new ArcPath(new Vec3(-20, 3, 4), new Vec3(90, -7, -16), 0.75);
        assertEquals(0, path.distanceAtParameter(-2));
        assertEquals(path.totalLength(), path.distanceAtParameter(2));
        double previous = -1;
        for (int i = 0; i <= 10_000; i++) {
            double distance = path.distanceAtParameter(i / 10_000.0);
            assertTrue(distance >= previous);
            assertTrue(distance <= path.totalLength());
            previous = distance;
        }
        assertEquals(path.totalLength(), path.distanceAtParameter(1));
    }

    @Test
    void distanceQueriesClampNegativeAndOversizedFractions() {
        ArcPath path = new ArcPath(new Vec3(1, 2, 3), new Vec3(40, 8, -9), 0.25);
        assertEquals(path.start(), path.pointAtDistance(-1));
        assertEquals(path.end(), path.pointAtDistance(path.totalLength() + 1));
        assertEquals(path.start(), path.pointAtFraction(-1));
        assertEquals(path.end(), path.pointAtFraction(2));
    }

    @Test
    void aboveHeightIntervalReportsBothExactArcIntersections() {
        ArcPath path = new ArcPath(new Vec3(0, 64, 0), new Vec3(100, 64, 0), 0.25);

        ArcPath.AboveHeightInterval interval = path.intervalAbove(80);

        assertNotNull(interval);
        assertTrue(interval.startParameter() > 0 && interval.startParameter() < 0.5);
        assertTrue(interval.endParameter() > 0.5 && interval.endParameter() < 1);
        assertEquals(80, interval.startPoint().y(), 1e-10);
        assertEquals(80, interval.endPoint().y(), 1e-10);
        assertEquals(interval.startParameter(),
                1 - interval.endParameter(), 1e-10);
        assertTrue(interval.endDistance() > interval.startDistance());
    }

    @Test
    void aboveHeightIntervalHandlesStartAboveAndNoCrossing() {
        ArcPath startsAbove = new ArcPath(new Vec3(0, 20, 0), new Vec3(40, 0, 0), 0.1);
        ArcPath entirelyBelow = new ArcPath(new Vec3(0, 0, 0), new Vec3(40, 5, 0), 0.1);
        ArcPath tangent = new ArcPath(new Vec3(0, 0, 0), new Vec3(40, 0, 0), 0.25);

        ArcPath.AboveHeightInterval interval = startsAbove.intervalAbove(10);

        assertNotNull(interval);
        assertEquals(0, interval.startDistance());
        assertEquals(10, interval.endPoint().y(), 1e-10);
        assertNull(entirelyBelow.intervalAbove(20));
        assertNull(tangent.intervalAbove(10));
    }
}
