package dev.cannons.pure;

import dev.cannons.pure.geometry.Vec3;
import dev.cannons.pure.logic.ChunkCorridor;
import dev.cannons.pure.logic.ChunkPos;
import dev.cannons.pure.logic.TicketReferenceCounter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChunkLogicTest {
    @Test
    void capsuleIncludesEndpointsAndPadding() {
        Set<ChunkPos> chunks = ChunkCorridor.calculate(
                new Vec3(8, 0, 8), new Vec3(40, 100, 8), 8);
        assertTrue(chunks.contains(new ChunkPos(0, 0)));
        assertTrue(chunks.contains(new ChunkPos(1, 0)));
        assertTrue(chunks.contains(new ChunkPos(2, 0)));
        assertTrue(chunks.contains(new ChunkPos(1, -1)));
        assertTrue(chunks.contains(new ChunkPos(1, 1)));
        assertFalse(chunks.contains(new ChunkPos(1, 2)));
        assertEquals(-1, ChunkCorridor.floorChunk(-0.01));
    }

    @Test
    void ticketsAreAddedAndRemovedOnlyAtReferenceTransitions() {
        List<String> transitions = new ArrayList<>();
        TicketReferenceCounter<ChunkPos> refs = new TicketReferenceCounter<>(
                (chunk, acquire) -> transitions.add(chunk + ":" + acquire));
        ChunkPos key = new ChunkPos(2, -3);
        assertEquals(1, refs.acquire(key));
        assertEquals(2, refs.acquire(key));
        assertEquals(1, refs.release(key));
        assertEquals(0, refs.release(key));
        assertEquals(List.of(key + ":true", key + ":false"), transitions);
    }
}
