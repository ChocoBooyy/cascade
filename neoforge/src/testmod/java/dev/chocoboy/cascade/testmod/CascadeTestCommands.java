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
                            .shape(ShapeSpec.hemisphere(0.15f))
                            .count(8)
                            .lifetime(25)
                            .speed(0.45f)
                            .size(0.12f, 0.12f, Easings.LINEAR)
                            .alpha(1.0f, 1.0f, Easings.LINEAR)
                            .color(0xFFFFFF, 0xFFEE88, Easings.LINEAR)
                            .gravity(0.0f, -0.02f, 0.0f)
                            .sprite(SpriteId.SPARK)
                            .stretch(2.0f)
                            .burstOnDeath(Vfx.emitter()
                                    .shape(ShapeSpec.sphere(0.1f))
                                    .count(60)
                                    .lifetime(30)
                                    .speed(0.25f)
                                    .size(0.12f, 0.03f, Easings.LINEAR)
                                    .alpha(1.0f, 0.0f, Easings.LINEAR)
                                    .color(0xFF66AA, 0x3366FF, Easings.LINEAR)
                                    .gravity(0.0f, -0.02f, 0.0f)
                                    .drag(0.05f)
                                    .sprite(SpriteId.SPARK)
                                    .stretch(1.5f))
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
                            .shape(ShapeSpec.sphere(0.1f))
                            .count(12)
                            .lifetime(50)
                            .speed(0.3f)
                            .size(0.18f, 0.06f, Easings.LINEAR)
                            .alpha(1.0f, 0.0f, Easings.LINEAR)
                            .color(0x66DDFF, 0x2244FF, Easings.LINEAR)
                            .gravity(0.0f, -0.01f, 0.0f)
                            .sprite(SpriteId.GLOW)
                            .trail(10)
                            .play(player.serverLevel(), player.position().add(0.0, 1.2, 0.0));
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
