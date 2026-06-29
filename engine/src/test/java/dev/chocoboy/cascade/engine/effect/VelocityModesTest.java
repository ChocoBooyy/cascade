package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.ShapeSampler;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class VelocityModesTest {

    private static final float EPS = 1e-6f;

    // every particle spawns at (2, 0, 0) so the launch direction is unambiguous
    private static final ShapeSampler RIGHT = rng -> new Vec3f(2f, 0f, 0f);

    private static Particle launch(VelocitySpec velocity) {
        ParticleSystem s = new ParticleSystem(
                RIGHT, new BurstSpawner(1), 20, 0.5f, velocity,
                Curve.of(1f, 0f, Easings.LINEAR),
                Curve.of(1f, 0f, Easings.LINEAR),
                ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                List.of(), RotationSpec.NONE, CollisionSpec.NONE, null, false, TrailSpec.NONE, new Random(1));
        return s.particles().get(0);
    }

    @Test
    void radialFiresOutward() {
        Particle p = launch(VelocitySpec.RADIAL);
        assertEquals(0.5f, p.vel.x(), EPS);
        assertEquals(0f, p.vel.y(), EPS);
        assertEquals(0f, p.vel.z(), EPS);
    }

    @Test
    void inwardFiresTowardCenter() {
        Particle p = launch(VelocitySpec.inward());
        assertEquals(-0.5f, p.vel.x(), EPS);
    }

    @Test
    void orbitalFiresTangentToTheVerticalAxis() {
        Particle p = launch(VelocitySpec.orbital());
        assertEquals(0f, p.vel.x(), EPS);
        assertEquals(0.5f, p.vel.z(), EPS);
    }

    @Test
    void directionalFollowsItsVectorWithoutSpread() {
        Particle p = launch(VelocitySpec.directional(new Vec3f(0f, 1f, 0f), 0f));
        assertEquals(0.5f, p.vel.y(), EPS);
        assertEquals(0f, p.vel.x(), EPS);
    }

    @Test
    void directionalSpreadStaysWithinTheCone() {
        // a 30 degree cone around +y; the sample must keep a positive upward bias and unit-ish speed
        Particle p = launch(VelocitySpec.directional(new Vec3f(0f, 1f, 0f), (float) Math.toRadians(30)));
        assertTrue(p.vel.y() > 0f, "spread sample should stay in the upper cone");
        assertEquals(0.5f, p.vel.length(), 1e-4f);
    }
}
