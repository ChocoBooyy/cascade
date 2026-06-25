package dev.chocoboy.cascade.engine.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
