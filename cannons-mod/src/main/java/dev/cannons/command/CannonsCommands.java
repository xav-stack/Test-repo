package dev.cannons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.cannons.CannonItems;
import dev.cannons.pure.command.CommandCoordinates;
import dev.cannons.pure.command.CommandTrees;
import dev.cannons.pure.geometry.Vec3;
import dev.cannons.pure.logic.Batteries;
import dev.cannons.pure.model.CannonId;
import dev.cannons.runtime.CannonsRuntime;
import dev.cannons.runtime.CannonsRuntime.StartStatus;
import dev.cannons.state.BatteriesState;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public final class CannonsCommands {
    private CannonsCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CannonsRuntime runtime) {
        dispatcher.register(targetCommand("cannontarget", runtime));
        dispatcher.register(targetCommand("ct", runtime));
        dispatcher.register(CommandTrees.targetAll(
                "cannontargetall",
                context -> targetAll(context, runtime, null),
                context -> targetAll(context, runtime,
                        StringArgumentType.getString(context, "battery"))));
        dispatcher.register(CommandTrees.targetAll(
                "cta",
                context -> targetAll(context, runtime, null),
                context -> targetAll(context, runtime,
                        StringArgumentType.getString(context, "battery"))));
        dispatcher.register(fireCommand("cannonfire", runtime));
        dispatcher.register(fireCommand("cf", runtime));
        dispatcher.register(fireAllCommand("cannonfireall", runtime));
        dispatcher.register(fireAllCommand("cfa", runtime));
        dispatcher.register(batteryCommand("cannonbattery", runtime));
        dispatcher.register(batteryCommand("cb", runtime));
        dispatcher.register(giveCommand());
    }

    private static LiteralArgumentBuilder<ServerCommandSource> targetCommand(
            String literal, CannonsRuntime runtime) {
        return CommandManager.literal(literal)
                .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                                .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            ServerPlayerEntity player = player(context.getSource());
                                            if (player == null) return 0;
                                            Vec3 target = coordinates(context.getSource(),
                                                    DoubleArgumentType.getDouble(context, "x"),
                                                    DoubleArgumentType.getDouble(context, "y"),
                                                    DoubleArgumentType.getDouble(context, "z"));
                                            if (target == null) return 0;
                                            LookResult look = lookAtBoundCannon(player, runtime);
                                            if (!look.valid()) {
                                                context.getSource().sendError(Text.literal(look.error()));
                                                return 0;
                                            }
                                            runtime.setTarget(player.getServerWorld(), look.pos(), target);
                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Cannon target set to "
                                                            + target.x() + ", " + target.y() + ", "
                                                            + target.z() + "."), false);
                                            return 1;
                                        }))));
    }

    private static int targetAll(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
                                 CannonsRuntime runtime,
                                 String batteryName) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        Vec3 target = coordinates(source, DoubleArgumentType.getDouble(context, "x"),
                DoubleArgumentType.getDouble(context, "y"),
                DoubleArgumentType.getDouble(context, "z"));
        if (target == null) return 0;

        List<CannonId> selected;
        if (batteryName == null) {
            selected = runtime.allCannons(source.getServer());
        } else {
            if (!Batteries.isValidName(batteryName, runtime.config().batteryNameMaxLength)) {
                source.sendError(Text.literal("Invalid battery name."));
                return 0;
            }
            var members = BatteriesState.get(player.getServerWorld())
                    .members(player.getUuid(), batteryName);
            if (members == null) {
                source.sendError(Text.literal("Unknown battery: " + batteryName + "."));
                return 0;
            }
            selected = new ArrayList<>(members);
        }

        int changed = 0;
        int otherDimension = 0;
        int unloaded = 0;
        int unbound = 0;
        for (CannonId id : selected) {
            if (!id.dimension().equals(player.getServerWorld().getRegistryKey().getValue().toString())) {
                otherDimension++;
                continue;
            }
            ServerWorld world = runtime.world(source.getServer(), id);
            BlockPos pos = CannonsRuntime.pos(id);
            if (world == null || !runtime.isLoaded(world, pos)) {
                unloaded++;
                continue;
            }
            if (!runtime.isBound(world, pos, player)) {
                unbound++;
                continue;
            }
            runtime.setTarget(world, pos, target);
            changed++;
        }
        int finalChanged = changed;
        int finalOtherDimension = otherDimension;
        int finalUnloaded = unloaded;
        int finalUnbound = unbound;
        source.sendFeedback(() -> Text.literal("Set target on " + finalChanged + " cannon(s); skipped "
                + finalOtherDimension + " in other dimensions, " + finalUnloaded + " unloaded, and "
                + finalUnbound + " unbound."), false);
        return changed;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> fireCommand(
            String literal, CannonsRuntime runtime) {
        return CommandManager.literal(literal).executes(context -> {
            ServerPlayerEntity player = player(context.getSource());
            if (player == null) return 0;
            LookResult look = lookAtBoundCannon(player, runtime);
            if (!look.valid()) {
                context.getSource().sendError(Text.literal(look.error()));
                return 0;
            }
            StartStatus status = runtime.toggle(player.getServerWorld(), look.pos(), player);
            return reportStartStatus(context.getSource(), status);
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> fireAllCommand(
            String literal, CannonsRuntime runtime) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal(literal)
                .executes(context -> fireAll(context.getSource(), runtime, null));
        command.then(CommandManager.argument("battery", StringArgumentType.word())
                .executes(context -> fireAll(context.getSource(), runtime,
                        StringArgumentType.getString(context, "battery"))));
        return command;
    }

    private static int fireAll(ServerCommandSource source, CannonsRuntime runtime,
                               String batteryName) {
        ServerPlayerEntity player = player(source);
        if (player == null) return 0;
        List<CannonId> selected;
        if (batteryName == null) {
            selected = runtime.allCannons(source.getServer());
        } else {
            if (!Batteries.isValidName(batteryName, runtime.config().batteryNameMaxLength)) {
                source.sendError(Text.literal("Invalid battery name."));
                return 0;
            }
            var members = BatteriesState.get(player.getServerWorld())
                    .members(player.getUuid(), batteryName);
            if (members == null) {
                source.sendError(Text.literal("Unknown battery: " + batteryName + "."));
                return 0;
            }
            selected = new ArrayList<>(members);
        }

        List<CannonId> eligibleLoaded = new ArrayList<>();
        List<CannonId> eligibleUnloaded = new ArrayList<>();
        int unbound = 0;
        int missingWorld = 0;
        for (CannonId id : selected) {
            ServerWorld world = runtime.world(source.getServer(), id);
            if (world == null) {
                missingWorld++;
                continue;
            }
            BlockPos pos = CannonsRuntime.pos(id);
            if (!runtime.isLoaded(world, pos)) {
                eligibleUnloaded.add(id);
            } else if (runtime.isBound(world, pos, player)) {
                eligibleLoaded.add(id);
            } else {
                unbound++;
            }
        }

        List<CannonId> firing = eligibleLoaded.stream().filter(runtime::isFiring).toList();
        if (!firing.isEmpty()) {
            firing.forEach(id -> runtime.stop(source.getServer(), id));
            int stopped = firing.size();
            int skippedUnbound = unbound;
            int skippedWorlds = missingWorld;
            source.sendFeedback(() -> Text.literal("Stopped " + stopped + " cannon(s); skipped "
                    + skippedUnbound + " unbound and " + skippedWorlds + " unavailable."), false);
            return stopped;
        }

        int started = 0;
        int queued = 0;
        int noTarget = 0;
        int noAmmo = 0;
        int skipped = 0;
        for (CannonId id : eligibleLoaded) {
            ServerWorld world = runtime.world(source.getServer(), id);
            if (world == null) {
                skipped++;
                continue;
            }
            StartStatus status = runtime.startOrQueue(world, CannonsRuntime.pos(id), player);
            switch (status) {
                case STARTED -> started++;
                case QUEUED, ALREADY_QUEUED -> queued++;
                case NO_TARGET -> noTarget++;
                case NO_AMMO -> noAmmo++;
                default -> skipped++;
            }
        }
        for (CannonId id : eligibleUnloaded) {
            ServerWorld world = runtime.world(source.getServer(), id);
            if (world == null) {
                skipped++;
                continue;
            }
            StartStatus status = runtime.startOrQueue(world, CannonsRuntime.pos(id), player);
            switch (status) {
                case QUEUED, ALREADY_QUEUED -> queued++;
                case STARTED -> started++;
                case NO_TARGET -> noTarget++;
                case NO_AMMO -> noAmmo++;
                default -> skipped++;
            }
        }
        int result = started + queued;
        int finalStarted = started;
        int finalQueued = queued;
        int finalNoTarget = noTarget;
        int finalNoAmmo = noAmmo;
        int finalSkipped = unbound + missingWorld + skipped;
        source.sendFeedback(() -> Text.literal("Started " + finalStarted + ", queued " + finalQueued
                + ", no target " + finalNoTarget + ", no TNT " + finalNoAmmo + "; skipped "
                + finalSkipped + " unbound or unavailable cannon(s)."), false);
        return result;
    }

    private static int reportStartStatus(ServerCommandSource source, StartStatus status) {
        switch (status) {
            case STARTED -> source.sendFeedback(() -> Text.literal("Cannon firing."), false);
            case STOPPED -> source.sendFeedback(() -> Text.literal("Cannon stopped."), false);
            case QUEUED -> source.sendFeedback(() -> Text.literal("Cannon is loading before firing."), false);
            case ALREADY_QUEUED -> source.sendFeedback(() -> Text.literal("Cannon is already loading."), false);
            case NO_TARGET -> source.sendError(Text.literal("This cannon has no target."));
            case NO_AMMO -> source.sendError(Text.literal("This cannon has no TNT."));
            case NOT_REGISTERED -> source.sendError(Text.literal("Not looking at a cannon."));
        }
        return switch (status) {
            case STARTED, STOPPED, QUEUED, ALREADY_QUEUED -> 1;
            default -> 0;
        };
    }

    private static LiteralArgumentBuilder<ServerCommandSource> batteryCommand(
            String literal, CannonsRuntime runtime) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(literal);
        root.then(CommandManager.literal("list").executes(context -> {
            ServerPlayerEntity player = player(context.getSource());
            if (player == null) return 0;
            var batteries = BatteriesState.get(player.getServerWorld()).list(player.getUuid());
            if (batteries.isEmpty()) {
                context.getSource().sendFeedback(() -> Text.literal("You have no batteries."), false);
            } else {
                String text = batteries.entrySet().stream()
                        .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                        .collect(java.util.stream.Collectors.joining(", "));
                context.getSource().sendFeedback(() -> Text.literal("Batteries: " + text), false);
            }
            return batteries.size();
        }));
        root.then(batteryMutation("add", runtime));
        root.then(batteryMutation("remove", runtime));
        return root;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> batteryMutation(
            String action, CannonsRuntime runtime) {
        return CommandManager.literal(action)
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayerEntity player = player(context.getSource());
                            if (player == null) return 0;
                            String name = StringArgumentType.getString(context, "name");
                            if (!Batteries.isValidName(name, runtime.config().batteryNameMaxLength)) {
                                context.getSource().sendError(Text.literal("Invalid battery name."));
                                return 0;
                            }
                            LookResult look = lookAtBoundCannon(player, runtime);
                            if (!look.valid()) {
                                context.getSource().sendError(Text.literal(look.error()));
                                return 0;
                            }
                            BatteriesState batteries = BatteriesState.get(player.getServerWorld());
                            CannonId id = CannonsRuntime.id(player.getServerWorld(), look.pos());
                            boolean changed = action.equals("add")
                                    ? batteries.add(player.getUuid(), name, id)
                                    : batteries.remove(player.getUuid(), name, id);
                            if (!changed) {
                                context.getSource().sendError(Text.literal(action.equals("add")
                                        ? "Cannon is already in that battery."
                                        : "Cannon is not in that battery."));
                                return 0;
                            }
                            context.getSource().sendFeedback(() -> Text.literal(action.equals("add")
                                    ? "Added cannon to battery " + name + "."
                                    : "Removed cannon from battery " + name + "."), false);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> giveCommand() {
        return CommandManager.literal("cannongive")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity self
                            ? self : null;
                    if (player == null) {
                        context.getSource().sendError(Text.literal("A player argument is required from console."));
                        return 0;
                    }
                    give(player);
                    context.getSource().sendFeedback(() -> Text.literal("Gave you a cannon."), false);
                    return 1;
                })
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            give(target);
                            context.getSource().sendFeedback(() ->
                                    Text.literal("Gave " + target.getName().getString() + " a cannon."), false);
                            return 1;
                        }));
    }

    private static void give(ServerPlayerEntity player) {
        ItemStack stack = CannonItems.create();
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    private static Vec3 coordinates(ServerCommandSource source, double x, double y, double z) {
        ServerPlayerEntity player = player(source);
        if (player == null) return null;
        ServerWorld world = player.getServerWorld();
        var border = world.getWorldBorder();
        if (!CommandCoordinates.areValid(x, y, z,
                border.getBoundWest(), border.getBoundEast(),
                border.getBoundNorth(), border.getBoundSouth(),
                world.getBottomY(), world.getTopY())) {
            source.sendError(Text.literal("Invalid coordinates for this world."));
            return null;
        }
        return new Vec3(x, y, z);
    }

    private record LookResult(boolean valid, BlockPos pos, String error) {
    }

    private static LookResult lookAtBoundCannon(ServerPlayerEntity player, CannonsRuntime runtime) {
        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d end = start.add(player.getRotationVec(1.0F).multiply(runtime.config().lookRangeBlocks));
        BlockHitResult hit = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK
                || !runtime.isRegistered(world, hit.getBlockPos())) {
            return new LookResult(false, null, "Not looking at a cannon.");
        }
        if (!runtime.isBound(world, hit.getBlockPos(), player)) {
            return new LookResult(false, null, "You are not bound to this cannon.");
        }
        return new LookResult(true, hit.getBlockPos(), null);
    }

    private static ServerPlayerEntity player(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return player;
        }
        source.sendError(Text.literal("Players only"));
        return null;
    }
}
