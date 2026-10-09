package dev.cannons.pure.geometry;

public interface CollisionQuery {
    boolean isLoaded(Box box);

    boolean intersectsSolid(Box box);
}
