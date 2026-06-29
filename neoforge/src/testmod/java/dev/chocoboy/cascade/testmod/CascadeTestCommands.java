package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import dev.chocoboy.cascade.neoforge.Vfx;
import dev.chocoboy.cascade.neoforge.VfxEmitter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
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
                    CommandSourceStack src = ctx.getSource();
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
                            .play(src.getLevel(), src.getPosition().add(0.0, 1.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("beam").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    Vec3 from = src.getPosition();
                    Vec3 to = from.add(lookVector(src).scale(10.0));
                    Vfx.beam(src.getLevel(), from, to);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("combo").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    Vec3 base = src.getPosition().add(0.0, 1.0, 0.0);
                    // left plume is lit, so it sits in world light; right plume is the full-bright twin
                    smokePlume(src.getLevel(), base.add(-1.0, 0.0, 0.0), true);
                    smokePlume(src.getLevel(), base.add(1.0, 0.0, 0.0), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("shake").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    Vfx.shake(src.getLevel(), src.getPosition(), 3.0f, 12);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("custom").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    Vfx.play(src.getLevel(), src.getPosition().add(0.0, 1.0, 0.0),
                            ResourceLocation.fromNamespaceAndPath("cascade", "firework"));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("custombeam").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    Vec3 from = src.getPosition();
                    Vec3 to = from.add(lookVector(src).scale(12.0));
                    Vfx.beam()
                            .color(0xFF3366)
                            .width(0.3f)
                            .arc(0.7f)
                            .duration(20)
                            .play(src.getLevel(), from, to);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    // the command source's facing as a unit vector. A command block defaults to (0,0), which points
    // due south, so beams still fire from blocks even with no aim
    private static Vec3 lookVector(CommandSourceStack src) {
        Vec2 rot = src.getRotation();
        double pitch = Math.toRadians(rot.x);
        double yaw = Math.toRadians(rot.y);
        double xz = Math.cos(pitch);
        return new Vec3(-xz * Math.sin(yaw), -Math.sin(pitch), xz * Math.cos(yaw));
    }

    // a rising grey smoke column, optionally tinted by world light so it darkens in shade
    private static void smokePlume(ServerLevel level, Vec3 pos, boolean lit) {
        VfxEmitter plume = Vfx.emitter()
                .shape(ShapeSpec.hemisphere(0.3f))
                .lifetime(60)
                .speed(0.03f)
                .size(0.3f, 0.9f, Easings.LINEAR)
                .alpha(0.7f, 0.0f, Easings.LINEAR)
                .color(0xCCCCCC, 0x555555, Easings.LINEAR)
                .gravity(0.0f, 0.015f, 0.0f)
                .curl(0.012f, 0.4f)
                .rate(5.0f, 80)
                .sprite(SpriteId.SMOKE)
                .blend(BlendMode.ALPHA)
                .spin(0.05f);
        if (lit) {
            plume.lit();
        }
        plume.play(level, pos);
    }
}
