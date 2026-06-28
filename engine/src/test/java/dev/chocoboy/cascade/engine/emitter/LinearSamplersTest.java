package dev.chocoboy.cascade.engine.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LinearSamplersTest {

    private static final float EPS = 1e-4f;

    @Test
    void pointAlwaysAtOrigin() {
        ShapeSampler s = new PointSampler();
        Random rng = new Random(1);
        for (int i = 0; i < 5; i++) {
            assertEquals(Vec3f.ZERO, s.sample(rng));
        }
    }

    @Test
    void lineSamplesLieOnSegment() {
        Vec3f from = new Vec3f(0f, 0f, 0f);
        Vec3f to = new Vec3f(10f, 0f, 0f);
        ShapeSampler s = new LineSampler(from, to);
        Random rng = new Random(7);
        for (int i = 0; i < 50; i++) {
            Vec3f p = s.sample(rng);
            assertTrue(p.x() >= -EPS && p.x() <= 10f + EPS, "x in range");
            assertEquals(0f, p.y(), EPS);
            assertEquals(0f, p.z(), EPS);
        }
    }

    @Test
    void ringSamplesLieOnCircle() {
        float radius = 3f;
        ShapeSampler s = new RingSampler(radius);
        Random rng = new Random(11);
        for (int i = 0; i < 50; i++) {
            Vec3f p = s.sample(rng);
            assertEquals(radius, p.length(), EPS);
            assertEquals(0f, p.y(), EPS);
        }
    }

    @Test
    void discSamplesFillCircle() {
        float radius = 4f;
        ShapeSampler s = new DiscSampler(radius);
        Random rng = new Random(13);
        for (int i = 0; i < 100; i++) {
            Vec3f p = s.sample(rng);
            assertTrue(p.length() <= radius + EPS, "within radius");
            assertEquals(0f, p.y(), EPS);
        }
    }

    @Test
    void sameSeedSameSequence() {
        ShapeSampler s = new RingSampler(2f);
        assertEquals(s.sample(new Random(42)), s.sample(new Random(42)));
    }
}
