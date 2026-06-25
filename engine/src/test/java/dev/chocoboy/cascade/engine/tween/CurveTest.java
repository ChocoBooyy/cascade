package dev.chocoboy.cascade.engine.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CurveTest {

    private static final float EPS = 1e-5f;

    @Test
    void linearInterpolates() {
        Curve c = Curve.of(0f, 10f, Easings.LINEAR);
        assertEquals(0f, c.at(0f), EPS);
        assertEquals(5f, c.at(0.5f), EPS);
        assertEquals(10f, c.at(1f), EPS);
    }

    @Test
    void clampsInputRange() {
        Curve c = Curve.of(0f, 10f, Easings.LINEAR);
        assertEquals(0f, c.at(-2f), EPS);
        assertEquals(10f, c.at(3f), EPS);
    }

    @Test
    void appliesEasing() {
        Curve c = Curve.of(1f, 0f, Easings.EASE_OUT_QUAD);
        assertEquals(1f, c.at(0f), EPS);
        assertEquals(0.25f, c.at(0.5f), EPS);
        assertEquals(0f, c.at(1f), EPS);
    }

    @Test
    void rejectsNullEasing() {
        assertThrows(NullPointerException.class, () -> Curve.of(0f, 1f, null));
    }
}
