package dev.cannons.pure.geometry;

import java.util.Objects;
import java.util.random.RandomGenerator;

public final class SphereSampler {
    private SphereSampler() {
    }

    public static Vec3 sampleInside(RandomGenerator random, double radius) {
        Objects.requireNonNull(random, "random");
        if (!Double.isFinite(radius) || radius < 0) {
            throw new IllegalArgumentException("radius must be finite and non-negative");
        }
        double z = random.nextDouble(-1, 1);
        double angle = random.nextDouble(0, Math.PI * 2);
        double radial = Math.cbrt(random.nextDouble()) * radius;
        double planar = Math.sqrt(Math.max(0, 1 - z * z));
        return new Vec3(
                radial * planar * Math.cos(angle),
                radial * z,
                radial * planar * Math.sin(angle)
        );
    }
}
