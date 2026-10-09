package dev.cannons.pure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

/** Brigadier tree factory independent of Minecraft command-source classes. */
public final class CommandTrees<S> {
    private CommandTrees() {
    }

    public static <S> LiteralArgumentBuilder<S> targetAll(
            String literal,
            Command<S> defaultSelection,
            Command<S> namedBattery
    ) {
        LiteralArgumentBuilder<S> coordinates = LiteralArgumentBuilder.<S>literal(literal)
                .then(RequiredArgumentBuilder.<S, Double>argument("x", DoubleArgumentType.doubleArg())
                        .then(RequiredArgumentBuilder.<S, Double>argument("y", DoubleArgumentType.doubleArg())
                                .then(RequiredArgumentBuilder.<S, Double>argument("z", DoubleArgumentType.doubleArg())
                                        .executes(defaultSelection))));
        coordinates.then(RequiredArgumentBuilder.<S, String>argument(
                        "battery", StringArgumentType.word())
                .then(RequiredArgumentBuilder.<S, Double>argument("x", DoubleArgumentType.doubleArg())
                        .then(RequiredArgumentBuilder.<S, Double>argument("y", DoubleArgumentType.doubleArg())
                                .then(RequiredArgumentBuilder.<S, Double>argument("z", DoubleArgumentType.doubleArg())
                                        .executes(namedBattery)))));
        return coordinates;
    }
}
