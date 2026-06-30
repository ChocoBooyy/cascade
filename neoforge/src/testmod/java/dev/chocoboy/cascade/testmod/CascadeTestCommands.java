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
                }))
                .then(Commands.literal("singularity").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    singularity(src.getLevel(), src.getPosition().add(0.0, 2.2, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("gravitywell").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    gravityWell(src.getLevel(), src.getPosition().add(0.0, 2.5, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("gravitystar").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    gravityStar(src.getLevel(), src.getPosition().add(0.0, 0.6, 0.0));
                    return Command.SINGLE_SUCCESS;
                })));
    }

    // Gravity Star: a kunai impact spawns a star of gravity that drags matter into a flat ground spiral,
    // then collapses into a blast built to read as knockback, a low wide shockwave that throws outward
    // rather than up. Sits near the ground since it is an impact, not an airburst.
    private static final int GS_GOLD = 0xFFD84D;
    private static final int GS_WHITE = 0xFFFFFF;
    private static final int GS_INDIGO = 0x33246B;
    private static final int GS_PALE = 0xBFD4FF;

    private static void gravityStar(ServerLevel level, Vec3 c) {
        Vfx.at(level)
                .parallel(
                        // flat spiral infall hugging the ground (disc shape + orbital + attractor)
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.disc(5.0f))
                                .lifetime(58).speed(0.30f)
                                .orbit()
                                .attractor(0.0f, 0.0f, 0.0f, 0.07f)
                                .size(0.16f, 0.04f, Easings.EASE_IN_QUAD)
                                .alpha(0.9f, 0.2f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, GS_PALE, GS_GOLD, GS_INDIGO)
                                .rate(12.0f, 66)
                                .sprite(SpriteId.SHARD).stretch(2.2f).spin(0.1f).trail(6)),
                        // the star itself, a growing spinning core
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.point())
                                .lifetime(24).speed(0.0f)
                                .size(0.4f, 0.9f, Easings.EASE_OUT_QUAD)
                                .alpha(1.0f, 0.0f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, GS_WHITE, GS_GOLD)
                                .rate(2.5f, 66)
                                .sprite(SpriteId.STAR).spin(0.04f)),
                        s -> s.light(c, GS_GOLD, 4.5f, 74))
                .delay(68)
                // collapse to a point, gold winks white
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.sphere(3.0f))
                        .count(120).lifetime(9).speed(0.55f)
                        .implode()
                        .size(0.16f, 0.0f, Easings.EASE_IN_QUAD)
                        .alpha(1.0f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, GS_GOLD, GS_WHITE)
                        .sprite(SpriteId.SPARK).stretch(2.5f).trail(4))
                .delay(10)
                .run(() -> gravityStarBlast(level, c))
                .delay(3)
                // aftermath: low dust drifts outward and settles
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.disc(2.5f))
                        .count(120).lifetime(70).speed(0.05f)
                        .size(0.16f, 0.0f, Easings.LINEAR)
                        .alpha(0.6f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, GS_PALE, GS_INDIGO)
                        .curl(0.02f, 0.5f)
                        .sprite(SpriteId.SMOKE).blend(BlendMode.ALPHA).lit())
                .play();
    }

    // the knockback blast: a flat outward sheet plus two ground shockwave rings dominate, so the energy
    // reads as a horizontal shove. A star flash, ground-hugging bolts, a wide flash and a hard kick finish.
    private static void gravityStarBlast(ServerLevel level, Vec3 c) {
        Vfx.emitter()
                .shape(ShapeSpec.disc(0.5f))
                .count(220).lifetime(26).speed(0.8f)
                .size(0.22f, 0.0f, Easings.EASE_OUT_QUAD)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, GS_WHITE, GS_GOLD, GS_INDIGO)
                .drag(0.06f)
                .sprite(SpriteId.STAR).stretch(3.5f).trail(6)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.ring(0.6f))
                .count(110).lifetime(20).speed(0.9f)
                .size(0.32f, 0.0f, Easings.LINEAR)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, GS_PALE, GS_WHITE)
                .sprite(SpriteId.RING).stretch(1.6f)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.ring(1.2f))
                .count(90).lifetime(28).speed(0.55f)
                .size(0.4f, 0.0f, Easings.LINEAR)
                .alpha(0.8f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, GS_GOLD, GS_INDIGO)
                .sprite(SpriteId.RING).stretch(1.4f)
                .play(level, c);
        int beams = 12;
        for (int i = 0; i < beams; i++) {
            double angle = Math.PI * 2.0 * i / beams;
            Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vfx.beam().color(GS_GOLD).width(0.3f).arc(0.4f).duration(12)
                    .play(level, c, c.add(dir.scale(9.0)));
        }
        Vfx.light(level, c, GS_WHITE, 9.0f, 26);
        Vfx.shake(level, c, 3.4f, 20);
    }

    // a gravity well: a point pulls ambient matter into a tightening spiral for a few seconds, then the
    // well collapses and detonates. Infall is orbital initial velocity plus a center attractor, so paths
    // decay inward instead of falling straight in.
    private static void gravityWell(ServerLevel level, Vec3 c) {
        Vfx.at(level)
                .parallel(
                        // matter spirals in from a wide shell over the whole charge
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.sphere(5.0f))
                                .lifetime(64).speed(0.30f)
                                .orbit()
                                .attractor(0.0f, 0.0f, 0.0f, 0.06f)
                                .size(0.15f, 0.04f, Easings.EASE_IN_QUAD)
                                .alpha(0.85f, 0.2f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, CYAN, VIOLET, DEEP_VIOLET)
                                .rate(11.0f, 72)
                                .sprite(SpriteId.SHARD).stretch(2.0f).spin(0.12f).trail(6)),
                        // a hot core glows brighter at the center as mass piles up
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.sphere(0.3f))
                                .lifetime(26).speed(0.0f)
                                .size(0.25f, 0.6f, Easings.EASE_OUT_QUAD)
                                .alpha(0.9f, 0.0f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, WHITE, VIOLET)
                                .rate(3.0f, 72)
                                .sprite(SpriteId.STAR)),
                        s -> s.light(c, VIOLET, 4.0f, 80))
                .delay(74)
                // collapse: the well yanks the last matter to a point and winks white
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.sphere(3.0f))
                        .count(130).lifetime(9).speed(0.55f)
                        .implode()
                        .size(0.16f, 0.0f, Easings.EASE_IN_QUAD)
                        .alpha(1.0f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, VIOLET, WHITE)
                        .sprite(SpriteId.SPARK).stretch(2.5f).trail(4))
                .delay(10)
                .run(() -> gravityBlast(level, c))
                .delay(3)
                // aftermath: blown out debris coasts and lit smoke settles
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.sphere(2.0f))
                        .count(120).lifetime(75).speed(0.06f)
                        .size(0.14f, 0.0f, Easings.LINEAR)
                        .alpha(0.7f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, VIOLET, DEEP_VIOLET)
                        .curl(0.025f, 0.5f)
                        .sprite(SpriteId.SMOKE).blend(BlendMode.ALPHA).lit())
                .play();
    }

    // the collapse blast: a star core, a fast double shockwave, twelve long radial bolts, a wide light
    // flash, and a hard screen kick. Bigger than the singularity's detonation on purpose.
    private static void gravityBlast(ServerLevel level, Vec3 c) {
        Vfx.emitter()
                .shape(ShapeSpec.sphere(0.4f))
                .count(220).lifetime(34).speed(0.55f)
                .size(0.22f, 0.02f, Easings.EASE_OUT_QUAD)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, WHITE, CYAN, VIOLET)
                .drag(0.05f)
                .sprite(SpriteId.STAR).stretch(3.5f).trail(6)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.ring(0.6f))
                .count(90).lifetime(20).speed(0.7f)
                .size(0.3f, 0.0f, Easings.LINEAR)
                .alpha(0.95f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, CYAN, WHITE)
                .sprite(SpriteId.RING).stretch(1.5f)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.sphere(1.0f))
                .count(120).lifetime(40).speed(0.32f)
                .size(0.2f, 0.0f, Easings.LINEAR)
                .alpha(0.8f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, VIOLET, DEEP_VIOLET)
                .drag(0.06f)
                .sprite(SpriteId.SPARK).stretch(2.5f).trail(5)
                .play(level, c);
        int beams = 12;
        for (int i = 0; i < beams; i++) {
            double angle = Math.PI * 2.0 * i / beams;
            Vec3 dir = new Vec3(Math.cos(angle), 0.12, Math.sin(angle));
            Vfx.beam().color(CYAN).width(0.28f).arc(0.6f).duration(12)
                    .play(level, c, c.add(dir.scale(9.0)));
        }
        Vfx.light(level, c, WHITE, 8.0f, 28);
        Vfx.shake(level, c, 3.0f, 18);
    }

    // the showcase: a collapsing singularity that implodes matter into an orbiting disc, then detonates.
    // every stage is one phase on the timeline, with parallel branches where light and particles overlap.
    private static final int CYAN = 0x33EEFF;
    private static final int VIOLET = 0x8833FF;
    private static final int DEEP_VIOLET = 0x3A1060;
    private static final int WHITE = 0xFFFFFF;

    private static void singularity(ServerLevel level, Vec3 c) {
        Vfx.at(level)
                // implosion: a shell of embers rushes inward while a faint pool of light gathers below
                .parallel(
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.sphere(4.0f))
                                .count(150).lifetime(34).speed(0.16f)
                                .implode()
                                .size(0.18f, 0.04f, Easings.EASE_IN_QUAD)
                                .alpha(0.9f, 0.4f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, CYAN, VIOLET)
                                .sprite(SpriteId.SPARK).stretch(2.5f).trail(5)),
                        s -> s.light(c, DEEP_VIOLET, 3.0f, 44))
                .delay(26)
                // accretion: a flat disc spins up around the core as the light deepens
                .parallel(
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.disc(2.6f))
                                .lifetime(40).speed(0.22f)
                                .orbit()
                                .size(0.16f, 0.05f, Easings.LINEAR)
                                .alpha(0.85f, 0.0f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, CYAN, VIOLET, DEEP_VIOLET)
                                .rate(8.0f, 34)
                                .sprite(SpriteId.SHARD).stretch(0.6f).spin(0.15f).trail(6)),
                        s -> s.light(c, VIOLET, 3.6f, 46))
                .delay(40)
                // collapse: the last matter is sucked to a point and winks white
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.sphere(2.2f))
                        .count(80).lifetime(12).speed(0.34f)
                        .implode()
                        .size(0.14f, 0.0f, Easings.EASE_IN_QUAD)
                        .alpha(1.0f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, VIOLET, WHITE)
                        .sprite(SpriteId.SPARK).stretch(2.0f))
                // a breath of stillness before it goes off
                .delay(14)
                .run(() -> detonate(level, c))
                // aftermath: debris drifts back out on curl noise and lit smoke settles
                .delay(3)
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.sphere(1.5f))
                        .count(90).lifetime(70).speed(0.05f)
                        .size(0.12f, 0.0f, Easings.LINEAR)
                        .alpha(0.7f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, VIOLET, DEEP_VIOLET)
                        .curl(0.02f, 0.5f)
                        .sprite(SpriteId.SMOKE).blend(BlendMode.ALPHA).lit())
                .play();
    }

    // the detonation beat: a white core burst, an expanding shockwave ring, eight radial bolts, a bright
    // flash of light, and the screen kick, all fired on the same tick
    private static void detonate(ServerLevel level, Vec3 c) {
        Vfx.emitter()
                .shape(ShapeSpec.sphere(0.4f))
                .count(160).lifetime(30).speed(0.4f)
                .size(0.2f, 0.02f, Easings.EASE_OUT_QUAD)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, WHITE, CYAN, VIOLET)
                .drag(0.04f)
                .sprite(SpriteId.STAR).stretch(3.5f).trail(6)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.ring(0.6f))
                .count(64).lifetime(18).speed(0.5f)
                .size(0.28f, 0.0f, Easings.LINEAR)
                .alpha(0.9f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, CYAN, WHITE)
                .sprite(SpriteId.RING).stretch(1.5f)
                .play(level, c);
        int beams = 8;
        for (int i = 0; i < beams; i++) {
            double angle = Math.PI * 2.0 * i / beams;
            Vec3 dir = new Vec3(Math.cos(angle), 0.15, Math.sin(angle));
            Vfx.beam().color(CYAN).width(0.25f).arc(0.5f).duration(10)
                    .play(level, c, c.add(dir.scale(7.0)));
        }
        Vfx.light(level, c, WHITE, 6.0f, 24);
        Vfx.shake(level, c, 2.4f, 16);
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
