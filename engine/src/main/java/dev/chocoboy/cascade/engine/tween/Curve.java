package dev.chocoboy.cascade.engine.tween;

import java.util.Objects;

public final class Curve {

    private final float from;
    private final float to;
    private final Easing easing;

    private Curve(float from, float to, Easing easing) {
        this.from = from;
        this.to = to;
        this.easing = easing;
    }

    public static Curve of(float from, float to, Easing easing) {
        return new Curve(from, to, Objects.requireNonNull(easing, "easing"));
    }

    public float at(float t) {
        float e = easing.ease(clamp01(t));
        return from + (to - from) * e;
    }

    static float clamp01(float t) {
        if (t < 0f) {
            return 0f;
        }
        return t > 1f ? 1f : t;
    }
}
