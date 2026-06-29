package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TrailTest {

    private static EmitterSpec spec(TrailSpec trail) {
        return new EmitterSpec(
                ShapeSpec.sphere(2f), 4, 20, 1f,
                new CurveSpec(1f, 0f, Easings.LINEAR),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR,
                List.of(), EmissionSpec.burst(), RenderSpec.DEFAULT,
                RotationSpec.NONE, CollisionSpec.NONE, null, trail);
    }

    @Test
    void recordsOnePointAfterOneTick() {
        ParticleSystem s = spec(TrailSpec.of(4)).build(new Random(1));
        s.tick();
        for (Particle p : s.particles()) {
            assertNotNull(p.trail);
            assertEquals(1, p.trailCount);
        }
    }

    @Test
    void trailCapsAtLengthAndTracksCurrentPos() {
        int len = 3;
        ParticleSystem s = spec(TrailSpec.of(len)).build(new Random(1));
        for (int t = 0; t < 8; t++) {
            s.tick();
            for (Particle p : s.particles()) {
                assertTrue(p.trailCount <= len);
                assertEquals(p.pos, p.trailPoint(p.trailCount - 1));
            }
        }
        for (Particle p : s.particles()) {
            assertEquals(len, p.trailCount);
        }
    }

    @Test
    void noTrailLeavesArrayNull() {
        ParticleSystem s = spec(TrailSpec.NONE).build(new Random(1));
        for (int t = 0; t < 5; t++) {
            s.tick();
        }
        for (Particle p : s.particles()) {
            assertNull(p.trail);
        }
    }
}
