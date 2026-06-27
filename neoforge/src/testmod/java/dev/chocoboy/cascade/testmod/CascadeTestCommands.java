package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import dev.chocoboy.cascade.neoforge.Vfx;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CascadeTestCommands {

    private CascadeTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxtest")
                .then(Commands.literal("burst").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vfx.burst(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("beam").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vec3 from = player.getEyePosition();
                    Vec3 to = from.add(player.getLookAngle().scale(10.0));
                    Vfx.beam(player.serverLevel(), from, to);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("combo").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vec3 from = player.getEyePosition();
                    Vec3 to = from.add(player.getLookAngle().scale(10.0));
                    Vfx.at(player.serverLevel())
                            .beam(from, to)
                            .delay(8)
                            .burst(player.position().add(0.0, 1.0, 0.0))
                            .play();
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("shake").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vfx.shake(player.serverLevel(), player.position(), 3.0f, 12);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("custom").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vfx.emitter()
                            .shape(ShapeSpec.cone(1.0f, 3.0f))
                            .count(300)
                            .speed(0.12f)
                            .size(0.4f, 0.0f, Easings.EASE_OUT_QUAD)
                            .color(0x66CCFF, 0x0033FF, Easings.LINEAR)
                            .play(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
