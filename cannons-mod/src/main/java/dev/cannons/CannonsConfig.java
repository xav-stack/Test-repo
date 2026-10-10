package dev.cannons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CannonsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "cannons.json");

    public double projectileSpeedBlocksPerSecond = 30;
    public double refusalRadiusBlocks = 20;
    public double offsetRadiusBlocks = 8;
    public int chunkRetentionSeconds = 90;
    public double explosionPower = 4.0;
    public double lookRangeBlocks = 5;
    public int maxRpm = 60;
    public double arcHeightRatio = 0.25;
    public double collisionSubstepBlocks = 0.25;
    public int chunkWaitTimeoutTicks = 200;
    public int noticeThrottleSeconds = 1;
    public int batteryNameMaxLength = 16;

    public static CannonsConfig load() {
        CannonsConfig config = new CannonsConfig();
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) {
                Files.writeString(FILE, GSON.toJson(config));
                return config;
            }
            JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            CannonsConfig parsed = GSON.fromJson(json, CannonsConfig.class);
            if (parsed != null) config = parsed;
            config.validate();
            Files.writeString(FILE, GSON.toJson(config));
        } catch (IOException | RuntimeException error) {
            LOGGER.error("Unable to read or write config/cannons.json; using safe defaults", error);
            config = new CannonsConfig();
        }
        return config;
    }

    void validate() {
        projectileSpeedBlocksPerSecond = clamp("projectileSpeedBlocksPerSecond",
                projectileSpeedBlocksPerSecond, 0.1, 1000);
        refusalRadiusBlocks = clamp("refusalRadiusBlocks", refusalRadiusBlocks, 0, 512);
        offsetRadiusBlocks = clamp("offsetRadiusBlocks", offsetRadiusBlocks, 0, 64);
        chunkRetentionSeconds = clamp("chunkRetentionSeconds", chunkRetentionSeconds, 1, 3600);
        explosionPower = clamp("explosionPower", explosionPower, 0, 64);
        lookRangeBlocks = clamp("lookRangeBlocks", lookRangeBlocks, 0.5, 32);
        maxRpm = clamp("maxRpm", maxRpm, 1, 60);
        arcHeightRatio = clamp("arcHeightRatio", arcHeightRatio, 0, 10);
        collisionSubstepBlocks = clamp("collisionSubstepBlocks", collisionSubstepBlocks, 0.05, 0.98);
        chunkWaitTimeoutTicks = clamp("chunkWaitTimeoutTicks", chunkWaitTimeoutTicks, 1, 1200);
        noticeThrottleSeconds = clamp("noticeThrottleSeconds", noticeThrottleSeconds, 0, 60);
        batteryNameMaxLength = clamp("batteryNameMaxLength", batteryNameMaxLength, 1, 16);
    }

    private static double clamp(String name, double value, double min, double max) {
        double result = Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
        if (result != value) {
            LOGGER.warn("Config value {}={} is invalid; clamped to {}", name, value, result);
        }
        return result;
    }

    private static int clamp(String name, int value, int min, int max) {
        int result = Math.max(min, Math.min(max, value));
        if (result != value) {
            LOGGER.warn("Config value {}={} is invalid; clamped to {}", name, value, result);
        }
        return result;
    }
}
