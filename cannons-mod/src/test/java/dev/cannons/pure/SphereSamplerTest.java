package dev.cannons.pure;

import dev.cannons.pure.geometry.SphereSampler;
import dev.cannons.pure.geometry.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SphereSamplerTest {
    @Test
    void seededSamplesAreUniformByVolumeAndCentered() {
        Random random = new Random(12345);
        int insideHalfRadius = 0;
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        for (int i = 0; i < 100_000; i++) {
            Vec3 sample = SphereSampler.sampleInside(random, 8);
            double radius = sample.length();
            assertTrue(radius <= 8 + 1e-12);
            if (radius < 4) {
                insideHalfRadius++;
            }
            sumX += sample.x();
            sumY += sample.y();
            sumZ += sample.z();
        }
        assertEquals(0.125, insideHalfRadius / 100_000.0, 0.01);
        assertEquals(0, sumX / 100_000.0, 0.05);
        assertEquals(0, sumY / 100_000.0, 0.05);
        assertEquals(0, sumZ / 100_000.0, 0.05);
    }
}
