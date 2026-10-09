package dev.cannons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import dev.cannons.command.CannonsCommands;
import dev.cannons.runtime.CannonsRuntime;
import net.minecraft.util.Identifier;

public final class CannonsMod implements ModInitializer {
    public static final String MOD_ID = "cannons";
    private static CannonsRuntime runtime;

    @Override
    public void onInitialize() {
        runtime = new CannonsRuntime(CannonsConfig.load());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CannonsCommands.register(dispatcher, runtime));
        ServerTickEvents.END_WORLD_TICK.register(runtime::tickWorld);
        ServerTickEvents.END_SERVER_TICK.register(runtime::endServerTick);
        ServerChunkEvents.CHUNK_LOAD.register(runtime::onChunkLoad);
        ServerEntityEvents.ENTITY_LOAD.register(runtime::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(runtime::onEntityUnload);
        ServerLifecycleEvents.SERVER_STOPPING.register(runtime::stopAll);
        ServerWorldEvents.UNLOAD.register((server, world) -> runtime.unloadWorld(world));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handler.player.unlockRecipes(new Identifier[]{new Identifier(MOD_ID, "cannon")}));
    }

    public static CannonsRuntime runtime() {
        if (runtime == null) {
            throw new IllegalStateException("Cannons runtime is not initialized");
        }
        return runtime;
    }
}
