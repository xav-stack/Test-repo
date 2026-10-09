package dev.cannons.pure.geometry;

public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public Box {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("box minimum must not exceed maximum");
        }
    }

    public static Box centered(Vec3 center, double width, double height) {
        double half = width / 2.0;
        return new Box(
                center.x() - half, center.y(), center.z() - half,
                center.x() + half, center.y() + height, center.z() + half
        );
    }
}
