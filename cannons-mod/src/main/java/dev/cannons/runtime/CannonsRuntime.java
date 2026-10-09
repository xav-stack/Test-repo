package dev.cannons.runtime;

import com.mojang.logging.LogUtils;
import dev.cannons.CannonsConfig;
import dev.cannons.CannonItems;
import dev.cannons.mixin.DisplayEntityAccessor;
import dev.cannons.mixin.TextDisplayEntityAccessor;
import dev.cannons.pure.geometry.ArcCollision;
import dev.cannons.pure.geometry.ArcPath;
import dev.cannons.pure.geometry.Box;
import dev.cannons.pure.geometry.SphereSampler;
import dev.cannons.pure.geometry.Vec3;
import dev.cannons.pure.logic.BlockedNoticeThrottle;
import dev.cannons.pure.logic.ChunkCorridor;
import dev.cannons.pure.logic.FiringAccumulator;
import dev.cannons.pure.logic.NameBinding;
import dev.cannons.pure.logic.OutOfAmmoAggregator;
import dev.cannons.pure.logic.Rpm;
import dev.cannons.pure.model.CannonId;
import dev.cannons.state.BatteriesState;
import dev.cannons.state.CannonRegistryState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CannonsRuntime {
    public static final String LABEL_TAG = "cannons_label";
    public static final String PROJECTILE_TAG = "cannons_projectile";
    public static final String LABEL_X_TAG = "cannons_x=";
    public static final String LABEL_Y_TAG = "cannons_y=";
    public static final String LABEL_Z_TAG = "cannons_z=";
    public static final String LABEL_DIM_TAG = "cannons_dimension=";
    public static final int TICKET_RADIUS = 2;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final net.minecraft.server.world.ChunkTicketType<ChunkPos> TICKET_TYPE =
            net.minecraft.server.world.ChunkTicketType.create(
                    "cannons", Comparator.comparingLong(ChunkPos::toLong));

    private final CannonsConfig config;
    private final Map<CannonId, CannonRuntimeState> cannons = new HashMap<>();
    private final Map<ServerWorld, dev.cannons.pure.logic.TicketReferenceCounter<ChunkPos>> tickets =
            new HashMap<>();
    private final Set<CannonId> recentlyRemoved = new HashSet<>();
    private final Map<CannonId, Set<UUID>> previousBindings = new HashMap<>();
    private final Map<CannonId, UUID> pendingLabelRecreation = new HashMap<>();
    private final Map<UUID, ProjectileFlight> projectiles = new HashMap<>();
    private final Set<UUID> liveProjectileIds = new HashSet<>();
    private final OutOfAmmoAggregator outOfAmmo = new OutOfAmmoAggregator();
    private final BlockedNoticeThrottle blockedNotices = new BlockedNoticeThrottle();
    private long serverTick;

    public CannonsRuntime(CannonsConfig config) {
        this.config = config;
    }

    public CannonsConfig config() {
        return config;
    }

    public static CannonId id(ServerWorld world, BlockPos pos) {
        return new CannonId(world.getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos pos(CannonId id) {
        return new BlockPos(id.x(), id.y(), id.z());
    }

    public boolean isRegistered(ServerWorld world, BlockPos pos) {
        return CannonRegistryState.get(world).contains(pos);
    }

    public boolean isRecentlyRemoved(ServerWorld world, BlockPos pos) {
        return recentlyRemoved.contains(id(world, pos));
    }

    public boolean shouldDropCannon(ServerWorld world, BlockPos pos) {
        return isRegistered(world, pos) || isRecentlyRemoved(world, pos);
    }

    public CannonRuntimeState state(ServerWorld world, BlockPos pos) {
        return cannons.computeIfAbsent(id(world, pos), ignored -> new CannonRuntimeState());
    }

    public CannonRuntimeState state(CannonId id) {
        return cannons.computeIfAbsent(id, ignored -> new CannonRuntimeState());
    }

    public void registerCannon(ServerWorld world, BlockPos pos) {
        CannonRegistryState registry = CannonRegistryState.get(world);
        registry.register(pos);
        state(world, pos);
        ensureLabel(world, pos);
    }

    public void unregisterCannon(ServerWorld world, BlockPos pos) {
        CannonId id = id(world, pos);
        CannonRegistryState registry = CannonRegistryState.get(world);
        UUID labelId = registry.labelUuid(pos);
        registry.unregister(pos);
        recentlyRemoved.add(id);
        CannonRuntimeState cannon = cannons.remove(id);
        if (cannon != null) {
            releaseCannonTickets(world, cannon);
        }
        if (labelId != null) {
            Entity label = world.getEntity(labelId);
            if (label != null) label.discard();
        }
        pendingLabelRecreation.remove(id);
        BatteriesState.get(world).removeCannonEverywhere(id);
        previousBindings.remove(id);
    }

    public void onChunkLoad(ServerWorld world, ChunkPos chunk) {
        CannonRegistryState registry = CannonRegistryState.get(world);
        for (BlockPos pos : registry.positionsInChunk(chunk)) {
            BlockState state = world.getBlockState(pos);
            if (!state.isOf(Blocks.BARREL)
                    || !(world.getBlockEntity(pos) instanceof BarrelBlockEntity)) {
                UUID labelId = registry.labelUuid(pos);
                registry.unregister(pos);
                CannonId id = id(world, pos);
                if (labelId != null) {
                    Entity label = world.getEntity(labelId);
                    if (label != null) label.discard();
                }
                cannonRemovedQuietly(world, pos, id);
                continue;
            }
        }
    }

    public void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        onChunkLoad(world, chunk.getPos());
    }

    private void cannonRemovedQuietly(ServerWorld world, BlockPos pos, CannonId id) {
        CannonRuntimeState cannon = cannons.remove(id);
        if (cannon != null) releaseCannonTickets(world, cannon);
        BatteriesState.get(world).removeCannonEverywhere(id);
        previousBindings.remove(id);
    }

    public void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity.getCommandTags().contains(PROJECTILE_TAG)) {
            if (!liveProjectileIds.contains(entity.getUuid())) {
                entity.discard();
            }
            return;
        }
        if (!entity.getCommandTags().contains(LABEL_TAG)) {
            return;
        }
        CannonId id = labelCannonId(entity);
        if (id == null || !id.dimension().equals(world.getRegistryKey().getValue().toString())) {
            entity.discard();
            return;
        }
        BlockPos pos = pos(id);
        CannonRegistryState registry = CannonRegistryState.get(world);
        if (!registry.contains(pos) || !world.getBlockState(pos).isOf(Blocks.BARREL)) {
            entity.discard();
            return;
        }
        UUID registeredLabel = registry.labelUuid(pos);
        if (registeredLabel == null) {
            registry.setLabelUuid(pos, entity.getUuid());
        } else if (!registeredLabel.equals(entity.getUuid())) {
            entity.discard();
        }
    }

    public void onEntityUnload(Entity entity, ServerWorld world) {
        if (entity.getCommandTags().contains(PROJECTILE_TAG)) {
            Entity.RemovalReason projectileReason = entity.getRemovalReason();
            if (projectileReason == Entity.RemovalReason.KILLED
                    || projectileReason == Entity.RemovalReason.DISCARDED) {
                ProjectileFlight flight = projectiles.remove(entity.getUuid());
                liveProjectileIds.remove(entity.getUuid());
                if (flight != null) release(world, flight.tickets);
            }
            return;
        }
        if (!entity.getCommandTags().contains(LABEL_TAG)) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) {
            return;
        }
        CannonId id = labelCannonId(entity);
        if (id != null) pendingLabelRecreation.put(id, entity.getUuid());
    }

    public void endServerTick(MinecraftServer server) {
        serverTick++;
        for (Map.Entry<CannonId, UUID> pending : new ArrayList<>(pendingLabelRecreation.entrySet())) {
            ServerWorld world = world(server, pending.getKey());
            if (world == null) continue;
            BlockPos pos = pos(pending.getKey());
            if (isRegistered(world, pos) && world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                pendingLabelRecreation.remove(pending.getKey());
                ensureLabel(world, pos);
            } else if (!isRegistered(world, pos)) {
                pendingLabelRecreation.remove(pending.getKey());
            }
        }
        outOfAmmo.drainTick().forEach((starterId, notice) -> {
            ServerPlayerEntity starter = server.getPlayerManager().getPlayer(starterId);
            if (starter == null) return;
            if (notice.count() == 1 && notice.onlyCannon() != null) {
                CannonId cannon = notice.onlyCannon();
                starter.sendMessage(Text.literal("Cannon out of TNT at "
                        + cannon.x() + ", " + cannon.y() + ", " + cannon.z()
                        + " (" + cannon.dimension() + ")."), false);
            } else {
                starter.sendMessage(Text.literal(notice.count()
                        + " cannon(s) out of TNT."), false);
            }
        });
        blockedNotices.flushReady(serverTick, config.noticeThrottleSeconds * 20L)
                .forEach((starterId, count) -> {
                    ServerPlayerEntity starter = server.getPlayerManager().getPlayer(starterId);
                    if (starter != null) {
                        starter.sendMessage(Text.literal(count + " shot(s) blocked."), true);
                    }
                });
        recentlyRemoved.clear();
    }

    public List<CannonId> allCannons(MinecraftServer server) {
        List<CannonId> result = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            for (BlockPos pos : CannonRegistryState.get(world).positions()) {
                result.add(id(world, pos));
            }
        }
        return result;
    }

    public ServerWorld world(MinecraftServer server, CannonId id) {
        Identifier dimension = Identifier.tryParse(id.dimension());
        if (dimension == null) return null;
        return server.getWorld(net.minecraft.registry.RegistryKey.of(
                net.minecraft.registry.RegistryKeys.WORLD, dimension));
    }

    public boolean isLoaded(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
                && world.getBlockEntity(pos) != null;
    }

    public boolean isBound(ServerWorld world, BlockPos pos, String username) {
        if (!(world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) return false;
        for (int slot = 18; slot <= 25; slot++) {
            var stack = barrel.getStack(slot);
            if (stack.hasCustomName()
                    && NameBinding.matches(stack.getName().getString(), username)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBound(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        return isBound(world, pos, player.getName().getString());
    }

    public Vec3 target(CannonId id) {
        return state(id).target;
    }

    public void setTarget(ServerWorld world, BlockPos pos, Vec3 target) {
        CannonRuntimeState cannon = state(world, pos);
        cannon.target = target;
        if (cannon.firing && isLoaded(world, pos)) {
            updateCannonCorridor(world, pos, cannon);
        }
    }

    public boolean isFiring(CannonId id) {
        CannonRuntimeState cannon = cannons.get(id);
        return cannon != null && cannon.firing;
    }

    public enum StartStatus {
        STARTED, QUEUED, ALREADY_QUEUED, STOPPED, NO_TARGET, NO_AMMO, NOT_REGISTERED
    }

    public StartStatus toggle(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        CannonId id = id(world, pos);
        CannonRuntimeState cannon = state(id);
        if (cannon.firing) {
            stop(world, pos);
            return StartStatus.STOPPED;
        }
        return startOrQueue(world, pos, player);
    }

    public StartStatus startOrQueue(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        CannonId id = id(world, pos);
        if (!isRegistered(world, pos)) return StartStatus.NOT_REGISTERED;
        CannonRuntimeState cannon = state(id);
        if (cannon.firing) return StartStatus.STARTED;
        if (cannon.pendingStarter != null) {
            cannon.pendingStarter = player.getUuid();
            return StartStatus.ALREADY_QUEUED;
        }
        if (!isLoaded(world, pos)) {
            queueStart(world, pos, player, cannon);
            return StartStatus.QUEUED;
        }
        if (cannon.target == null) return StartStatus.NO_TARGET;
        if (tntCount(world, pos) < 1) return StartStatus.NO_AMMO;
        startFiring(world, pos, player.getUuid(), cannon);
        return StartStatus.STARTED;
    }

    private void queueStart(ServerWorld world, BlockPos pos, ServerPlayerEntity player,
                            CannonRuntimeState cannon) {
        ChunkPos chunk = new ChunkPos(pos);
        cannon.pendingStarter = player.getUuid();
        if (cannon.cannonLoadTicketCount == 0) {
            cannon.cannonLoadTicketChunk = chunk;
            ticketRefs(world).acquire(chunk);
        }
        cannon.cannonLoadTicketCount++;
    }

    public void stop(ServerWorld world, BlockPos pos) {
        CannonRuntimeState cannon = cannons.get(id(world, pos));
        if (cannon != null) stop(world, cannon);
    }

    public void stop(MinecraftServer server, CannonId id) {
        ServerWorld world = world(server, id);
        if (world == null) return;
        CannonRuntimeState cannon = cannons.get(id);
        if (cannon != null) stop(world, cannon);
    }

    private void stop(ServerWorld world, CannonRuntimeState cannon) {
        cannon.firing = false;
        cannon.starter = null;
        cannon.pendingStarter = null;
        cannon.pendingPath = null;
        cannon.shotWaitTicks = 0;
        cannon.cadence.stop();
        releaseCannonTickets(world, cannon);
    }

    private void startFiring(ServerWorld world, BlockPos pos, UUID starter,
                             CannonRuntimeState cannon) {
        cannon.pendingStarter = null;
        cannon.firing = true;
        cannon.starter = starter;
        cannon.cadence.start();
        cannon.pendingPath = null;
        cannon.shotWaitTicks = 0;
        updateCannonCorridor(world, pos, cannon);
    }

    public int tntCount(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) return 0;
        int count = 0;
        for (int slot = 0; slot < 18; slot++) {
            ItemStack stack = barrel.getStack(slot);
            if (stack.isOf(Items.TNT)) count += stack.getCount();
        }
        return count;
    }

    public int rpm(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) return 1;
        return Rpm.clamp(barrel.getStack(26).isOf(Items.EMERALD)
                ? barrel.getStack(26).getCount() : 0, config.maxRpm);
    }

    public void tickWorld(ServerWorld world) {
        for (BlockPos pos : CannonRegistryState.get(world).positions()) {
            if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (!world.getBlockState(pos).isOf(Blocks.BARREL)) {
                unregisterCannon(world, pos);
                continue;
            }
            if (!(world.getBlockEntity(pos) instanceof BarrelBlockEntity)) {
                unregisterCannon(world, pos);
                continue;
            }
            pruneUnboundPlayers(world, pos);
            CannonRuntimeState cannon = state(world, pos);
            if (cannon.pendingStarter != null) {
                completePendingStart(world, pos, cannon);
            }
            if (!cannon.firing) continue;
            int ammo = tntCount(world, pos);
            if (ammo == 0) {
                UUID starter = cannon.starter;
                stop(world, cannon);
                if (starter != null) outOfAmmo.record(starter, id(world, pos));
                continue;
            }
            cannon.cadence.tick(rpm(world, pos));
            updateCannonCorridor(world, pos, cannon);
            if (!cannon.cadence.isShotDue()) continue;
            boolean waiting = attemptShot(world, pos, cannon);
            cannon.cadence.resolveAttempt(waiting);
            updateCannonCorridor(world, pos, cannon);
        }
        tickProjectiles(world);
    }

    private void completePendingStart(ServerWorld world, BlockPos pos, CannonRuntimeState cannon) {
        if (!isLoaded(world, pos)) return;
        UUID starterId = cannon.pendingStarter;
        cannon.pendingStarter = null;
        ServerPlayerEntity starter = world.getServer().getPlayerManager().getPlayer(starterId);
        if (!isRegistered(world, pos) || starter == null) {
            releasePendingTicket(world, cannon);
            return;
        }
        if (!isBound(world, pos, starter)) {
            starter.sendMessage(Text.literal("You are no longer bound to this cannon."), false);
        } else if (cannon.target == null) {
            starter.sendMessage(Text.literal("This cannon has no target."), false);
        } else if (tntCount(world, pos) < 1) {
            starter.sendMessage(Text.literal("This cannon has no TNT."), false);
        } else {
            startFiring(world, pos, starterId, cannon);
        }
        releasePendingTicket(world, cannon);
    }

    private void releasePendingTicket(ServerWorld world, CannonRuntimeState cannon) {
        if (cannon.cannonLoadTicketCount > 0 && cannon.cannonLoadTicketChunk != null) {
            for (int i = 0; i < cannon.cannonLoadTicketCount; i++) {
                ticketRefs(world).release(cannon.cannonLoadTicketChunk);
            }
        }
        cannon.cannonLoadTicketCount = 0;
        cannon.cannonLoadTicketChunk = null;
    }

    private void pruneUnboundPlayers(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) return;
        CannonId id = id(world, pos);
        Set<UUID> current = new HashSet<>();
        MinecraftServer server = world.getServer();
        for (int slot = 18; slot <= 25; slot++) {
            ItemStack stack = barrel.getStack(slot);
            if (!stack.hasCustomName()) continue;
            String name = stack.getName().getString();
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(name);
            if (online != null) {
                current.add(online.getUuid());
            } else if (server.getUserCache() != null) {
                server.getUserCache().findByName(name).ifPresent(profile -> current.add(profile.getId()));
            }
        }
        Set<UUID> previous = previousBindings.put(id, current);
        Set<UUID> owners = BatteriesState.get(world).owners(id);
        Set<UUID> mustRemove = new HashSet<>();
        if (previous != null) {
            for (UUID player : previous) {
                if (!current.contains(player)) mustRemove.add(player);
            }
        }
        for (UUID player : owners) {
            if (!current.contains(player)) {
                mustRemove.add(player);
            }
        }
        for (UUID player : mustRemove) BatteriesState.get(world).pruneOnUnbind(player, id);
    }

    private boolean attemptShot(ServerWorld world, BlockPos pos, CannonRuntimeState cannon) {
        if (cannon.target == null) {
            cannon.pendingPath = null;
            recordBlocked(cannon);
            return false;
        }
        if (cannon.pendingPath == null) {
            Vec3 muzzle = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            Vec3 offset = SphereSampler.sampleInside(
                    new java.util.Random(world.random.nextLong()), config.offsetRadiusBlocks);
            Vec3 aim = clampAim(world, cannon.target.add(offset));
            ArcPath path = new ArcPath(muzzle, aim, config.arcHeightRatio);
            if (path.isDegenerate()) {
                recordBlocked(cannon);
                return false;
            }
            cannon.pendingPath = path;
            cannon.shotWaitTicks = 0;
        }

        ArcPath path = cannon.pendingPath;
        WorldCollisionQuery collision = new WorldCollisionQuery(world);
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        ArcCollision.Result refusal = ArcCollision.checkRefusalRadius(
                path, center, config.refusalRadiusBlocks, 0.98, 0.98,
                config.collisionSubstepBlocks, collision);
        if (refusal == ArcCollision.Result.WAITING_FOR_CHUNK) {
            cannon.shotWaitTicks++;
            if (cannon.shotWaitTicks >= config.chunkWaitTimeoutTicks) {
                cannon.pendingPath = null;
                cannon.shotWaitTicks = 0;
                return false;
            }
            return true;
        }
        cannon.pendingPath = null;
        cannon.shotWaitTicks = 0;
        if (refusal == ArcCollision.Result.BLOCKED) {
            recordBlocked(cannon);
            return false;
        }
        if (!consumeTnt(world, pos)) return false;
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1, 0, 0, 0, 0);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        spawnProjectile(world, id(world, pos), path, cannon.starter);
        return false;
    }

    private void recordBlocked(CannonRuntimeState cannon) {
        if (cannon.starter != null) blockedNotices.record(cannon.starter);
    }

    private boolean consumeTnt(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) return false;
        for (int slot = 0; slot < 18; slot++) {
            ItemStack stack = barrel.getStack(slot);
            if (stack.isOf(Items.TNT) && !stack.isEmpty()) {
                stack.decrement(1);
                if (stack.isEmpty()) barrel.setStack(slot, ItemStack.EMPTY);
                barrel.markDirty();
                return true;
            }
        }
        return false;
    }

    private Vec3 clampAim(ServerWorld world, Vec3 aim) {
        var border = world.getWorldBorder();
        double x = Math.max(border.getBoundWest(), Math.min(border.getBoundEast(), aim.x()));
        double z = Math.max(border.getBoundNorth(), Math.min(border.getBoundSouth(), aim.z()));
        double y = Math.max(world.getBottomY(),
                Math.min(world.getTopY() - 1.0E-6, aim.y()));
        return new Vec3(x, y, z);
    }

    private void spawnProjectile(ServerWorld world, CannonId cannonId, ArcPath path, UUID starterId) {
        ServerPlayerEntity starter = starterId == null
                ? null : world.getServer().getPlayerManager().getPlayer(starterId);
        TntEntity entity = new TntEntity(world, path.start().x(), path.start().y(),
                path.start().z(), starter);
        entity.setNoGravity(true);
        entity.setVelocity(Vec3d.ZERO);
        entity.setFuse(32767);
        entity.addCommandTag(PROJECTILE_TAG);
        Set<ChunkPos> corridor = projectileCorridor(path);
        acquire(world, corridor);
        ProjectileFlight flight = new ProjectileFlight(
                entity.getUuid(), entity, cannonId, path, corridor);
        projectiles.put(entity.getUuid(), flight);
        liveProjectileIds.add(entity.getUuid());
        if (!world.spawnEntity(entity)) {
            removeProjectile(world, flight, false);
        }
    }

    private Set<ChunkPos> projectileCorridor(ArcPath path) {
        int blastReach = (int) Math.ceil(2 * config.explosionPower);
        return toMinecraftChunks(ChunkCorridor.calculate(path.start(), path.end(), blastReach));
    }

    private void tickProjectiles(ServerWorld world) {
        for (ProjectileFlight flight : new ArrayList<>(projectiles.values())) {
            if (flight.entity.getWorld() != world || flight.entity.isRemoved()) continue;
            double nextTickDistance = Math.min(flight.path.totalLength(),
                    flight.distance + config.projectileSpeedBlocksPerSecond / 20.0);
            double delta = nextTickDistance - flight.distance;
            int steps = Math.max(1, (int) Math.ceil(delta / config.collisionSubstepBlocks));
            double startDistance = flight.distance;
            boolean waiting = false;
            boolean hit = false;
            double currentDistance = flight.distance;
            WorldCollisionQuery collision = new WorldCollisionQuery(world);
            for (int i = 1; i <= steps && delta > 0; i++) {
                double distance = startDistance + delta * i / steps;
                Vec3 point = flight.path.pointAtDistance(distance);
                Box box = Box.centered(point, 0.98, 0.98);
                if (!collision.isLoaded(box)) {
                    waiting = true;
                    break;
                }
                if (collision.intersectsSolid(box)) {
                    hit = true;
                    break;
                }
                currentDistance = distance;
            }
            flight.distance = currentDistance;
            Vec3 current = flight.path.pointAtDistance(currentDistance);
            flight.entity.setPosition(current.x(), current.y(), current.z());
            flight.entity.updateTrackedPosition(current.x(), current.y(), current.z());
            flight.entity.velocityDirty = true;
            world.getChunkManager().sendToNearbyPlayers(
                    flight.entity, new EntityPositionS2CPacket(flight.entity));
            if (waiting) {
                flight.waitTicks++;
                if (flight.waitTicks >= config.chunkWaitTimeoutTicks) {
                    LOGGER.warn("Discarded cannon projectile {} after waiting {} ticks for an unloaded chunk",
                            flight.id, flight.waitTicks);
                    removeProjectile(world, flight, true);
                }
                continue;
            }
            flight.waitTicks = 0;
            if (hit || currentDistance >= flight.path.totalLength()) {
                detonate(world, flight, current);
            }
        }
    }

    private void detonate(ServerWorld world, ProjectileFlight flight, Vec3 position) {
        flight.entity.setPosition(position.x(), position.y(), position.z());
        world.createExplosion(flight.entity, position.x(), flight.entity.getBodyY(0.0625),
                position.z(), (float) config.explosionPower, World.ExplosionSourceType.TNT);
        removeProjectile(world, flight, true);
    }

    private void removeProjectile(ServerWorld world, ProjectileFlight flight, boolean discard) {
        projectiles.remove(flight.id);
        liveProjectileIds.remove(flight.id);
        release(world, flight.tickets);
        if (discard && !flight.entity.isRemoved()) flight.entity.discard();
    }

    private void updateCannonCorridor(ServerWorld world, BlockPos pos, CannonRuntimeState cannon) {
        int ammo = tntCount(world, pos);
        int rpm = rpm(world, pos);
        boolean retain = FiringAccumulator.hasShotDueWithin(
                cannon.firing, ammo, cannon.cadence.progress(), rpm,
                config.chunkRetentionSeconds * 20);
        Set<ChunkPos> desired = new HashSet<>();
        if (retain && cannon.target != null) {
            Vec3 muzzle = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            int blastReach = (int) Math.ceil(2 * config.explosionPower);
            desired.addAll(toMinecraftChunks(ChunkCorridor.calculate(
                    muzzle, cannon.target, config.offsetRadiusBlocks + blastReach)));
        }
        if (retain && cannon.pendingPath != null) {
            desired.addAll(projectileCorridor(cannon.pendingPath));
        }
        for (ChunkPos chunk : new HashSet<>(cannon.corridorTickets)) {
            if (!desired.contains(chunk)) {
                ticketRefs(world).release(chunk);
                cannon.corridorTickets.remove(chunk);
            }
        }
        for (ChunkPos chunk : desired) {
            if (cannon.corridorTickets.add(chunk)) ticketRefs(world).acquire(chunk);
        }
    }

    private Set<ChunkPos> toMinecraftChunks(Set<dev.cannons.pure.logic.ChunkPos> chunks) {
        return chunks.stream().map(chunk -> new ChunkPos(chunk.x(), chunk.z())).collect(Collectors.toSet());
    }

    private void acquire(ServerWorld world, Set<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) ticketRefs(world).acquire(chunk);
    }

    private void release(ServerWorld world, Set<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) ticketRefs(world).release(chunk);
    }

    public void onCannonItemPlaced(ServerWorld world, BlockPos pos) {
        registerCannon(world, pos);
    }

    private void ensureLabel(ServerWorld world, BlockPos pos) {
        CannonRegistryState registry = CannonRegistryState.get(world);
        if (!registry.contains(pos)
                || !world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        UUID existingId = registry.labelUuid(pos);
        Entity existing = existingId == null ? null : world.getEntity(existingId);
        if (existing instanceof DisplayEntity.TextDisplayEntity
                && existing.getCommandTags().contains(LABEL_TAG)) {
            return;
        }
        if (existingId != null) {
            registry.setLabelUuid(pos, null);
        }

        DisplayEntity.TextDisplayEntity label = EntityType.TEXT_DISPLAY.create(world);
        if (label == null) {
            LOGGER.error("Could not create text display for cannon at {}", pos);
            return;
        }
        label.setPosition(pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5);
        ((DisplayEntityAccessor) label).cannons$setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        ((TextDisplayEntityAccessor) label).cannons$setText(Text.literal("cannon"));
        label.setInvulnerable(true);
        label.addCommandTag(LABEL_TAG);
        label.addCommandTag(LABEL_X_TAG + pos.getX());
        label.addCommandTag(LABEL_Y_TAG + pos.getY());
        label.addCommandTag(LABEL_Z_TAG + pos.getZ());
        label.addCommandTag(LABEL_DIM_TAG + world.getRegistryKey().getValue());
        if (world.spawnEntity(label)) {
            registry.setLabelUuid(pos, label.getUuid());
        }
    }

    private CannonId labelCannonId(Entity entity) {
        Integer x = tagInt(entity, LABEL_X_TAG);
        Integer y = tagInt(entity, LABEL_Y_TAG);
        Integer z = tagInt(entity, LABEL_Z_TAG);
        String dimension = tagValue(entity, LABEL_DIM_TAG);
        if (x == null || y == null || z == null || dimension == null) return null;
        return new CannonId(dimension, x, y, z);
    }

    private static Integer tagInt(Entity entity, String prefix) {
        String value = tagValue(entity, prefix);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String tagValue(Entity entity, String prefix) {
        return entity.getCommandTags().stream()
                .filter(tag -> tag.startsWith(prefix))
                .map(tag -> tag.substring(prefix.length()))
                .findFirst().orElse(null);
    }

    private void releaseCannonTickets(ServerWorld world, CannonRuntimeState cannon) {
        dev.cannons.pure.logic.TicketReferenceCounter<ChunkPos> refs = ticketRefs(world);
        for (ChunkPos chunk : cannon.corridorTickets) refs.release(chunk);
        cannon.corridorTickets.clear();
        while (cannon.cannonLoadTicketCount > 0) {
            if (cannon.cannonLoadTicketChunk != null) {
                refs.release(cannon.cannonLoadTicketChunk);
            }
            cannon.cannonLoadTicketCount--;
        }
        cannon.cannonLoadTicketChunk = null;
        cannon.cannonLoadTicketCount = 0;
    }

    private dev.cannons.pure.logic.TicketReferenceCounter<ChunkPos> ticketRefs(ServerWorld world) {
        return tickets.computeIfAbsent(world, key ->
                new dev.cannons.pure.logic.TicketReferenceCounter<>((chunk, add) -> {
                    if (add) {
                        key.getChunkManager().addTicket(TICKET_TYPE, chunk, TICKET_RADIUS, chunk);
                    } else {
                        key.getChunkManager().removeTicket(TICKET_TYPE, chunk, TICKET_RADIUS, chunk);
                    }
                }));
    }

    public void stopAll(MinecraftServer server) {
        for (ProjectileFlight flight : new ArrayList<>(projectiles.values())) {
            if (!flight.entity.isRemoved()) flight.entity.discard();
            liveProjectileIds.remove(flight.id);
        }
        projectiles.clear();
        tickets.values().forEach(dev.cannons.pure.logic.TicketReferenceCounter::clear);
        tickets.clear();
        cannons.clear();
        previousBindings.clear();
        pendingLabelRecreation.clear();
        recentlyRemoved.clear();
    }

    public void unloadWorld(ServerWorld world) {
        for (ProjectileFlight flight : new ArrayList<>(projectiles.values())) {
            if (flight.entity.getWorld() == world) {
                projectiles.remove(flight.id);
                liveProjectileIds.remove(flight.id);
                if (!flight.entity.isRemoved()) flight.entity.discard();
            }
        }
        dev.cannons.pure.logic.TicketReferenceCounter<ChunkPos> refs = tickets.remove(world);
        if (refs != null) refs.clear();
        String dimension = world.getRegistryKey().getValue().toString();
        cannons.keySet().removeIf(id -> id.dimension().equals(dimension));
        previousBindings.keySet().removeIf(id -> id.dimension().equals(dimension));
        pendingLabelRecreation.keySet().removeIf(id -> id.dimension().equals(dimension));
    }
}
