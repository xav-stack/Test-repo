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
        if (!Double.isFinite(refusalRadius) || refusalRadius < 0
                || !Double.isFinite(stepLength) || stepLength <= 0) {
            throw new IllegalArgumentException("radius and step length are invalid");
        }
        if (!Double.isFinite(projectileWidth) || projectileWidth <= 0
                || !Double.isFinite(projectileHeight) || projectileHeight <= 0) {
            throw new IllegalArgumentException("projectile dimensions are invalid");
        }
        if (path.isDegenerate()) {
            return Result.BLOCKED;
        }

        double dx = path.end().x() - path.start().x();
        double dz = path.end().z() - path.start().z();
        double horizontalLengthSquared = dx * dx + dz * dz;
        double firstParameter = 0;
        double lastParameter = 1;
        double radiusSquared = refusalRadius * refusalRadius;
        if (horizontalLengthSquared == 0) {
            double offsetX = path.start().x() - cannonCenter.x();
            double offsetZ = path.start().z() - cannonCenter.z();
            if (offsetX * offsetX + offsetZ * offsetZ > radiusSquared) {
                return Result.CLEAR;
            }
        } else {
            double offsetX = cannonCenter.x() - path.start().x();
            double offsetZ = cannonCenter.z() - path.start().z();
            double closestParameter = Math.max(0, Math.min(1,
                    (offsetX * dx + offsetZ * dz) / horizontalLengthSquared));
            double closestX = path.start().x() + dx * closestParameter;
            double closestZ = path.start().z() + dz * closestParameter;
            double distanceX = closestX - cannonCenter.x();
            double distanceZ = closestZ - cannonCenter.z();
            double closestDistanceSquared = distanceX * distanceX + distanceZ * distanceZ;
            if (closestDistanceSquared > radiusSquared) {
                return Result.CLEAR;
            }
            double halfWidth = Math.sqrt(Math.max(0,
                    radiusSquared - closestDistanceSquared) / horizontalLengthSquared);
            firstParameter = Math.max(0, closestParameter - halfWidth);
            lastParameter = Math.min(1, closestParameter + halfWidth);
        }

        double firstDistance = path.distanceAtParameter(firstParameter);
        double lastDistance = path.distanceAtParameter(lastParameter);
        double safeStepLength = Math.min(stepLength, Math.min(projectileWidth, projectileHeight));
        long steps = Math.max(1L,
                (long) Math.ceil((lastDistance - firstDistance) / safeStepLength));
        for (long i = 0; i <= steps; i++) {
            double distance = firstDistance + (lastDistance - firstDistance) * i / steps;
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
