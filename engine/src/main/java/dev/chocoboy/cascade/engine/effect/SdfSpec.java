package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import java.util.List;

// a signed-distance volume: a handful of primitive shapes fused by a smooth union and raymarched by
// the client. size means radius for a sphere, half extents for a box, major/minor radii for a torus.
// rotateSpeed spins the whole volume around its local y axis, radians per tick
public record SdfSpec(List<SdfShape> shapes, float smoothness, ColorSpec color, float alpha,
        int duration, float rotateSpeed) {

    public static final int MAX_SHAPES = 8;

    public SdfSpec {
        if (shapes.isEmpty() || shapes.size() > MAX_SHAPES) {
            throw new IllegalArgumentException("shapes must be 1.." + MAX_SHAPES);
        }
        if (duration < 1) {
            throw new IllegalArgumentException("duration < 1");
        }
        smoothness = Math.max(0f, smoothness);
        shapes = List.copyOf(shapes);
    }

    public enum Type {
        SPHERE, BOX, TORUS
    }

    public record SdfShape(Type type, Vec3f center, Vec3f size) {

        // farthest surface point from the shape's own center
        float extent() {
            return switch (type) {
                case SPHERE -> size.x();
                case BOX -> size.length();
                case TORUS -> size.x() + size.y();
            };
        }
    }

    // radius of a sphere around the volume origin guaranteed to contain every surface point, so the
    // billboard quad and the march interval always cover the whole volume. the smooth union can only
    // bulge outward by about its blend width, hence the smoothness pad
    public float boundRadius() {
        float max = 0f;
        for (SdfShape s : shapes) {
            max = Math.max(max, s.center().length() + s.extent());
        }
        return max + smoothness;
    }
}
