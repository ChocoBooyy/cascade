package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

public final class ParticleBurstEffect implements RenderedEffect {

    private final Vec3 origin;
    private final ParticleSystem sim;
    private final BlendMode blend;
    private final float[] uv;

    public ParticleBurstEffect(Vec3 origin, ParticleSystem sim, BlendMode blend, SpriteId sprite) {
        this.origin = origin;
        this.sim = sim;
        this.blend = blend;
        this.uv = ParticleAtlas.uv(sprite);
    }

    @Override
    public boolean tick() {
        return sim.tick();
    }

    @Override
    public void render(VfxFrame frame) {
        ParticleAtlas.ensureUploaded();
        RenderType type = blend == BlendMode.ALPHA ? VfxRenderTypes.TEXTURED_ALPHA : VfxRenderTypes.TEXTURED_ADDITIVE;
        VertexConsumer vc = frame.buffers().getBuffer(type);
        Vec3 cam = frame.cameraPos();
        for (Particle p : sim.particles()) {
            int color = sim.colorOf(p);
            int alpha = (int) (sim.alphaOf(p) * 255f);
            Billboards.quad(frame.pose(), vc, frame.cameraRotation(),
                    (float) (origin.x + p.pos.x() - cam.x),
                    (float) (origin.y + p.pos.y() - cam.y),
                    (float) (origin.z + p.pos.z() - cam.z),
                    sim.sizeOf(p), p.rotation, uv,
                    (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
        }
    }
}
