package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

// the seeded-replay contract: the same spec and seed must produce the same simulation, tick for tick,
// across every shape and launch mode. this is what lets a networked effect look identical on every client
class ReplayDeterminismTest {

    private static final List<ShapeSpec> SHAPES = List.of(
            ShapeSpec.point(),
            ShapeSpec.line(new Vec3f(-1f, 0f, 0f), new Vec3f(1f, 2f, 0f)),
            ShapeSpec.ring(1.5f),
            ShapeSpec.sphere(2f),
            ShapeSpec.cone(1f, 2f),
            ShapeSpec.box(new Vec3f(1f, 0.5f, 2f)),
            ShapeSpec.disc(1.5f),
            ShapeSpec.hemisphere(2f));

    private static final List<VelocitySpec> LAUNCHES = List.of(
            VelocitySpec.RADIAL,
            VelocitySpec.inward(),
            VelocitySpec.orbital(),
            // spread makes directional draw from the rng per particle, the risky case
            VelocitySpec.directional(new Vec3f(0.3f, 1f, -0.2f), 0.6f));

    private static EmitterSpec spec(ShapeSpec shape, VelocitySpec velocity) {
        return new EmitterSpec(shape, 12, 15, 0.1f,
                new CurveSpec(0.3f, 0f, Easings.EASE_OUT_QUAD),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                ColorSpec.of(0xFFCC33, 0xFF3300, Easings.LINEAR),
                List.of(new GravitySpec(new Vec3f(0f, -0.02f, 0f)),
                        new DragSpec(0.05f),
                        new VortexSpec(Vec3f.ZERO, 0.03f)),
                EmissionSpec.burst(), RenderSpec.DEFAULT, RotationSpec.spin(0.2f),
                CollisionSpec.NONE, null, TrailSpec.NONE, velocity);
    }

    @Test
    void sameSeedReplaysIdenticallyAcrossShapesAndLaunchModes() {
        for (ShapeSpec shape : SHAPES) {
            for (VelocitySpec velocity : LAUNCHES) {
                EmitterSpec spec = spec(shape, velocity);
                ParticleSystem a = spec.build(new Random(31L));
                ParticleSystem b = spec.build(new Random(31L));
                String label = shape.kind() + "/" + velocity.mode();
                for (int t = 0; t < 20; t++) {
                    assertSameState(a, b, label + " tick " + t);
                    a.tick();
                    b.tick();
                }
            }
        }
    }

    @Test
    void rateEmissionReplaysIdenticallyThroughSpawnAndReapChurn() {
        EmitterSpec spec = new EmitterSpec(ShapeSpec.sphere(1f), 0, 4, 0.08f,
                new CurveSpec(0.2f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                ColorSpec.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                List.of(new GravitySpec(new Vec3f(0f, -0.01f, 0f))),
                EmissionSpec.rate(2.5f, 30), RenderSpec.DEFAULT, RotationSpec.spin(0.1f),
                CollisionSpec.NONE, null, TrailSpec.NONE, VelocitySpec.RADIAL);
        ParticleSystem a = spec.build(new Random(7L));
        ParticleSystem b = spec.build(new Random(7L));
        for (int t = 0; t < 40; t++) {
            assertSameState(a, b, "churn tick " + t);
            a.tick();
            b.tick();
        }
    }

    private static void assertSameState(ParticleSystem a, ParticleSystem b, String label) {
        assertEquals(a.particles().size(), b.particles().size(), label + " count");
        for (int i = 0; i < a.particles().size(); i++) {
            Particle pa = a.particles().get(i);
            Particle pb = b.particles().get(i);
            String at = label + " particle " + i;
            assertEquals(pa.pos, pb.pos, at + " pos");
            assertEquals(pa.vel, pb.vel, at + " vel");
            assertEquals(pa.age, pb.age, at + " age");
            assertEquals(pa.rotation, pb.rotation, at + " rotation");
            assertEquals(pa.spin, pb.spin, at + " spin");
        }
    }
}
