package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MeshOrientationTest {

    private static EmitterSpec mesh(RotationSpec rotation) {
        return new EmitterSpec(ShapeSpec.point(), 3, 10, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR), new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR,
                List.of(), EmissionSpec.burst(),
                new RenderSpec(BlendMode.ADDITIVE, SpriteId.GLOW, 0f, false, false, MeshId.CUBE),
                rotation, CollisionSpec.NONE);
    }

    @Test
    void defaultRenderSpecIsBillboard() {
        assertEquals(MeshId.NONE, RenderSpec.DEFAULT.mesh());
    }

    @Test
    void tumbleAdvancesAllThreeAxes() {
        ParticleSystem s = mesh(RotationSpec.spin(0.2f)).build(new Random(5));
        Particle p = s.particles().get(0);
        float pitch0 = p.pitch, yaw0 = p.yaw, roll0 = p.rotation;
        s.tick();
        assertEquals(pitch0 + p.pitchSpin, p.pitch, 1e-6f);
        assertEquals(yaw0 + p.yawSpin, p.yaw, 1e-6f);
        assertEquals(roll0 + p.spin, p.rotation, 1e-6f);
        // a tumble system should actually have nonzero spin on the new axes
        assertTrue(p.pitchSpin != 0f || p.yawSpin != 0f);
    }

    @Test
    void billboardSystemLeavesMeshAxesZero() {
        EmitterSpec billboard = new EmitterSpec(ShapeSpec.point(), 3, 10, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR), new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR);
        ParticleSystem s = billboard.build(new Random(5));
        Particle p = s.particles().get(0);
        s.tick();
        assertEquals(0f, p.pitch, 1e-6f);
        assertEquals(0f, p.yaw, 1e-6f);
    }
}
