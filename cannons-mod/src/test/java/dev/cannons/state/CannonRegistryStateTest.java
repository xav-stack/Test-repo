package dev.cannons.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CannonRegistryStateTest {
    @Test
    void registrationIsIdempotentAndChunkIndexAndLabelsSurvivePersistence() {
        BlockPos negative = new BlockPos(-17, 70, -1);
        UUID label = UUID.fromString("4a938320-2cbe-4c04-b48f-335133bb56ab");
        CannonRegistryState registry = new CannonRegistryState();
        assertTrue(registry.register(negative));
        assertFalse(registry.register(negative));
        registry.setLabelUuid(negative, label);

        CannonRegistryState loaded = CannonRegistryState.fromNbt(
                registry.writeNbt(new NbtCompound()));
        assertTrue(loaded.contains(negative));
        assertEquals(label, loaded.labelUuid(negative));
        assertTrue(loaded.positionsInChunk(new ChunkPos(-2, -1)).contains(negative));
        assertEquals(1, loaded.positions().size());

        assertTrue(loaded.unregister(negative));
        assertFalse(loaded.unregister(negative));
        assertFalse(loaded.contains(negative));
        assertTrue(loaded.positionsInChunk(new ChunkPos(-2, -1)).isEmpty());
    }
}
