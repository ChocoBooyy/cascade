package dev.chocoboy.cascade.engine.tween;

import java.util.ArrayList;
import java.util.List;

// Serializable color over a particle's life. Two stops is the common case (of), but a longer list paints
// a multi stop gradient (white -> orange -> red -> ash). The ease shapes the blend within each segment.
public record ColorSpec(List<Integer> stops, Easings ease) {

    public ColorSpec {
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("stops < 1");
        }
        stops = List.copyOf(stops);
    }

    public static ColorSpec of(int fromRgb, int toRgb, Easings ease) {
        return new ColorSpec(List.of(fromRgb, toRgb), ease);
    }

    public static ColorSpec gradient(Easings ease, int... colors) {
        List<Integer> list = new ArrayList<>(colors.length);
        for (int c : colors) {
            list.add(c);
        }
        return new ColorSpec(list, ease);
    }

    public ColorCurve toCurve() {
        return ColorCurve.of(stops, ease);
    }
}
