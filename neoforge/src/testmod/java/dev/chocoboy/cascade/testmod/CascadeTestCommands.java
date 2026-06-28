package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.SpriteId;
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
                    Vfx.emitter()
                            .shape(ShapeSpec.disc(0.6f))
                            .count(160)
                            .lifetime(40)
                            .speed(0.0f)
                            .size(0.1f, 0.04f, Easings.LINEAR)
                            .alpha(1.0f, 0.0f, Easings.LINEAR)
                            .color(0x66FFCC, 0x3366FF, Easings.LINEAR)
                            .attractor(0.0f, 2.5f, 0.0f, 0.02f)
                            .curl(0.01f, 0.5f)
                            .sprite(SpriteId.SPARK)
                            .stretch(2.0f)
                            .play(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
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
                    Vfx.emitter()
                            .shape(ShapeSpec.hemisphere(0.2f))
                            .count(100)
                            .lifetime(80)
                            .speed(0.32f)
                            .size(0.1f, 0.07f, Easings.LINEAR)
                            .alpha(1.0f, 0.0f, Easings.LINEAR)
                            .color(0xFFCC66, 0x885522, Easings.LINEAR)
                            .gravity(0.0f, -0.03f, 0.0f)
                            .collide(0.5f, 0.2f)
                            .sprite(SpriteId.SPARK)
                            .spin(0.05f)
                            .play(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
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
                            .shape(ShapeSpec.hemisphere(0.3f))
                            .lifetime(50)
                            .speed(0.03f)
                            .size(0.25f, 0.7f, Easings.LINEAR)
                            .alpha(0.6f, 0.0f, Easings.LINEAR)
                            .color(0x888888, 0x222222, Easings.LINEAR)
                            .gravity(0.0f, 0.012f, 0.0f)
                            .curl(0.012f, 0.4f)
                            .vortex(0.0f, 0.0f, 0.0f, 0.004f)
                            .rate(6.0f, 60)
                            .sprite(SpriteId.SMOKE)
                            .blend(BlendMode.ALPHA)
                            .spin(0.06f)
                            .animate()
                            .play(player.serverLevel(), player.position().add(0.0, 1.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("custombeam").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vec3 from = player.getEyePosition();
                    Vec3 to = from.add(player.getLookAngle().scale(12.0));
                    Vfx.beam()
                            .color(0xFF3366)
                            .width(0.3f)
                            .arc(0.7f)
                            .duration(20)
                            .play(player.serverLevel(), from, to);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
