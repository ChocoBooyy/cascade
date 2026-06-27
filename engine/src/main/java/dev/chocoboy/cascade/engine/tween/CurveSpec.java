package dev.chocoboy.cascade.engine.tween;

public record CurveSpec(float start, float end, Easings ease) {

    public Curve toCurve() {
        return Curve.of(start, end, ease);
    }
}
