package dev.cannons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CannonsConfigTest {
    @Test
    void invalidAndExtremeSettingsClampToSafeRanges() {
        CannonsConfig config = new CannonsConfig();
        config.projectileSpeedBlocksPerSecond = Double.NaN;
        config.refusalRadiusBlocks = Double.POSITIVE_INFINITY;
        config.offsetRadiusBlocks = -1;
        config.chunkRetentionSeconds = Integer.MAX_VALUE;
        config.explosionPower = -1;
        config.lookRangeBlocks = Double.POSITIVE_INFINITY;
        config.maxRpm = 0;
        config.arcHeightRatio = Double.NaN;
        config.collisionSubstepBlocks = 2;
        config.chunkWaitTimeoutTicks = 0;
        config.noticeThrottleSeconds = Integer.MAX_VALUE;
        config.batteryNameMaxLength = 0;
        config.validate();

        assertEquals(0.1, config.projectileSpeedBlocksPerSecond);
        assertEquals(0, config.refusalRadiusBlocks);
        assertEquals(0, config.offsetRadiusBlocks);
        assertEquals(3600, config.chunkRetentionSeconds);
        assertEquals(0, config.explosionPower);
        assertEquals(0.5, config.lookRangeBlocks);
        assertEquals(1, config.maxRpm);
        assertEquals(0, config.arcHeightRatio);
        assertEquals(0.98, config.collisionSubstepBlocks);
        assertEquals(1, config.chunkWaitTimeoutTicks);
        assertEquals(60, config.noticeThrottleSeconds);
        assertEquals(1, config.batteryNameMaxLength);
    }

    @Test
    void eachSettingAllowsItsDocumentedExtremes() {
        CannonsConfig config = new CannonsConfig();
        config.projectileSpeedBlocksPerSecond = 1000;
        config.refusalRadiusBlocks = 512;
        config.offsetRadiusBlocks = 64;
        config.chunkRetentionSeconds = 3600;
        config.explosionPower = 64;
        config.lookRangeBlocks = 32;
        config.maxRpm = 60;
        config.arcHeightRatio = 10;
        config.collisionSubstepBlocks = 0.98;
        config.chunkWaitTimeoutTicks = 1200;
        config.noticeThrottleSeconds = 60;
        config.batteryNameMaxLength = 16;
        config.validate();

        assertEquals(1000, config.projectileSpeedBlocksPerSecond);
        assertEquals(512, config.refusalRadiusBlocks);
        assertEquals(64, config.offsetRadiusBlocks);
        assertEquals(3600, config.chunkRetentionSeconds);
        assertEquals(64, config.explosionPower);
        assertEquals(32, config.lookRangeBlocks);
        assertEquals(60, config.maxRpm);
        assertEquals(10, config.arcHeightRatio);
        assertEquals(0.98, config.collisionSubstepBlocks);
        assertEquals(1200, config.chunkWaitTimeoutTicks);
        assertEquals(60, config.noticeThrottleSeconds);
        assertEquals(16, config.batteryNameMaxLength);
    }
}
