package dev.cannons.pure;

import dev.cannons.pure.geometry.ArcCollision;
import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.geometry.Box;
import dev.cannons.pure.geometry.CollisionQuery;
import dev.cannons.pure.geometry.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private ArcCollision.Result checkAgainstX(Vec3 start, Vec3 end, double obstacleX) {
        ArcPath path = new ArcPath(start, end, 0);
        return ArcCollision.checkRefusalRadius(path, CANNON_CENTER, 20, 0.98, 0.98, 0.25,
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
