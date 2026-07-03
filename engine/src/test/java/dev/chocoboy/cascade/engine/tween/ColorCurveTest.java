package dev.chocoboy.cascade.engine.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ColorCurveTest {

    @Test
    void interpolatesEachChannel() {
        ColorCurve c = ColorCurve.of(0x000000, 0xFFFFFF, Easings.LINEAR);
        assertEquals(0x000000, c.at(0f));
        assertEquals(0xFFFFFF, c.at(1f));
        assertEquals(0x808080, c.at(0.5f));
    }

    @Test
    void mixesDistinctChannels() {
        ColorCurve c = ColorCurve.of(0xFF0000, 0x0000FF, Easings.LINEAR);
        assertEquals(0x800080, c.at(0.5f));
    }

    @Test
    void clampsInputRange() {
        ColorCurve c = ColorCurve.of(0x102030, 0x405060, Easings.LINEAR);
        assertEquals(0x102030, c.at(-1f));
        assertEquals(0x405060, c.at(2f));
    }

    @Test
    void blendsAcrossMultipleStops() {
        ColorCurve c = ColorCurve.of(List.of(0x000000, 0xFF0000, 0xFFFFFF), Easings.LINEAR);
        assertEquals(0x000000, c.at(0f));
        assertEquals(0xFF0000, c.at(0.5f));
        assertEquals(0xFFFFFF, c.at(1f));
        // a quarter of the way is the midpoint of the first segment, black to red
        assertEquals(0x800000, c.at(0.25f));
    }

    @Test
    void segmentBoundariesLandExactlyOnStops() {
        // four stops split t into thirds; each boundary must return its stop bit-exact, no blend bleed
        ColorCurve c = ColorCurve.of(List.of(0x112233, 0x445566, 0x778899, 0xAABBCC), Easings.LINEAR);
        assertEquals(0x112233, c.at(0f));
        assertEquals(0x445566, c.at(1f / 3f));
        assertEquals(0x778899, c.at(2f / 3f));
        assertEquals(0xAABBCC, c.at(1f));
    }

    @Test
    void clampsOvershootEasing() {
        ColorCurve c = ColorCurve.of(0x000000, 0xFFFFFF, t -> 1.5f);
        assertEquals(0xFFFFFF, c.at(0.5f));
    }

    @Test
    void clampsUndershootEasing() {
        ColorCurve c = ColorCurve.of(0x000000, 0xFFFFFF, t -> -0.5f);
        assertEquals(0x000000, c.at(0.5f));
    }
}
