package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.BeamState;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

public final class VfxRenderManager {

    private static final VfxRenderManager INSTANCE = new VfxRenderManager();

    private final List<Quad> quads = new ArrayList<>();
    private final List<Burst> bursts = new ArrayList<>();
    private final List<BeamRender> beams = new ArrayList<>();

    private VfxRenderManager() {
    }

    public static VfxRenderManager get() {
        return INSTANCE;
    }

    public void addQuad(Vec3 pos, int rgb, float size, int lifetime) {
        quads.add(new Quad(pos, rgb, size, lifetime));
    }

    public void spawnBurst(Vec3 origin, long seed) {
        ParticleSystem sim = new ParticleSystem(
                Shapes.sphere(1.5f), 120, 30, 0.08f,
                Curve.of(0.25f, 0.0f, Easings.EASE_OUT_QUAD),
                Curve.of(1.0f, 0.0f, Easings.LINEAR),
                ColorCurve.of(0xFFCC33, 0xFF3300, Easings.LINEAR),
                new Random(seed));
        bursts.add(new Burst(origin, sim));
    }

    public void spawnBeam(Vec3 from, Vec3 to, long seed) {
        Vec3f a = new Vec3f((float) from.x, (float) from.y, (float) from.z);
        Vec3f b = new Vec3f((float) to.x, (float) to.y, (float) to.z);
        beams.add(new BeamRender(new BeamState(a, b, 16, 0.35f, 12, new Random(seed))));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        quads.removeIf(q -> ++q.age >= q.lifetime);
        bursts.removeIf(b -> b.sim.tick());
        beams.removeIf(b -> b.sim.tick());
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || (quads.isEmpty() && bursts.isEmpty() && beams.isEmpty())) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Quaternionf rotation = camera.rotation();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(VfxRenderTypes.ADDITIVE);
        for (Quad q : quads) {
            int r = (q.rgb >> 16) & 0xFF;
            int g = (q.rgb >> 8) & 0xFF;
            int b = q.rgb & 0xFF;
            Billboards.quad(pose, vc, rotation,
                    (float) (q.pos.x - cam.x), (float) (q.pos.y - cam.y), (float) (q.pos.z - cam.z),
                    q.size, r, g, b, 255);
        }
        for (Burst burst : bursts) {
            for (Particle p : burst.sim.particles()) {
                int color = burst.sim.colorOf(p);
                int alpha = (int) (burst.sim.alphaOf(p) * 255f);
                Billboards.quad(pose, vc, rotation,
                        (float) (burst.origin.x + p.pos.x() - cam.x),
                        (float) (burst.origin.y + p.pos.y() - cam.y),
                        (float) (burst.origin.z + p.pos.z() - cam.z),
                        burst.sim.sizeOf(p),
                        (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
            }
        }
        float halfWidth = 0.12f;
        for (BeamRender beam : beams) {
            List<Vec3f> spine = beam.sim.spine();
            for (int i = 0; i < spine.size() - 1; i++) {
                Vec3f p0 = spine.get(i);
                Vec3f p1 = spine.get(i + 1);
                Vec3 a = new Vec3(p0.x() - cam.x, p0.y() - cam.y, p0.z() - cam.z);
                Vec3 b = new Vec3(p1.x() - cam.x, p1.y() - cam.y, p1.z() - cam.z);
                Vec3 dir = b.subtract(a).normalize();
                Vec3 toView = a.scale(-1.0).normalize();
                Vec3 side = dir.cross(toView).normalize().scale(halfWidth);
                var m = pose.last().pose();
                vc.addVertex(m, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(120, 200, 255, 255);
                vc.addVertex(m, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(120, 200, 255, 255);
                vc.addVertex(m, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z)).setColor(120, 200, 255, 255);
                vc.addVertex(m, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z)).setColor(120, 200, 255, 255);
            }
        }
        buffers.endBatch(VfxRenderTypes.ADDITIVE);
    }

    private static final class Quad {
        private final Vec3 pos;
        private final int rgb;
        private final float size;
        private final int lifetime;
        private int age;

        private Quad(Vec3 pos, int rgb, float size, int lifetime) {
            this.pos = pos;
            this.rgb = rgb;
            this.size = size;
            this.lifetime = lifetime;
        }
    }

    private static final class Burst {
        private final Vec3 origin;
        private final ParticleSystem sim;

        private Burst(Vec3 origin, ParticleSystem sim) {
            this.origin = origin;
            this.sim = sim;
        }
    }

    private static final class BeamRender {
        private final BeamState sim;

        private BeamRender(BeamState sim) {
            this.sim = sim;
        }
    }
}
