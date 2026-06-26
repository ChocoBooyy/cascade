package dev.chocoboy.cascade.engine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Vec3fTest {

    private static final float EPS = 1e-5f;

    @Test
    void addsComponentwise() {
        Vec3f r = new Vec3f(1f, 2f, 3f).add(new Vec3f(4f, 5f, 6f));
        assertEquals(5f, r.x(), EPS);
        assertEquals(7f, r.y(), EPS);
        assertEquals(9f, r.z(), EPS);
    }

    @Test
    void scalesByFactor() {
        Vec3f r = new Vec3f(1f, -2f, 3f).scale(2f);
        assertEquals(2f, r.x(), EPS);
        assertEquals(-4f, r.y(), EPS);
        assertEquals(6f, r.z(), EPS);
    }

    @Test
    void lengthIsEuclidean() {
        assertEquals(5f, new Vec3f(3f, 4f, 0f).length(), EPS);
    }

    @Test
    void normalizeGivesUnitLength() {
        assertEquals(1f, new Vec3f(0f, 0f, 5f).normalize().length(), EPS);
    }

    @Test
    void normalizeZeroStaysZero() {
        assertEquals(Vec3f.ZERO, Vec3f.ZERO.normalize());
    }
}
