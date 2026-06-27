package dev.chocoboy.cascade.engine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoiseTest {

    @Test
    void staysWithinUnitRange() {
        for (int i = 0; i < 2000; i++) {
            float x = (i * 7.13f) % 50f - 25f;
            float y = (i * 3.91f) % 50f - 25f;
            float z = (i * 11.27f) % 50f - 25f;
            float n = Noise.value(x, y, z);
            assertTrue(n >= -1f && n <= 1f, "out of range at " + i + ": " + n);
        }
    }

    @Test
    void deterministic() {
        assertEquals(Noise.value(1.5f, 2.5f, 3.5f), Noise.value(1.5f, 2.5f, 3.5f));
    }

    @Test
    void continuousBetweenLatticePoints() {
        float a = Noise.value(4f, 4f, 4f);
        float b = Noise.value(4.01f, 4f, 4f);
        assertTrue(Math.abs(a - b) < 0.05f, "discontinuous: " + a + " vs " + b);
    }

    @Test
    void variesAcrossSpace() {
        assertTrue(Noise.value(0f, 0f, 0f) != Noise.value(8f, 13f, 21f));
    }
}
