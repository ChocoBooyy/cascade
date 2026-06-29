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
        }
    }

    // map a particle's life fraction onto an atlas frame, clamped to the last frame at end of life
    private static int frameOf(Particle p) {
        int frame = (int) (p.life() * ParticleAtlas.FRAMES);
        return frame >= ParticleAtlas.FRAMES ? ParticleAtlas.FRAMES - 1 : frame;
    }
}
