package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

// pins BurstSampler to ParticleSystem.spawn: same seed, same rng draw order, identical launch state.
// a drift here means a gpu-simulated effect would no longer start where its cpu twin does
class BurstSamplerTest {

    private record Launch(float px, float py, float pz, float vx, float vy, float vz, float rotation, float spin) {
    }

    private static EmitterSpec spec(ShapeSpec shape, RotationSpec rotation, VelocitySpec velocity) {
        return new EmitterSpec(shape, 64, 40, 0.3f,
                new CurveSpec(0.2f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                ColorSpec.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                List.of(), EmissionSpec.burst(), RenderSpec.DEFAULT,
                rotation, CollisionSpec.NONE, null, TrailSpec.NONE, velocity);
    }

    private static void assertMatches(EmitterSpec spec) {
        ParticleSystem system = spec.build(new Random(99L));
        List<Launch> sampled = new ArrayList<>();
        BurstSampler.sample(spec, new Random(99L), spec.count(),
                (px, py, pz, vx, vy, vz, rotation, spin) ->
                        sampled.add(new Launch(px, py, pz, vx, vy, vz, rotation, spin)));
        assertEquals(spec.count(), system.particles().size());
        for (int i = 0; i < spec.count(); i++) {
            Particle p = system.particles().get(i);
            Launch s = sampled.get(i);
            assertEquals(p.pos.x(), s.px(), 0f);
            assertEquals(p.pos.y(), s.py(), 0f);
            assertEquals(p.pos.z(), s.pz(), 0f);
            assertEquals(p.vel.x(), s.vx(), 0f);
            assertEquals(p.vel.y(), s.vy(), 0f);
            assertEquals(p.vel.z(), s.vz(), 0f);
            assertEquals(p.rotation, s.rotation(), 0f);
            assertEquals(p.spin, s.spin(), 0f);
        }
    }

    @Test
    void radialSphereMatchesTheSystemSpawn() {
        assertMatches(spec(ShapeSpec.sphere(1.5f), RotationSpec.NONE, VelocitySpec.RADIAL));
    }

    @Test
    void spinningRingMatchesTheSystemSpawn() {
        assertMatches(spec(ShapeSpec.ring(2f), RotationSpec.spin(0.2f), VelocitySpec.orbital()));
    }

    @Test
    void directionalConeWithSpreadMatchesTheSystemSpawn() {
        assertMatches(spec(ShapeSpec.point(), RotationSpec.spin(0.1f),
                VelocitySpec.directional(new Vec3f(0f, 1f, 0f), 0.4f)));
    }
}
