package dev.cannons.pure.geometry;

public final class ArcCollision {
    private ArcCollision() {
    }

    public enum Result {
        CLEAR,
        BLOCKED,
        WAITING_FOR_CHUNK
    }

    public static Result checkRefusalRadius(
            ArcPath path,
            Vec3 cannonCenter,
            double refusalRadius,
            double projectileWidth,
            double projectileHeight,
            double stepLength,
            CollisionQuery query
    ) {
        if (path.isDegenerate()) {
            return Result.BLOCKED;
        }
        if (refusalRadius < 0 || stepLength <= 0) {
            throw new IllegalArgumentException("radius and step length are invalid");
        }
        int steps = Math.max(1, (int) Math.ceil(path.totalLength() / stepLength));
        for (int i = 0; i <= steps; i++) {
            double distance = path.totalLength() * i / steps;
            Vec3 point = path.pointAtDistance(distance);
            if (point.distanceTo(cannonCenter) > refusalRadius) {
                continue;
            }
            Box box = Box.centered(point, projectileWidth, projectileHeight);
            if (!query.isLoaded(box)) {
                return Result.WAITING_FOR_CHUNK;
            }
            if (query.intersectsSolid(box)) {
                return Result.BLOCKED;
            }
        }
        return Result.CLEAR;
    }

    public static boolean intersects(Box box, Box obstacle) {
        return box.minX() < obstacle.maxX() && box.maxX() > obstacle.minX()
                && box.minY() < obstacle.maxY() && box.maxY() > obstacle.minY()
                && box.minZ() < obstacle.maxZ() && box.maxZ() > obstacle.minZ();
    }
}
