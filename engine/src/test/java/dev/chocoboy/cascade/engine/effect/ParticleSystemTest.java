package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ParticleSystemTest {

    private static final float EPS = 1e-4f;

    private static ParticleSystem system(int count, int lifetime, float speed) {
        return new ParticleSystem(
                Shapes.sphere(2f), count, lifetime, speed,
                Curve.of(1f, 0f, Easings.LINEAR),
                Curve.of(1f, 0f, Easings.LINEAR),
                ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                new Random(4));
    }

    @Test
    void burstSpawnsCount() {
        assertEquals(10, system(10, 20, 0f).particles().size());
    }

    @Test
    void zeroCountIsDoneImmediately() {
        assertTrue(system(0, 20, 0f).isDone());
    }

    @Test
    void rejectsBadArgs() {
        assertThrows(IllegalArgumentException.class, () -> system(-1, 20, 0f));
        assertThrows(IllegalArgumentException.class, () -> system(5, 0, 0f));
    }

    @Test
    void particlesAgeAndDie() {
        ParticleSystem s = system(5, 3, 0f);
        assertFalse(s.tick());
        assertFalse(s.tick());
        assertTrue(s.tick());
        assertTrue(s.isDone());
    }

    @Test
    void particleMovesByVelocity() {
        ParticleSystem s = system(1, 20, 2f);
        Particle p = s.particles().get(0);
        Vec3f before = p.pos;
        Vec3f vel = p.vel;
        s.tick();
        assertEquals(before.x() + vel.x(), p.pos.x(), EPS);
        assertEquals(before.y() + vel.y(), p.pos.y(), EPS);
        assertEquals(before.z() + vel.z(), p.pos.z(), EPS);
        assertEquals(1, p.age);
    }

    @Test
    void attributesComeFromCurvesAtBirth() {
        ParticleSystem s = system(1, 20, 0f);
        Particle p = s.particles().get(0);
        assertEquals(1f, s.sizeOf(p), EPS);
        assertEquals(1f, s.alphaOf(p), EPS);
        assertEquals(0xFFFFFF, s.colorOf(p));
    }

    @Test
    void rateEmissionSpawnsOverTimeAndStaysAliveWhileEmitting() {
        ParticleSystem s = new ParticleSystem(
                Shapes.sphere(2f), new RateSpawner(3f, 4), 50, 0f,
                Curve.of(1f, 0f, Easings.LINEAR),
                Curve.of(1f, 0f, Easings.LINEAR),
                ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                List.of(), RotationSpec.NONE, new Random(2));
        assertEquals(3, s.particles().size());
        s.tick();
        assertEquals(6, s.particles().size());
        assertFalse(s.isDone());
    }

    @Test
    void deterministicWithSeed() {
        assertEquals(system(3, 20, 1f).particles().get(0).pos,
                system(3, 20, 1f).particles().get(0).pos);
    }
}
