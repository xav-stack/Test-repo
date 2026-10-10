package dev.cannons.pure;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.cannons.pure.command.CommandCoordinates;
import dev.cannons.pure.command.CommandTrees;
import dev.cannons.pure.logic.BlockedNoticeThrottle;
import dev.cannons.pure.logic.OutOfAmmoAggregator;
import dev.cannons.pure.model.CannonId;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommandAndNotificationsTest {
    private static final class Source {
    }

    private static final UUID STARTER = UUID.fromString("7f3ee074-4bd3-4ef6-ae3d-002759137ccb");

    @Test
    void targetAllCommandAcceptsBothBranchesAndRejectsRelativeCoordinates() throws Exception {
        CommandDispatcher<Source> dispatcher = new CommandDispatcher<>();
        dispatcher.register(CommandTrees.targetAll("cta", context -> 1, context -> 2));
        assertEquals(1, dispatcher.execute("cta 1 2 3", new Source()));
        assertEquals(2, dispatcher.execute("cta main 1 2 3", new Source()));
        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("cta ~ 1 2", new Source()));
        assertThrows(CommandSyntaxException.class,
                () -> dispatcher.execute("cta 1 2", new Source()));
        assertTrue(Double.isInfinite(Double.parseDouble("1".repeat(400))));
    }

    @Test
    void coordinateValidationRejectsNonFiniteAndOutOfBoundsValues() {
        assertTrue(CommandCoordinates.areValid(0, 64, 0, -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(Double.POSITIVE_INFINITY, 64, 0,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(0, 320, 0, -100, 100, -100, 100, -64, 320));
    }

    @Test
    void coordinateBoundsAreInclusiveExceptForTheExclusiveTopBuildHeight() {
        assertTrue(CommandCoordinates.areValid(-100, -64, 100,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(-100.0001, 64, 0,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(100.0001, 64, 0,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(0, 64, -100.0001,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(0, 64, 100.0001,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(0, -64.0001, 0,
                -100, 100, -100, 100, -64, 320));
        assertFalse(CommandCoordinates.areValid(0, Double.NEGATIVE_INFINITY, 0,
                -100, 100, -100, 100, -64, 320));
    }

    @Test
    void blockedNoticesThrottleAndAggregateCounts() {
        BlockedNoticeThrottle throttle = new BlockedNoticeThrottle();
        throttle.record(STARTER);
        throttle.record(STARTER);
        assertEquals(Map.of(STARTER, 2), throttle.flushReady(5, 20));
        throttle.record(STARTER);
        assertTrue(throttle.flushReady(24, 20).isEmpty());
        assertEquals(Map.of(STARTER, 1), throttle.flushReady(25, 20));
    }

    @Test
    void emptyCannonsAreBatchedPerStarter() {
        OutOfAmmoAggregator aggregator = new OutOfAmmoAggregator();
        CannonId cannon = new CannonId("minecraft:overworld", 1, 2, 3);
        aggregator.record(STARTER, cannon);
        Map<UUID, OutOfAmmoAggregator.Notice> notices = aggregator.drainTick();
        assertEquals(1, notices.get(STARTER).count());
        assertEquals(cannon, notices.get(STARTER).onlyCannon());
        assertTrue(aggregator.drainTick().isEmpty());
    }

    @Test
    void blockedNoticeThrottleSeparatesStartersAndZeroDelayFlushesImmediately() {
        BlockedNoticeThrottle throttle = new BlockedNoticeThrottle();
        UUID secondStarter = UUID.fromString("a54d2202-f9ae-4a6b-a717-4e8c92e0ce66");
        throttle.record(STARTER);
        throttle.record(secondStarter);
        assertEquals(Map.of(STARTER, 1, secondStarter, 1),
                throttle.flushReady(0, 0));
        throttle.record(STARTER);
        assertEquals(Map.of(STARTER, 1), throttle.flushReady(1, 0));
    }
}
