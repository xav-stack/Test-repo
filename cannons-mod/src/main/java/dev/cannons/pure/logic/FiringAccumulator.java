package dev.cannons.pure.logic;

/** Pure per-cannon shot cadence. A due shot pauses accumulation until resolved. */
public final class FiringAccumulator {
    public static final int SHOT_INTERVAL = 1_200;
    private int progress;

    public int progress() {
        return progress;
    }

    public void start() {
        progress = SHOT_INTERVAL;
    }

    public void stop() {
        progress = 0;
    }

    public boolean isShotDue() {
        return progress >= SHOT_INTERVAL;
    }

    public boolean tick(int rpm) {
        if (progress < SHOT_INTERVAL) {
            progress += Rpm.clamp(rpm, 60);
        }
        return isShotDue();
    }

    public void resolveAttempt(boolean waitingForChunks) {
        if (isShotDue() && !waitingForChunks) {
            progress -= SHOT_INTERVAL;
        }
    }

    public static int ticksUntilNextShot(int progress, int rpm) {
        if (progress >= SHOT_INTERVAL) {
            return 0;
        }
        int clampedRpm = Rpm.clamp(rpm, 60);
        int remaining = SHOT_INTERVAL - Math.max(0, progress);
        return (remaining + clampedRpm - 1) / clampedRpm;
    }

    public static boolean hasShotDueWithin(
            boolean firing,
            int tntCount,
            int progress,
            int rpm,
            int retentionTicks
    ) {
        return firing && tntCount > 0
                && ticksUntilNextShot(progress, rpm) <= retentionTicks;
    }
}
