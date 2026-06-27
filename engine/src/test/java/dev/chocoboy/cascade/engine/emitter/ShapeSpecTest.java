package dev.chocoboy.cascade.engine.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ShapeSpecTest {

    @Test
    void factoriesSetKind() {
        assertEquals(ShapeSpec.Kind.SPHERE, ShapeSpec.sphere(2f).kind());
        assertEquals(ShapeSpec.Kind.CONE, ShapeSpec.cone(1f, 3f).kind());
        assertEquals(ShapeSpec.Kind.BOX, ShapeSpec.box(new Vec3f(1f, 1f, 1f)).kind());
        assertEquals(ShapeSpec.Kind.POINT, ShapeSpec.point().kind());
        assertEquals(ShapeSpec.Kind.LINE, ShapeSpec.line(Vec3f.ZERO, new Vec3f(1f, 0f, 0f)).kind());
        assertEquals(ShapeSpec.Kind.RING, ShapeSpec.ring(2f).kind());
    }

    @Test
    void samplerMatchesKind() {
        assertInstanceOf(SphereSampler.class, ShapeSpec.sphere(2f).sampler());
        assertInstanceOf(ConeSampler.class, ShapeSpec.cone(1f, 3f).sampler());
        assertInstanceOf(BoxSampler.class, ShapeSpec.box(new Vec3f(1f, 1f, 1f)).sampler());
        assertInstanceOf(PointSampler.class, ShapeSpec.point().sampler());
        assertInstanceOf(LineSampler.class, ShapeSpec.line(Vec3f.ZERO, new Vec3f(1f, 0f, 0f)).sampler());
        assertInstanceOf(RingSampler.class, ShapeSpec.ring(2f).sampler());
    }

    @Test
    void sphereSamplerHonorsRadius() {
        assertEquals(2.5f, ShapeSpec.sphere(2.5f).sampler().sample(new Random(1)).length(), 1e-3f);
    }
}
