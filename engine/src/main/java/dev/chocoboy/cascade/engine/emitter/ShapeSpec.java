package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;

/**
 * The spawn volume an emitter samples: a point, line, ring, sphere, cone, box, disc or hemisphere. Build one
 * with the static factories, e.g. {@code ShapeSpec.sphere(radius)} or {@code ShapeSpec.cone(radius, height)}.
 * Sizes are in blocks.
 */
public record ShapeSpec(Kind kind, float radius, float height, Vec3f a, Vec3f b) {

    public enum Kind {
        POINT, LINE, RING, SPHERE, CONE, BOX, DISC, HEMISPHERE
    }

    public static ShapeSpec point() {
        return new ShapeSpec(Kind.POINT, 0f, 0f, Vec3f.ZERO, Vec3f.ZERO);
    }

    public static ShapeSpec line(Vec3f from, Vec3f to) {
        return new ShapeSpec(Kind.LINE, 0f, 0f, from, to);
    }

    public static ShapeSpec ring(float radius) {
        return new ShapeSpec(Kind.RING, radius, 0f, Vec3f.ZERO, Vec3f.ZERO);
    }

    public static ShapeSpec sphere(float radius) {
        return new ShapeSpec(Kind.SPHERE, radius, 0f, Vec3f.ZERO, Vec3f.ZERO);
    }

    public static ShapeSpec cone(float radius, float height) {
        return new ShapeSpec(Kind.CONE, radius, height, Vec3f.ZERO, Vec3f.ZERO);
    }

    public static ShapeSpec box(Vec3f half) {
        return new ShapeSpec(Kind.BOX, 0f, 0f, half, Vec3f.ZERO);
    }

    public static ShapeSpec disc(float radius) {
        return new ShapeSpec(Kind.DISC, radius, 0f, Vec3f.ZERO, Vec3f.ZERO);
    }

    public static ShapeSpec hemisphere(float radius) {
        return new ShapeSpec(Kind.HEMISPHERE, radius, 0f, Vec3f.ZERO, Vec3f.ZERO);
    }

    public ShapeSampler sampler() {
        return switch (kind) {
            case POINT -> new PointSampler();
            case LINE -> new LineSampler(a, b);
            case RING -> new RingSampler(radius);
            case SPHERE -> new SphereSampler(radius);
            case CONE -> new ConeSampler(radius, height);
            case BOX -> new BoxSampler(a);
            case DISC -> new DiscSampler(radius);
            case HEMISPHERE -> new HemisphereSampler(radius);
        };
    }
}
