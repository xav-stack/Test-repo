package dev.cannons.runtime;

import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.model.CannonId;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;
import java.util.UUID;

final class ProjectileFlight {
    enum Stage {
        FOLLOWING_ARC,
        ABOVE_BUILD_LIMIT,
        WAITING_FOR_REENTRY_CHUNKS
    }

    final UUID id;
    final TntEntity entity;
    final CannonId cannon;
    final ArcPath path;
    final ArcPath.AboveHeightInterval aboveHeightInterval;
    final Set<ChunkPos> tickets;
    double distance;
    int waitTicks;
    double remainingAboveDistance;
    Stage stage = Stage.FOLLOWING_ARC;

    ProjectileFlight(UUID id, TntEntity entity, CannonId cannon, ArcPath path,
                    ArcPath.AboveHeightInterval aboveHeightInterval, Set<ChunkPos> tickets) {
        this.id = id;
        this.entity = entity;
        this.cannon = cannon;
        this.path = path;
        this.aboveHeightInterval = aboveHeightInterval;
        this.tickets = tickets;
    }
}
