package dev.cannons.pure;

import dev.cannons.pure.logic.FiringAccumulator;
import dev.cannons.pure.logic.Rpm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FiringAccumulatorTest {
    @Test
    void cadenceDoesNotDriftForEveryRpmOverOneHundredThousandTicks() {
        for (int rpm = 1; rpm <= 60; rpm++) {
            FiringAccumulator accumulator = new FiringAccumulator();
            accumulator.start();
            accumulator.resolveAttempt(false);
            int shots = 1;
            for (int tick = 1; tick <= 100_000; tick++) {
                if (accumulator.tick(rpm)) {
                    shots++;
                    accumulator.resolveAttempt(false);
                }
                assertEquals(1 + (int) ((long) tick * rpm / 1_200), shots,
                        "rpm=" + rpm + ", tick=" + tick);
            }
        }
    }

    @Test
    void liveRpmChangeAndWaitingShotDoNotAccumulateProgress() {
        FiringAccumulator accumulator = new FiringAccumulator();
        accumulator.start();
        accumulator.resolveAttempt(false);
        accumulator.tick(1);
        int beforeWait = accumulator.progress();
        assertFalse(accumulator.tick(50));
        assertEquals(beforeWait + 50, accumulator.progress());

        while (!accumulator.tick(60)) {
            // Accumulate until the next shot is due.
        }
        int dueProgress = accumulator.progress();
        for (int i = 0; i < 100; i++) {
            assertTrue(accumulator.tick(60));
            accumulator.resolveAttempt(true);
            assertEquals(dueProgress, accumulator.progress());
        }
        accumulator.resolveAttempt(false);
        assertEquals(dueProgress - FiringAccumulator.SHOT_INTERVAL, accumulator.progress());
    }

    @Test
    void rpmClampingAndRetentionBoundariesAreCorrect() {
        assertEquals(1, Rpm.clamp(0, 60));
        assertEquals(60, Rpm.clamp(61, 60));
        assertEquals(60, Rpm.clamp(64, 60));
        assertTrue(FiringAccumulator.hasShotDueWithin(true, 1, 0, 1, 1_800));
        assertFalse(FiringAccumulator.hasShotDueWithin(true, 1, 0, 1, 1_199));
        assertFalse(FiringAccumulator.hasShotDueWithin(true, 0, 1_200, 60, 1_800));
        assertFalse(FiringAccumulator.hasShotDueWithin(false, 1, 1_200, 60, 1_800));
    }

    @Test
    void startAndStopResetProgressAndAWaitingAttemptPausesExactlyAtTheThreshold() {
        FiringAccumulator accumulator = new FiringAccumulator();
        accumulator.start();
        accumulator.tick(17);
        accumulator.start();
        assertEquals(FiringAccumulator.SHOT_INTERVAL, accumulator.progress());
        assertTrue(accumulator.isShotDue());
        accumulator.resolveAttempt(true);
        assertEquals(FiringAccumulator.SHOT_INTERVAL, accumulator.progress());
        accumulator.resolveAttempt(false);
        assertEquals(0, accumulator.progress());

        accumulator.tick(60);
        accumulator.stop();
        assertEquals(0, accumulator.progress());
        assertFalse(accumulator.isShotDue());
        accumulator.tick(1);
        assertEquals(1, accumulator.progress());
    }

    @Test
    void rpmSixtyFiresOnTheTwentiethTickAndNoMoreThanOneShotPerTick() {
        FiringAccumulator accumulator = new FiringAccumulator();
        accumulator.start();
        accumulator.resolveAttempt(false);
        int shots = 0;
        for (int tick = 1; tick <= 40; tick++) {
            if (accumulator.tick(60)) {
                shots++;
                accumulator.resolveAttempt(false);
            }
            assertEquals(tick / 20, shots);
        }
        assertEquals(0, accumulator.progress());
    }
}
