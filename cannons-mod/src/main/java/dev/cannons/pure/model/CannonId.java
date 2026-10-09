package dev.cannons.pure.model;

public record CannonId(String dimension, int x, int y, int z) {
    public CannonId {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
    }
}
