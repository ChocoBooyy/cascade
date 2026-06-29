package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.CollisionProbe;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import dev.chocoboy.cascade.engine.effect.SubEmitterSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ParticleBurstEffect implements RenderedEffect {

    // hard ceiling on sub-emitter nesting so a self-referencing spec cannot recurse without bound
    private static final int MAX_DEPTH = 4;

    private final Vec3 origin;
    private final ParticleSystem sim;
    private final RenderSpec render;
    private final SubEmitterSpec subEmitter;
    private final int depth;
    private final float[] uv;

    public ParticleBurstEffect(Vec3 origin, ParticleSystem sim, RenderSpec render, SubEmitterSpec subEmitter, int depth) {
        this.origin = origin;
        this.sim = sim;
        this.render = render;
        this.subEmitter = subEmitter;
        this.depth = depth;
        this.uv = ParticleAtlas.uv(render.sprite());
    }

    @Override
    public boolean tick() {
        boolean done = sim.tick();
        if (subEmitter != null && depth < MAX_DEPTH) {
            List<Vec3f> deaths = sim.drainSpawnRequests();
            for (int i = 0; i < deaths.size(); i++) {
                spawnChild(deaths.get(i));
            }
        }
        return done;
    }

    // build the child system where a parent particle died and hand it to the manager as its own effect
    private void spawnChild(Vec3f local) {
        EmitterSpec child = subEmitter.child();
        Vec3 childOrigin = origin.add(local.x(), local.y(), local.z());
        CollisionProbe probe = child.collision().enabled()
                ? new LevelCollisionProbe(Minecraft.getInstance().level, childOrigin)
                : null;
        ParticleSystem childSim = child.build(new Random(), probe);
        VfxRenderManager.get().spawn(
                new ParticleBurstEffect(childOrigin, childSim, child.render(), child.subEmitter(), depth + 1));
    }

    @Override
    public void render(VfxFrame frame) {
        ParticleAtlas.ensureUploaded();
        RenderType type = render.blend() == BlendMode.ALPHA ? VfxRenderTypes.TEXTURED_ALPHA : VfxRenderTypes.TEXTURED_ADDITIVE;
        VertexConsumer vc = frame.buffers().getBuffer(type);
        Vec3 cam = frame.cameraPos();
        Quaternionf camRot = frame.cameraRotation();
        Matrix4f m = frame.pose().last().pose();
        float stretch = render.stretch();
        boolean animate = render.animate();
        // the billboard plane axes in world space, so velocity can be projected onto the quad when streaking
        Vector3f right = camRot.transform(new Vector3f(1f, 0f, 0f));
        Vector3f up = camRot.transform(new Vector3f(0f, 1f, 0f));
        for (Particle p : sim.particles()) {
            int color = sim.colorOf(p);
            int alpha = (int) (sim.alphaOf(p) * 255f);
            float size = sim.sizeOf(p);
            float hx = size;
            float hy = size;
            float roll = p.rotation;
            float[] cell = animate ? ParticleAtlas.uv(render.sprite(), frameOf(p)) : uv;
            if (stretch > 0f) {
                float speed = p.vel.length();
                if (speed > 1e-5f) {
                    float vx = p.vel.x() * right.x + p.vel.y() * right.y + p.vel.z() * right.z;
                    float vy = p.vel.x() * up.x + p.vel.y() * up.y + p.vel.z() * up.z;
                    roll = (float) Math.atan2(vy, vx);
                    hx = size * (1f + speed * stretch);
                }
            }
            Billboards.quad(frame.pose(), vc, camRot,
                    (float) (origin.x + p.pos.x() - cam.x),
                    (float) (origin.y + p.pos.y() - cam.y),
                    (float) (origin.z + p.pos.z() - cam.z),
                    hx, hy, roll, cell,
                    (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
            if (p.trail != null && p.trailCount >= 2) {
                renderTrail(m, vc, p, cam, color, sim.alphaOf(p), size);
            }
        }
    }

    // draws the particle's position history as a camera-facing ribbon that tapers from the moving head
    // back to nothing at the oldest point. The sprite cell is sampled across the width so edges stay soft.
    private void renderTrail(Matrix4f m, VertexConsumer vc, Particle p, Vec3 cam, int color, float headAlpha, float size) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float uMid = (uv[0] + uv[2]) * 0.5f;
        float v0 = uv[1];
        float v1 = uv[3];
        int last = p.trailCount - 1;
        for (int i = 0; i < last; i++) {
            Vec3f lp0 = p.trailPoint(i);
            Vec3f lp1 = p.trailPoint(i + 1);
            Vec3 a = new Vec3(origin.x + lp0.x() - cam.x, origin.y + lp0.y() - cam.y, origin.z + lp0.z() - cam.z);
            Vec3 c = new Vec3(origin.x + lp1.x() - cam.x, origin.y + lp1.y() - cam.y, origin.z + lp1.z() - cam.z);
            Vec3 seg = c.subtract(a);
            double len = seg.length();
            if (len < 1e-6) {
                continue;
            }
            Vec3 dir = seg.scale(1.0 / len);
            Vec3 side = dir.cross(a.scale(-1.0).normalize()).normalize();
            float t0 = (float) i / last;
            float t1 = (float) (i + 1) / last;
            Vec3 s0 = side.scale(size * t0);
            Vec3 s1 = side.scale(size * t1);
            int a0 = (int) (headAlpha * t0 * 255f);
            int a1 = (int) (headAlpha * t1 * 255f);
            vc.addVertex(m, (float) (a.x - s0.x), (float) (a.y - s0.y), (float) (a.z - s0.z)).setUv(uMid, v0).setColor(r, g, b, a0);
            vc.addVertex(m, (float) (a.x + s0.x), (float) (a.y + s0.y), (float) (a.z + s0.z)).setUv(uMid, v1).setColor(r, g, b, a0);
            vc.addVertex(m, (float) (c.x + s1.x), (float) (c.y + s1.y), (float) (c.z + s1.z)).setUv(uMid, v1).setColor(r, g, b, a1);
            vc.addVertex(m, (float) (c.x - s1.x), (float) (c.y - s1.y), (float) (c.z - s1.z)).setUv(uMid, v0).setColor(r, g, b, a1);
        }
    }

    // map a particle's life fraction onto an atlas frame, clamped to the last frame at end of life
    private static int frameOf(Particle p) {
        int frame = (int) (p.life() * ParticleAtlas.FRAMES);
        return frame >= ParticleAtlas.FRAMES ? ParticleAtlas.FRAMES - 1 : frame;
    }
}
