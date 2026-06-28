package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EmitterSpecTest {

    @Test
    void buildsParticleSystemWithCount() {
        EmitterSpec spec = new EmitterSpec(
                ShapeSpec.sphere(1.5f), 80, 20, 0.1f,
                new CurveSpec(0.3f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0x66CCFF, 0x0033FF, Easings.LINEAR);
        assertEquals(80, spec.build(new Random(1)).particles().size());
    }

    @Test
    void gravityModifierPullsParticlesDown() {
        EmitterSpec spec = new EmitterSpec(
                ShapeSpec.sphere(1f), 5, 50, 0f,
                new CurveSpec(0.3f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0xFFFFFF, Easings.LINEAR,
                List.of(ModifierSpec.gravity(new Vec3f(0f, -0.1f, 0f))));
        ParticleSystem sys = spec.build(new Random(1));
        Particle p = sys.particles().get(0);
        float startY = p.pos.y();
        sys.tick();
        sys.tick();
        assertTrue(p.pos.y() < startY, "gravity should lower the particle");
    }

    @Test
    void rateEmissionBuildsAContinuousSystem() {
        EmitterSpec spec = new EmitterSpec(
                ShapeSpec.point(), 0, 50, 0f,
                new CurveSpec(0.3f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0xFFFFFF, Easings.LINEAR,
                List.of(), EmissionSpec.rate(4f, 3));
        ParticleSystem sys = spec.build(new Random(1));
        assertEquals(4, sys.particles().size());
        sys.tick();
        assertEquals(8, sys.particles().size());
    }

    @Test
    void spinAdvancesParticleRotationEachTick() {
        EmitterSpec spec = new EmitterSpec(
                ShapeSpec.sphere(1f), 4, 50, 0f,
                new CurveSpec(0.3f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0xFFFFFF, Easings.LINEAR,
                List.of(), EmissionSpec.burst(), BlendMode.ADDITIVE, SpriteId.SPARK,
                RotationSpec.spin(0.2f));
        ParticleSystem sys = spec.build(new Random(7));
        Particle p = sys.particles().get(0);
        float before = p.rotation;
        float spin = p.spin;
        assertTrue(spin != 0f, "spin should be seeded non-zero");
        sys.tick();
        assertEquals(before + spin, p.rotation, 1e-6f);
    }

    @Test
    void renderDefaultsAreAdditiveGlow() {
        EmitterSpec spec = new EmitterSpec(
                ShapeSpec.point(), 1, 10, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0xFFFFFF, Easings.LINEAR);
        assertEquals(BlendMode.ADDITIVE, spec.blend());
        assertEquals(SpriteId.GLOW, spec.sprite());
    }

    @Test
    void defaultBurstIsTheOriginalPreset() {
        EmitterSpec d = EmitterSpec.defaultBurst();
        assertEquals(120, d.count());
        assertEquals(30, d.lifetime());
        assertEquals(0xFFCC33, d.colorStart());
        assertEquals(0xFF3300, d.colorEnd());
    }
}
