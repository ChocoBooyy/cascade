package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

public final class ParticleBurstEffect implements RenderedEffect {

    private final Vec3 origin;
    private final ParticleSystem sim;

    private ParticleBurstEffect(Vec3 origin, ParticleSystem sim) {
        this.origin = origin;
        this.sim = sim;
    }

    public static ParticleBurstEffect burst(Vec3 origin, long seed) {
        ParticleSystem sim = new ParticleSystem(
                Shapes.sphere(1.5f), 120, 30, 0.08f,
                Curve.of(0.25f, 0.0f, Easings.EASE_OUT_QUAD),
                Curve.of(1.0f, 0.0f, Easings.LINEAR),
                ColorCurve.of(0xFFCC33, 0xFF3300, Easings.LINEAR),
                new Random(seed));
        return new ParticleBurstEffect(origin, sim);
    }

    @Override
    public boolean tick() {
        return sim.tick();
    }

    @Override
    public void render(VfxFrame frame) {
        Vec3 cam = frame.cameraPos();
        for (Particle p : sim.particles()) {
            int color = sim.colorOf(p);
            int alpha = (int) (sim.alphaOf(p) * 255f);
            Billboards.quad(frame.pose(), frame.vertexConsumer(), frame.cameraRotation(),
                    (float) (origin.x + p.pos.x() - cam.x),
                    (float) (origin.y + p.pos.y() - cam.y),
                    (float) (origin.z + p.pos.z() - cam.z),
                    sim.sizeOf(p),
                    (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
        }
    }
}
