package dev.cannons.runtime;

import dev.cannons.pure.geometry.Vec3;
import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.logic.FiringAccumulator;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class CannonRuntimeState {
    Vec3 target;
    final FiringAccumulator cadence = new FiringAccumulator();
    final Set<ChunkPos> corridorTickets = new HashSet<>();
    UUID starter;
    boolean firing;
    int shotWaitTicks;
    int cannonLoadTicketCount;
    ChunkPos cannonLoadTicketChunk;
    UUID pendingStarter;
    ArcPath pendingPath;
}
