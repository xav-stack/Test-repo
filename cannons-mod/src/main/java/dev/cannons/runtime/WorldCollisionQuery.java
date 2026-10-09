package dev.cannons.runtime;

import dev.cannons.pure.geometry.Box;
import dev.cannons.pure.geometry.CollisionQuery;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

final class WorldCollisionQuery implements CollisionQuery {
    private final ServerWorld world;

    WorldCollisionQuery(ServerWorld world) {
        this.world = world;
    }

    @Override
    public boolean isLoaded(Box box) {
        int minChunkX = Math.floorDiv((int) Math.floor(box.minX()), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(box.maxX())), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(box.minZ()), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(box.maxZ())), 16);
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (!world.getChunkManager().isChunkLoaded(x, z)) return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersectsSolid(Box box) {
        net.minecraft.util.math.Box vanillaBox = new net.minecraft.util.math.Box(
                box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ()
        );
        return world.getBlockCollisions(null, vanillaBox).iterator().hasNext();
    }
}
