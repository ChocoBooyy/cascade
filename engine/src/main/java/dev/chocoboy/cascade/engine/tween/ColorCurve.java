package dev.chocoboy.cascade.engine.tween;

import java.util.Objects;

public final class ColorCurve {

    private final int from;
    private final int to;
    private final Easing easing;

    private ColorCurve(int from, int to, Easing easing) {
        this.from = from;
        this.to = to;
        this.easing = easing;
    }

    public static ColorCurve of(int fromRgb, int toRgb, Easing easing) {
        return new ColorCurve(fromRgb, toRgb, Objects.requireNonNull(easing, "easing"));
    }

    public int at(float t) {
        float e = easing.ease(Curve.clamp01(t));
        int r = channel(from >> 16, to >> 16, e);
        int g = channel(from >> 8, to >> 8, e);
        int b = channel(from, to, e);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int fromShifted, int toShifted, float e) {
        int a = fromShifted & 0xFF;
        int b = toShifted & 0xFF;
        int value = Math.round(a + (b - a) * e);
        return Math.min(255, Math.max(0, value));
    }
}
