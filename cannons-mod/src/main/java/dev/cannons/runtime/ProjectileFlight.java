package dev.cannons.runtime;

import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.model.CannonId;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;
import java.util.UUID;

final class ProjectileFlight {
    final UUID id;
    final TntEntity entity;
    final CannonId cannon;
    final ArcPath path;
    final Set<ChunkPos> tickets;
    double distance;
    int waitTicks;

    ProjectileFlight(UUID id, TntEntity entity, CannonId cannon, ArcPath path,
                    Set<ChunkPos> tickets) {
        this.id = id;
        this.entity = entity;
        this.cannon = cannon;
        this.path = path;
        this.tickets = tickets;
    }
}
