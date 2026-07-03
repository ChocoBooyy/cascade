package dev.chocoboy.cascade.testmod;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.VfxEmitter;
import dev.chocoboy.cascade.VfxSequence;
import dev.chocoboy.cascade.client.GpuSim;
import dev.chocoboy.cascade.client.ParticleBurstEffect;
import dev.chocoboy.cascade.client.PostFx;
import dev.chocoboy.cascade.client.ScreenVfx;
import dev.chocoboy.cascade.client.VfxRenderManager;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CascadeTestCommands {

    private CascadeTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vfxtest")
                .then(Commands.literal("burst").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    burst(src.getLevel(), src.getPosition().add(0.0, 1.0, 0.0));
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
                    combo(src.getLevel(), src.getPosition().add(0.0, 1.0, 0.0));
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
                    customBeam(ctx.getSource());
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
                    Vec3 impact = onGround(src.getLevel(), src.getPosition(), src.getEntity());
                    gravityStar(src.getLevel(), impact.add(0.0, 0.6, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("splash").executes(ctx -> {
                    splash(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("layered").executes(ctx -> {
                    layered(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("debris").executes(ctx -> {
                    debris(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("blockdebris").executes(ctx -> {
                    blockDebris(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("itemdebris").executes(ctx -> {
                    itemDebris(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("softsmoke").executes(ctx -> {
                    softSmoke(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("component").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    component(src.getLevel(), src.getPosition().add(0.0, 1.5, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("boids").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    boids(src.getLevel(), src.getPosition().add(0.0, 2.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("storm").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    storm(src.getLevel(), src.getPosition().add(0.0, 3.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("sdf").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    sdfCluster(src.getLevel(), src.getPosition().add(0.0, 2.0, 0.0));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("gallery").executes(ctx -> gallery(ctx.getSource()))));
    }

    // client side, since bloom is client render state; toggling on the server would only work in singleplayer
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cascadebloom").executes(ctx -> {
            boolean on = !PostFx.enabled();
            PostFx.setEnabled(on);
            ctx.getSource().sendSuccess(() -> Component.literal("bloom " + (on ? "on" : "off")), false);
            return Command.SINGLE_SUCCESS;
        }));
        // hud particles are pure client render state too, so this stays a client command. sizes and speeds
        // are gui units. a ring shape samples the xz plane and would flatten to a line on the hud, so the
        // burst spawns on a sphere shell, which projects as a round ring of sparks
        event.getDispatcher().register(Commands.literal("cascadescreen").executes(ctx -> {
            ScreenVfx.playCentered(Vfx.emitter()
                    .shape(ShapeSpec.sphere(30f))
                    .count(90).lifetime(40).speed(2.5f)
                    .size(8f, 0f, Easings.EASE_OUT_QUAD)
                    .alpha(1f, 0f, Easings.LINEAR)
                    .color(0xFFD75A, 0xFF4422, Easings.LINEAR)
                    .gravity(0f, 0.15f, 0f)
                    .sprite(SpriteId.SPARK));
            return Command.SINGLE_SUCCESS;
        }));
        // gpu backend toggle; render state is client-side, so this stays a client command
        event.getDispatcher().register(Commands.literal("cascadegpu").executes(ctx -> {
            String status;
            if (!GpuSim.available()) {
                status = "gpu sim unavailable, needs gl 4.3";
            } else {
                boolean on = !GpuSim.enabled();
                GpuSim.setEnabled(on);
                status = "gpu sim " + (on ? "on" : "off");
            }
            ctx.getSource().sendSuccess(() -> Component.literal(status), false);
            return Command.SINGLE_SUCCESS;
        }));
        // a client-local stress field for measuring render throughput: a grid of long lived spark systems
        // around the player, no network involved, so fps under load compares cleanly between builds
        event.getDispatcher().register(Commands.literal("cascadestress")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 256)).executes(ctx -> {
                    int count = IntegerArgumentType.getInteger(ctx, "count");
                    spawnStressField(count);
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("spawned " + count + " systems"), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    // the same path the network handler takes, minus the wire: build each system straight from the spec
    // and hand it to the render manager. alternating blends keep both textured passes under load
    private static void spawnStressField(int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        EmitterSpec additive = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.4f))
                .count(300).lifetime(200).speed(0.12f)
                .size(0.1f, 0.02f, Easings.LINEAR)
                .alpha(1f, 0f, Easings.LINEAR)
                .color(0x66CCFF, 0xFF44AA, Easings.LINEAR)
                .gravity(0f, -0.004f, 0f)
                .drag(0.02f)
                .sprite(SpriteId.SPARK)
                .spec();
        EmitterSpec alpha = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.4f))
                .count(300).lifetime(200).speed(0.1f)
                .size(0.25f, 0.05f, Easings.LINEAR)
                .alpha(0.8f, 0f, Easings.LINEAR)
                .color(0xDDDDDD, 0x555555, Easings.LINEAR)
                .gravity(0f, 0.003f, 0f)
                .sprite(SpriteId.SMOKE)
                .blend(BlendMode.ALPHA)
                .spec();
        Vec3 base = mc.player.position();
        int side = (int) Math.ceil(Math.sqrt(count));
        for (int i = 0; i < count; i++) {
            double x = (i % side - side / 2.0) * 5.0;
            double z = (i / side - side / 2.0) * 5.0;
            Vec3 origin = base.add(x, 2.0, z);
            EmitterSpec spec = i % 2 == 0 ? additive : alpha;
            VfxRenderManager.get().spawn(new ParticleBurstEffect(
                    origin, spec.build(new Random(i)), spec.render(), spec.subEmitter(), 0));
        }
    }

    // two low billowing smoke clouds that sit on the ground, four blocks apart, snapped to the surface. the
    // left is soft and should melt into the floor where the billboards cut it; the right is the hard
    // reference and shows sharp slice lines along the ground. that contact line is the whole demo
    private static void softSmoke(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 ground = onGround(level, src.getPosition(), src.getEntity());
        fog(level, ground.add(-2.0, 0.0, 0.0), true);
        fog(level, ground.add(2.0, 0.0, 0.0), false);
    }

    private static void fog(ServerLevel level, Vec3 pos, boolean soft) {
        // dark, dense, and kept low so the whole cloud sits near the ground: with the wide fade the soft
        // one reads as thin and wispy all over, the hard one as a solid dark mass. high contrast on purpose
        VfxEmitter f = Vfx.emitter()
                .shape(ShapeSpec.disc(1.0f))
                .lifetime(60)
                .speed(0.04f)
                .size(0.9f, 1.6f, Easings.LINEAR)
                .alpha(0.85f, 0.0f, Easings.LINEAR)
                .color(0x333333, 0x000000, Easings.LINEAR)
                .gravity(0.0f, 0.006f, 0.0f)
                .curl(0.01f, 0.4f)
                .rate(7.0f, 120)
                .sprite(SpriteId.SMOKE)
                .blend(BlendMode.ALPHA)
                .lit();
        if (soft) {
            f.soft();
        }
        // lift the base a touch so the big billboards straddle the surface instead of spawning fully buried
        f.play(level, pos.add(0.0, 0.4, 0.0));
    }

    // a fast outward burst caught on an invisible sphere by a third-party contain component, proving custom
    // components register and run without any engine change. particles rush out then pile up on the shell
    private static void component(ServerLevel level, Vec3 pos) {
        Vfx.emitter()
                .shape(ShapeSpec.sphere(0.2f))
                .count(120).lifetime(50).speed(0.3f)
                .size(0.14f, 0.06f, Easings.LINEAR)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .color(0x66FF88, 0x1188AA, Easings.LINEAR)
                .component(new ContainSpec(2.5f))
                .sprite(SpriteId.SPARK).stretch(1.5f).trail(4)
                .play(level, pos);
    }

    // the gpu showcase: a hundred thousand sparks in one burst, swirled by a vortex against a weak inward
    // pull so the cloud churns in place for its whole life. the spec is gpu-eligible on purpose; with the
    // backend off it falls back to the cpu sim, which clips at its 4000 cap, so the toggle is its own A/B
    private static void storm(ServerLevel level, Vec3 pos) {
        Vfx.emitter()
                .shape(ShapeSpec.sphere(2.0f))
                .count(100000).lifetime(300).speed(0.05f)
                .size(0.06f, 0.02f, Easings.LINEAR)
                .alpha(1.0f, 0.0f, Easings.EASE_IN_QUAD)
                .gradient(Easings.LINEAR, 0xFFE9A8, 0xFF9C33, 0xB03A9C, 0x3A2C86)
                .vortex(0.0f, 0.0f, 0.0f, 0.012f)
                .attractor(0.0f, 0.0f, 0.0f, 0.003f)
                .drag(0.01f)
                .spin(0.25f)
                .sprite(SpriteId.SPARK)
                .play(level, pos);
    }

    // the sdf showcase: a rotating metaball cluster whose shapes fuse where they near each other, a look
    // no billboard sprite can fake. the rim gradient paints the silhouette
    private static void sdfCluster(ServerLevel level, Vec3 pos) {
        Vfx.sdf()
                .sphere(0.6f, 0.0f, 0.0f, 0.45f)
                .sphere(-0.5f, 0.3f, 0.2f, 0.35f)
                .sphere(0.0f, -0.4f, -0.3f, 0.3f)
                .torus(0.0f, 0.0f, 0.0f, 0.9f, 0.12f)
                .smoothness(0.5f)
                .gradient(Easings.LINEAR, 0x2430A8, 0xB03AF0, 0x22E5FF)
                .duration(400)
                .rotate(0.02f)
                .play(level, pos);
    }

    // a swarm that flocks: each mote steers off its neighbors (separation, alignment, cohesion) while a weak
    // pull to the center keeps the murmuration bounded so it swirls in place instead of drifting off
    private static void boids(ServerLevel level, Vec3 pos) {
        Vfx.emitter()
                .shape(ShapeSpec.sphere(2.5f))
                .count(80).lifetime(400).speed(0.06f)
                .size(0.12f, 0.12f, Easings.LINEAR)
                .alpha(1.0f, 1.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, 0x33EEFF, 0x2277FF, 0x1133AA)
                .flock(3.0f, 0.02f, 0.015f, 0.012f, 0.14f)
                .attractor(0.0f, 0.0f, 0.0f, 0.004f)
                .sprite(SpriteId.GLOW).stretch(1.5f).trail(5)
                .play(level, pos);
    }

    // Gravity Star: a kunai impact opens a purple gravity zone that draws matter straight inward, a steady
    // field rather than a whirlpool, then collapses into a knockback blast, a low wide shockwave that throws
    // outward rather than up. Sits on the ground since it is an impact, not an airburst.
    private static final int GZ_BRIGHT = 0xB36BFF;
    private static final int GZ_VIOLET = 0x7A2CE0;
    private static final int GZ_DEEP = 0x2A1060;
    private static final int GZ_LAVENDER = 0xD9B8FF;

    private static void gravityStar(ServerLevel level, Vec3 c) {
        Vfx.at(level)
                .parallel(
                        // the zone itself: a real translucent dome mesh, not a cloud of particles
                        s -> s.dome(c, 4.5f, GZ_BRIGHT, 80),
                        // a few motes pulled inward off the shell, so the dome reads as gravity, not decor
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.hemisphere(4.5f))
                                .lifetime(40).speed(0.12f)
                                .implode()
                                .size(0.14f, 0.04f, Easings.EASE_IN_QUAD)
                                .alpha(0.6f, 0.0f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, GZ_LAVENDER, GZ_DEEP)
                                .rate(9.0f, 74)
                                .sprite(SpriteId.GLOW).stretch(0.8f).trail(4)),
                        // a soft core orb swells at the center as the field tightens
                        s -> s.emit(c, Vfx.emitter()
                                .shape(ShapeSpec.sphere(0.3f))
                                .lifetime(28).speed(0.0f)
                                .size(0.5f, 1.0f, Easings.EASE_OUT_QUAD)
                                .alpha(0.9f, 0.0f, Easings.LINEAR)
                                .gradient(Easings.LINEAR, GZ_BRIGHT, GZ_VIOLET)
                                .rate(2.5f, 74)
                                .sprite(SpriteId.GLOW)),
                        s -> s.light(c, GZ_VIOLET, 5.0f, 82))
                .delay(76)
                // collapse to a point, the field winks white
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.sphere(3.0f))
                        .count(120).lifetime(9).speed(0.55f)
                        .implode()
                        .size(0.16f, 0.0f, Easings.EASE_IN_QUAD)
                        .alpha(1.0f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, GZ_VIOLET, WHITE)
                        .sprite(SpriteId.SPARK).stretch(2.0f).trail(4))
                .delay(10)
                .run(() -> gravityStarBlast(level, c))
                .delay(3)
                // aftermath: low dust drifts outward and settles
                .emit(c, Vfx.emitter()
                        .shape(ShapeSpec.disc(2.5f))
                        .count(120).lifetime(70).speed(0.05f)
                        .size(0.16f, 0.0f, Easings.LINEAR)
                        .alpha(0.6f, 0.0f, Easings.LINEAR)
                        .gradient(Easings.LINEAR, GZ_LAVENDER, GZ_DEEP)
                        .curl(0.02f, 0.5f)
                        .sprite(SpriteId.SMOKE).blend(BlendMode.ALPHA).lit())
                .play();
    }

    // the knockback blast: a flat outward sheet plus two ground shockwave rings dominate, so the energy
    // reads as a horizontal shove. A bright flash, ground-hugging bolts, a wide flash and a hard kick finish.
    private static void gravityStarBlast(ServerLevel level, Vec3 c) {
        Vfx.emitter()
                .shape(ShapeSpec.disc(0.5f))
                .count(200).lifetime(26).speed(0.8f)
                .size(0.24f, 0.0f, Easings.EASE_OUT_QUAD)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, WHITE, GZ_BRIGHT, GZ_DEEP)
                .drag(0.06f)
                .sprite(SpriteId.GLOW).stretch(3.0f).trail(5)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.ring(0.6f))
                .count(110).lifetime(20).speed(0.9f)
                .size(0.32f, 0.0f, Easings.LINEAR)
                .alpha(1.0f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, GZ_LAVENDER, WHITE)
                .sprite(SpriteId.RING).stretch(1.6f)
                .play(level, c);
        Vfx.emitter()
                .shape(ShapeSpec.ring(1.2f))
                .count(90).lifetime(28).speed(0.55f)
                .size(0.4f, 0.0f, Easings.LINEAR)
                .alpha(0.8f, 0.0f, Easings.LINEAR)
                .gradient(Easings.LINEAR, GZ_BRIGHT, GZ_DEEP)
                .sprite(SpriteId.RING).stretch(1.4f)
                .play(level, c);
        int beams = 12;
        for (int i = 0; i < beams; i++) {
            double angle = Math.PI * 2.0 * i / beams;
            Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vfx.beam().color(GZ_BRIGHT).width(0.3f).arc(0.4f).duration(12)
                    .play(level, c, c.add(dir.scale(9.0)));
        }
        Vfx.light(level, c, GZ_LAVENDER, 9.0f, 26);
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

    // fountain whose droplets pop into a spark on first block contact, proving collision-triggered sub-emitters
    private static void splash(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 c = src.getPosition();
        VfxEmitter spark = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.2f))
                .count(10).lifetime(22).speed(0.1f)
                .size(0.22f, 0f, Easings.EASE_OUT_QUAD)
                .alpha(1f, 0f, Easings.LINEAR)
                .color(0x88E0FF, 0x1144AA, Easings.LINEAR);
        // sprinkle over time so impacts stagger across a few seconds, otherwise the whole burst lands at once
        Vfx.emitter()
                .shape(ShapeSpec.cone(0.3f, 1.0f))
                .rate(2f, 70).lifetime(50).speed(0.4f)
                .size(0.16f, 0.05f, Easings.LINEAR)
                .alpha(1f, 0f, Easings.LINEAR)
                .color(0x66CCFF, 0x2266CC, Easings.LINEAR)
                .gravity(0f, -0.02f, 0f)
                .collide(0f, 1f)
                .burstOnCollision(spark)
                .play(level, c);
    }

    // three emitters at one origin: a core flash, a lit smoke puff, and a spark spray, proving layered effects
    private static void layered(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 c = src.getPosition();
        VfxEmitter core = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(40).lifetime(12).speed(0.05f)
                .size(0.5f, 0f, Easings.EASE_OUT_QUAD)
                .alpha(1f, 0f, Easings.LINEAR)
                .color(0xFFFFFF, 0xFFE08A, Easings.LINEAR);
        VfxEmitter smoke = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.6f))
                .count(30).lifetime(50).speed(0.03f)
                .size(0.6f, 1.4f, Easings.LINEAR)
                .alpha(0.6f, 0f, Easings.LINEAR)
                .color(0x555555, 0x222222, Easings.LINEAR)
                .sprite(SpriteId.SMOKE).blend(BlendMode.ALPHA).lit();
        VfxEmitter sparks = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.2f))
                .count(60).lifetime(30).speed(0.4f)
                .size(0.14f, 0f, Easings.EASE_OUT_QUAD)
                .alpha(1f, 0f, Easings.LINEAR)
                .color(0xFFDD55, 0xFF6622, Easings.LINEAR)
                .sprite(SpriteId.SPARK).gravity(0f, -0.02f, 0f).trail(5);
        Vfx.effect().add(core).add(smoke).add(sparks).play(level, c);
    }

    // a burst of solid tumbling cube and shard debris that falls and bounces, proving mesh particles
    private static void debris(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 c = src.getPosition();
        VfxEmitter chunks = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(50).lifetime(70).speed(0.4f)
                .size(0.12f, 0.12f, Easings.LINEAR)
                .alpha(1f, 1f, Easings.LINEAR)
                .color(0x9A7B5A, 0x6E5238, Easings.LINEAR)
                .spin(0.5f).cube().lit()
                .gravity(0f, -0.03f, 0f).collide(0.3f, 0.4f);
        VfxEmitter shards = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(24).lifetime(60).speed(0.5f)
                .size(0.1f, 0.1f, Easings.LINEAR)
                .alpha(1f, 1f, Easings.LINEAR)
                .color(0xC8C8D0, 0x8A8A95, Easings.LINEAR)
                .spin(0.7f).shard().lit()
                .gravity(0f, -0.03f, 0f).collide(0.2f, 0.5f);
        Vfx.effect().add(chunks).add(shards).play(level, c);
    }

    // a burst of real block model chunks (stone and dirt) that tumble, fall, and bounce, block break debris
    private static void blockDebris(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 c = src.getPosition();
        VfxEmitter stone = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(40).lifetime(70).speed(0.4f)
                .size(0.14f, 0.14f, Easings.LINEAR)
                .alpha(1f, 1f, Easings.LINEAR)
                .color(0xFFFFFF, 0xFFFFFF, Easings.LINEAR)
                .spin(0.5f).block(Blocks.STONE).lit()
                .gravity(0f, -0.03f, 0f).collide(0.3f, 0.4f);
        VfxEmitter dirt = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(30).lifetime(60).speed(0.45f)
                .size(0.13f, 0.13f, Easings.LINEAR)
                .alpha(1f, 1f, Easings.LINEAR)
                .color(0xFFFFFF, 0xFFFFFF, Easings.LINEAR)
                .spin(0.6f).block(Blocks.DIRT).lit()
                .gravity(0f, -0.03f, 0f).collide(0.2f, 0.5f);
        Vfx.effect().add(stone).add(dirt).play(level, c);
    }

    // a burst of real item model debris (nuggets and sticks) that tumble, fall, and bounce
    private static void itemDebris(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 c = src.getPosition();
        VfxEmitter nuggets = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(36).lifetime(70).speed(0.4f)
                .size(0.12f, 0.12f, Easings.LINEAR)
                .alpha(1f, 1f, Easings.LINEAR)
                .color(0xFFFFFF, 0xFFFFFF, Easings.LINEAR)
                .spin(0.6f).item(Items.IRON_NUGGET).lit()
                .gravity(0f, -0.03f, 0f).collide(0.3f, 0.4f);
        VfxEmitter sticks = Vfx.emitter()
                .shape(ShapeSpec.sphere(0.3f))
                .count(20).lifetime(60).speed(0.45f)
                .size(0.13f, 0.13f, Easings.LINEAR)
                .alpha(1f, 1f, Easings.LINEAR)
                .color(0xFFFFFF, 0xFFFFFF, Easings.LINEAR)
                .spin(0.7f).item(Items.STICK).lit()
                .gravity(0f, -0.03f, 0f).collide(0.2f, 0.5f);
        Vfx.effect().add(nuggets).add(sticks).play(level, c);
    }

    // a two-stage spark burst whose motes pop into a second color on death, the sub-emitter basic
    private static void burst(ServerLevel level, Vec3 pos) {
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
                .play(level, pos);
    }

    // left plume is lit, so it sits in world light; right plume is the full-bright twin
    private static void combo(ServerLevel level, Vec3 base) {
        smokePlume(level, base.add(-1.0, 0.0, 0.0), true);
        smokePlume(level, base.add(1.0, 0.0, 0.0), false);
    }

    private static void customBeam(CommandSourceStack src) {
        Vec3 from = src.getPosition();
        Vec3 to = from.add(lookVector(src).scale(12.0));
        Vfx.beam()
                .color(0xFF3366)
                .width(0.3f)
                .arc(0.7f)
                .duration(20)
                .play(src.getLevel(), from, to);
    }

    private static boolean galleryRunning;

    // one command tours every showcase in order, one entry every six seconds, so a single run puts
    // eyes on the whole library. the command source's position is a snapshot, so the whole tour
    // plays where it was started. big set pieces overlap their neighbors a little on purpose; the
    // chat lines keep entries attributable. gravitywell is skipped, its capabilities all appear in
    // singularity
    private static int gallery(CommandSourceStack src) {
        if (galleryRunning) {
            src.sendFailure(Component.literal("gallery already running"));
            return 0;
        }
        galleryRunning = true;
        ServerLevel level = src.getLevel();
        String[] names = {"burst", "firework json", "layered", "splash", "debris", "block debris",
                "item debris", "smoke lit vs unlit", "soft smoke", "custom beam", "component", "boids",
                "sdf", "singularity", "gravity star", "storm"};
        Runnable[] fires = {
                () -> burst(level, src.getPosition().add(0.0, 1.0, 0.0)),
                () -> Vfx.play(level, src.getPosition().add(0.0, 1.0, 0.0),
                        ResourceLocation.fromNamespaceAndPath("cascade", "firework")),
                () -> layered(src),
                () -> splash(src),
                () -> debris(src),
                () -> blockDebris(src),
                () -> itemDebris(src),
                () -> combo(level, src.getPosition().add(0.0, 1.0, 0.0)),
                () -> softSmoke(src),
                () -> customBeam(src),
                () -> component(level, src.getPosition().add(0.0, 1.5, 0.0)),
                () -> boids(level, src.getPosition().add(0.0, 2.0, 0.0)),
                () -> sdfCluster(level, src.getPosition().add(0.0, 2.0, 0.0)),
                () -> singularity(level, src.getPosition().add(0.0, 2.2, 0.0)),
                () -> gravityStar(level,
                        onGround(level, src.getPosition(), src.getEntity()).add(0.0, 0.6, 0.0)),
                () -> storm(level, src.getPosition().add(0.0, 3.0, 0.0)),
        };
        VfxSequence seq = Vfx.at(level);
        for (int i = 0; i < names.length; i++) {
            String label = "cascade gallery " + (i + 1) + "/" + names.length + ": " + names[i];
            Runnable fire = fires[i];
            seq.run(() -> {
                src.sendSuccess(() -> Component.literal(label), false);
                // a throwing entry would take the whole timeline down with it, and the guard reset
                // is the timeline's last step; contain it so the tour and the reset survive
                try {
                    fire.run();
                } catch (RuntimeException e) {
                    src.sendFailure(Component.literal("gallery entry failed: " + label));
                }
            }).delay(120);
        }
        seq.run(() -> galleryRunning = false).play();
        return Command.SINGLE_SUCCESS;
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

    // snap a point down onto the surface beneath it, so an impact effect lands on top of the block instead
    // of inside terrain or floating. Starts a touch high in case the point already sits on the surface, then
    // casts down; over a void it falls back to the original point.
    private static Vec3 onGround(ServerLevel level, Vec3 pos, Entity source) {
        Vec3 start = pos.add(0.0, 1.0, 0.0);
        Vec3 end = pos.subtract(0.0, 32.0, 0.0);
        BlockHitResult hit = level.clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        double y = hit.getType() == HitResult.Type.MISS ? pos.y : hit.getLocation().y;
        return new Vec3(pos.x, y, pos.z);
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
