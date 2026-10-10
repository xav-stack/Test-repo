package dev.cannons.pure;

import dev.cannons.pure.geometry.ArcCollision;
import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.geometry.Box;
import dev.cannons.pure.geometry.CollisionQuery;
import dev.cannons.pure.geometry.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcCollisionTest {
    private static final Vec3 CANNON_CENTER = new Vec3(0.5, 0.5, 0.5);
    private static final Vec3 MUZZLE = new Vec3(0.5, 1, 0.5);

    @Test
    void obstacleAtNineteenBlocksRefusesShotButAtTwentyOneDoesNot() {
        assertEquals(ArcCollision.Result.BLOCKED, checkAgainstX(MUZZLE, new Vec3(30.5, 1, 0.5), 19));
        assertEquals(ArcCollision.Result.CLEAR, checkAgainstX(MUZZLE, new Vec3(30.5, 1, 0.5), 21));
    }

    @Test
    void targetBlockInsideRefusalRadiusAndInitialOverlapAreRefused() {
        assertEquals(ArcCollision.Result.BLOCKED,
                checkAgainstX(MUZZLE, new Vec3(10.5, 1, 0.5), 10));
        ArcPath path = new ArcPath(MUZZLE, new Vec3(10.5, 1, 0.5), 0);
        assertEquals(ArcCollision.Result.BLOCKED,
                ArcCollision.checkRefusalRadius(path, CANNON_CENTER, 20, 0.98, 0.98, 0.25,
                        new CollisionQuery() {
                            @Override
                            public boolean isLoaded(Box box) {
                                return true;
                            }

                            @Override
                            public boolean intersectsSolid(Box box) {
                                return true;
                            }
                        }));
    }

    @Test
    void downwardArcIntersectsTheBarrel() {
        ArcPath path = new ArcPath(MUZZLE, new Vec3(0.5, -8, 0.5), 0);
        assertEquals(ArcCollision.Result.BLOCKED,
                ArcCollision.checkRefusalRadius(path, CANNON_CENTER, 20, 0.98, 0.98, 0.25,
                        new CollisionQuery() {
                            @Override
                            public boolean isLoaded(Box box) {
                                return true;
                            }

                            @Override
                            public boolean intersectsSolid(Box box) {
                                return box.minY() <= 0.875 && box.maxY() > 0
                                        && box.minX() < 1 && box.maxX() > 0
                                        && box.minZ() < 1 && box.maxZ() > 0;
                            }
                        }));
    }

    @Test
    void unloadedChunksOnlyPauseWhenTheArcEntersTheRefusalRadius() {
        ArcPath path = new ArcPath(MUZZLE, new Vec3(100, 1, 0.5), 0);
        CollisionQuery unloaded = new CollisionQuery() {
            @Override
            public boolean isLoaded(Box box) {
                return false;
            }

            @Override
            public boolean intersectsSolid(Box box) {
                throw new AssertionError("An unloaded box must not be collision-tested");
            }
        };
        assertEquals(ArcCollision.Result.WAITING_FOR_CHUNK,
                ArcCollision.checkRefusalRadius(path, CANNON_CENTER, 20,
                        0.98, 0.98, 0.25, unloaded));

        ArcPath distant = new ArcPath(new Vec3(100, 1, 0.5), new Vec3(200, 1, 0.5), 0);
        assertEquals(ArcCollision.Result.CLEAR,
                ArcCollision.checkRefusalRadius(distant, CANNON_CENTER, 20,
                        0.98, 0.98, 0.25, unloaded));
    }

    @Test
    void invalidRadiusAndNonFiniteOrNonPositiveStepAreRejected() {
        ArcPath path = new ArcPath(MUZZLE, new Vec3(10.5, 1, 0.5), 0);
        CollisionQuery clear = new CollisionQuery() {
            @Override
            public boolean isLoaded(Box box) {
                return true;
            }

            @Override
            public boolean intersectsSolid(Box box) {
                return false;
            }
        };
        assertThrows(IllegalArgumentException.class,
                () -> ArcCollision.checkRefusalRadius(path, CANNON_CENTER,
                        Double.NaN, 0.98, 0.98, 0.25, clear));
        assertThrows(IllegalArgumentException.class,
                () -> ArcCollision.checkRefusalRadius(path, CANNON_CENTER,
                        20, 0.98, 0.98, Double.POSITIVE_INFINITY, clear));
        assertThrows(IllegalArgumentException.class,
                () -> ArcCollision.checkRefusalRadius(path, CANNON_CENTER,
                        20, 0.98, 0.98, 0, clear));
        assertThrows(IllegalArgumentException.class,
                () -> ArcCollision.checkRefusalRadius(path, CANNON_CENTER,
                        20, 0, 0.98, 0.25, clear));
    }

    @Test
    void coarseConfiguredStepCannotSkipAOneBlockWall() {
        Vec3 end = new Vec3(30.5, 1, 0.5);
        assertEquals(ArcCollision.Result.BLOCKED,
                checkAgainstX(MUZZLE, end, 9, 2.0));
    }

    private ArcCollision.Result checkAgainstX(Vec3 start, Vec3 end, double obstacleX) {
        return checkAgainstX(start, end, obstacleX, 0.25);
    }

    private ArcCollision.Result checkAgainstX(
            Vec3 start, Vec3 end, double obstacleX, double stepLength) {
        ArcPath path = new ArcPath(start, end, 0);
        return ArcCollision.checkRefusalRadius(path, CANNON_CENTER, 20, 0.98, 0.98, stepLength,
                new CollisionQuery() {
                    @Override
                    public boolean isLoaded(Box box) {
                        return true;
                    }

                    @Override
                    public boolean intersectsSolid(Box box) {
                        return box.minX() < obstacleX + 1
                                && box.maxX() > obstacleX;
                    }
                });
    }
}
