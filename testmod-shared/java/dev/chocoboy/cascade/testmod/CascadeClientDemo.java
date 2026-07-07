package dev.chocoboy.cascade.testmod;

import dev.chocoboy.cascade.Vfx;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

// the render-state demos, split out because they touch client-only classes. CascadeTestCommands calls in here
// only behind a dist check, so a dedicated server never loads this class or the render types it names. the
// command runs on the integrated server thread, so each demo marshals onto the client thread before touching
// render state
final class CascadeClientDemo {

    private CascadeClientDemo() {
    }

    static void bloom() {
        Minecraft.getInstance().execute(() -> {
            boolean on = !PostFx.enabled();
            PostFx.setEnabled(on);
            say("bloom " + (on ? "on" : "off"));
        });
    }

    static void gpu() {
        Minecraft.getInstance().execute(() -> {
            if (!GpuSim.available()) {
                say("gpu sim unavailable, needs gl 4.3");
                return;
            }
            boolean on = !GpuSim.enabled();
            GpuSim.setEnabled(on);
            say("gpu sim " + (on ? "on" : "off"));
        });
    }

    // hud particles are client render state. sizes and speeds are gui units; a sphere shell projects as a
    // round ring of sparks where a ring shape would flatten to a line on the hud
    static void screen() {
        Minecraft.getInstance().execute(() -> ScreenVfx.playCentered(Vfx.emitter()
                .shape(ShapeSpec.sphere(30f))
                .count(90).lifetime(40).speed(2.5f)
                .size(8f, 0f, Easings.EASE_OUT_QUAD)
                .alpha(1f, 0f, Easings.LINEAR)
                .color(0xFFD75A, 0xFF4422, Easings.LINEAR)
                .gravity(0f, 0.15f, 0f)
                .sprite(SpriteId.SPARK)));
    }

    // a client-local stress field for measuring render throughput: a grid of long lived spark systems around
    // the player, no network involved, so fps under load compares cleanly between builds. alternating blends
    // keep both textured passes busy
    static void stress(int count) {
        Minecraft.getInstance().execute(() -> {
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
            say("spawned " + count + " systems");
        });
    }

    private static void say(String msg) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(msg), false);
        }
    }
}
