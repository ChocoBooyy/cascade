package dev.chocoboy.cascade.engine.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EasingsTest {

    private static final float EPS = 1e-5f;

    @Test
    void endpointsArePinned() {
        for (Easings e : Easings.values()) {
            assertEquals(0f, e.ease(0f), EPS, e + " at 0");
            assertEquals(1f, e.ease(1f), EPS, e + " at 1");
        }
    }

    @Test
    void linearIsIdentity() {
        assertEquals(0.25f, Easings.LINEAR.ease(0.25f), EPS);
        assertEquals(0.5f, Easings.LINEAR.ease(0.5f), EPS);
    }

    @Test
    void quadShapesMatchFormula() {
        assertEquals(0.25f, Easings.EASE_IN_QUAD.ease(0.5f), EPS);
        assertEquals(0.75f, Easings.EASE_OUT_QUAD.ease(0.5f), EPS);
    }

    @Test
    void inOutQuadCoversBothBranches() {
        assertEquals(0.125f, Easings.EASE_IN_OUT_QUAD.ease(0.25f), EPS);
        assertEquals(0.875f, Easings.EASE_IN_OUT_QUAD.ease(0.75f), EPS);
    }
}
