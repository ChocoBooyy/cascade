package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class Shapes {

    private Shapes() {
    }

    public static ShapeSampler point() {
        return new PointSampler();
    }

    public static ShapeSampler line(Vec3f from, Vec3f to) {
        return new LineSampler(from, to);
    }

    public static ShapeSampler ring(float radius) {
        return new RingSampler(radius);
    }

    public static ShapeSampler sphere(float radius) {
        return new SphereSampler(radius);
    }

    public static ShapeSampler cone(float radius, float height) {
        return new ConeSampler(radius, height);
    }

    public static ShapeSampler box(Vec3f half) {
        return new BoxSampler(half);
    }
}
