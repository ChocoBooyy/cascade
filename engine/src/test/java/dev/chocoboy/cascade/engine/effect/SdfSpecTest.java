package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

// the bound radius feeds the client's billboard quad and march interval; a bound that misses a surface
// point clips the volume visibly, so every shape type's farthest reach is pinned here
class SdfSpecTest {

    private static final ColorSpec WHITE = ColorSpec.of(0xFFFFFF, 0xFFFFFF, Easings.LINEAR);

    private static SdfSpec spec(float smoothness, SdfSpec.SdfShape... shapes) {
        return new SdfSpec(List.of(shapes), smoothness, WHITE, 1f, 100, 0f);
    }

    @Test
    void rejectsBadInputs() {
        SdfSpec.SdfShape sphere = new SdfSpec.SdfShape(SdfSpec.Type.SPHERE,
                Vec3f.ZERO, new Vec3f(1f, 0f, 0f));
        List<SdfSpec.SdfShape> tooMany = Collections.nCopies(SdfSpec.MAX_SHAPES + 1, sphere);
        assertThrows(IllegalArgumentException.class,
                () -> new SdfSpec(List.of(), 0f, WHITE, 1f, 100, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new SdfSpec(tooMany, 0f, WHITE, 1f, 100, 0f));
        assertThrows(IllegalArgumentException.class,
                () -> new SdfSpec(List.of(sphere), 0f, WHITE, 1f, 0, 0f));
    }

    @Test
    void boundCoversSphere() {
        SdfSpec s = spec(0f, new SdfSpec.SdfShape(SdfSpec.Type.SPHERE,
                new Vec3f(2f, 0f, 0f), new Vec3f(1f, 0f, 0f)));
        assertEquals(3f, s.boundRadius(), 1e-6f);
    }

    @Test
    void boundCoversBoxCorner() {
        SdfSpec s = spec(0f, new SdfSpec.SdfShape(SdfSpec.Type.BOX,
                Vec3f.ZERO, new Vec3f(1f, 2f, 2f)));
        assertEquals(3f, s.boundRadius(), 1e-6f);
    }

    @Test
    void boundCoversTorusRim() {
        SdfSpec s = spec(0f, new SdfSpec.SdfShape(SdfSpec.Type.TORUS,
                Vec3f.ZERO, new Vec3f(1.5f, 0.5f, 0f)));
        assertEquals(2f, s.boundRadius(), 1e-6f);
    }

    @Test
    void boundTakesFarthestShapeAndSmoothnessPad() {
        SdfSpec s = spec(0.5f,
                new SdfSpec.SdfShape(SdfSpec.Type.SPHERE, Vec3f.ZERO, new Vec3f(0.5f, 0f, 0f)),
                new SdfSpec.SdfShape(SdfSpec.Type.SPHERE, new Vec3f(0f, 3f, 0f), new Vec3f(1f, 0f, 0f)));
        assertEquals(4.5f, s.boundRadius(), 1e-6f);
    }

    @Test
    void smoothnessClampsToZero() {
        SdfSpec s = new SdfSpec(List.of(new SdfSpec.SdfShape(SdfSpec.Type.SPHERE,
                Vec3f.ZERO, new Vec3f(1f, 0f, 0f))), -1f, WHITE, 1f, 100, 0f);
        assertEquals(0f, s.smoothness(), 0f);
        assertEquals(1f, s.boundRadius(), 1e-6f);
    }
}
