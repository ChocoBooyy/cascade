package dev.chocoboy.cascade.engine.tween;

import java.util.List;
import java.util.Objects;

public final class ColorCurve {

    private final int[] stops;
    private final Easing easing;

    private ColorCurve(int[] stops, Easing easing) {
        this.stops = stops;
        this.easing = easing;
    }

    public static ColorCurve of(int fromRgb, int toRgb, Easing easing) {
        return new ColorCurve(new int[] {fromRgb, toRgb}, Objects.requireNonNull(easing, "easing"));
    }

    public static ColorCurve of(List<Integer> stops, Easing easing) {
        Objects.requireNonNull(easing, "easing");
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("stops < 1");
        }
        int[] arr = new int[stops.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = stops.get(i);
        }
        return new ColorCurve(arr, easing);
    }

    public int at(float t) {
        if (stops.length == 1) {
            return stops[0] & 0xFFFFFF;
        }
        // map t across the segments, then ease within the segment it lands in. two stops collapse to the
        // original single-segment behavior, so existing curves are unchanged.
        float scaled = Curve.clamp01(t) * (stops.length - 1);
        int i = (int) scaled;
        if (i >= stops.length - 1) {
            i = stops.length - 2;
        }
        float e = easing.ease(scaled - i);
        int from = stops[i];
        int to = stops[i + 1];
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
