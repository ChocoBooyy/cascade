package dev.chocoboy.cascade.engine.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Random;
import org.junit.jupiter.api.Test;

class VolumetricSamplersTest {

    private static final float EPS = 1e-4f;

    @Test
    void sphereSamplesOnSurface() {
        float radius = 2.5f;
        ShapeSampler s = new SphereSampler(radius);
        Random rng = new Random(3);
        for (int i = 0; i < 100; i++) {
            assertEquals(radius, s.sample(rng).length(), 1e-3f);
        }
    }

    @Test
    void coneSamplesWithinTaper() {
        float radius = 2f;
        float height = 4f;
        ShapeSampler s = new ConeSampler(radius, height);
        Random rng = new Random(5);
        for (int i = 0; i < 100; i++) {
            Vec3f p = s.sample(rng);
            assertTrue(p.y() >= -EPS && p.y() <= height + EPS, "y in range");
            float radial = (float) Math.sqrt(p.x() * p.x() + p.z() * p.z());
            float maxRadial = radius * (p.y() / height);
            assertTrue(radial <= maxRadial + EPS, "radial within taper");
        }
    }

    @Test
    void boxSamplesWithinHalfExtents() {
        Vec3f half = new Vec3f(1f, 2f, 3f);
        ShapeSampler s = new BoxSampler(half);
        Random rng = new Random(9);
        for (int i = 0; i < 100; i++) {
            Vec3f p = s.sample(rng);
            assertTrue(Math.abs(p.x()) <= half.x() + EPS, "x");
            assertTrue(Math.abs(p.y()) <= half.y() + EPS, "y");
            assertTrue(Math.abs(p.z()) <= half.z() + EPS, "z");
        }
    }

    @Test
    void hemisphereSamplesOnUpperSurface() {
        float radius = 2.5f;
        ShapeSampler s = new HemisphereSampler(radius);
        Random rng = new Random(7);
        for (int i = 0; i < 100; i++) {
            Vec3f p = s.sample(rng);
            assertEquals(radius, p.length(), 1e-3f);
            assertTrue(p.y() >= -EPS, "upper half");
        }
    }

    @Test
    void sameSeedSameSequence() {
        ShapeSampler s = new SphereSampler(1f);
        assertEquals(s.sample(new Random(2)), s.sample(new Random(2)));
    }
}
