package dev.chocoboy.cascade.engine.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ShapesTest {

    @Test
    void factoryBuildsEachSampler() {
        assertInstanceOf(PointSampler.class, Shapes.point());
        assertInstanceOf(LineSampler.class, Shapes.line(Vec3f.ZERO, new Vec3f(1f, 0f, 0f)));
        assertInstanceOf(RingSampler.class, Shapes.ring(1f));
        assertInstanceOf(SphereSampler.class, Shapes.sphere(1f));
        assertInstanceOf(ConeSampler.class, Shapes.cone(1f, 2f));
        assertInstanceOf(BoxSampler.class, Shapes.box(new Vec3f(1f, 1f, 1f)));
    }

    @Test
    void factorySphereMatchesDirectConstruction() {
        assertEquals(
                new SphereSampler(2f).sample(new Random(1)),
                Shapes.sphere(2f).sample(new Random(1)));
    }
}
