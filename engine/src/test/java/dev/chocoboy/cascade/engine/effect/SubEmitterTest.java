package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SubEmitterTest {

    private static EmitterSpec child() {
        return new EmitterSpec(ShapeSpec.point(), 4, 10, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR), new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR);
    }

    @Test
    void defaultTriggerIsDeath() {
        SubEmitterSpec sub = new SubEmitterSpec(child());
        assertEquals(SubEmitterSpec.Trigger.DEATH, sub.trigger());
    }

    @Test
    void deathsRecordSpawnRequestsAtParticlePositions() {
        EmitterSpec spec = new EmitterSpec(ShapeSpec.sphere(2f), 5, 3, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR), new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR,
                List.of(), EmissionSpec.burst(), RenderSpec.DEFAULT, RotationSpec.NONE, CollisionSpec.NONE,
                new SubEmitterSpec(child()));
        ParticleSystem s = spec.build(new Random(7));

        // speed is zero so particles never move, death positions match birth positions
        List<Vec3f> births = new ArrayList<>();
        for (Particle p : s.particles()) {
            births.add(p.pos);
        }

        List<Vec3f> requests = new ArrayList<>();
        while (!s.isDone()) {
            s.tick();
            requests.addAll(s.drainSpawnRequests());
        }

        assertEquals(5, requests.size());
        for (Vec3f pos : requests) {
            assertTrue(births.contains(pos));
        }
        assertTrue(s.drainSpawnRequests().isEmpty());
    }

    @Test
    void collisionTriggerRecordsRequestOnFirstContactOnly() {
        CollisionProbe floor = (x, y, z) -> y < 0f;
        ParticleSystem s = new ParticleSystem(
                Shapes.point(), new BurstSpawner(0), 20, 0f,
                VelocitySpec.RADIAL,
                Curve.of(1f, 0f, Easings.LINEAR),
                Curve.of(1f, 0f, Easings.LINEAR),
                ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                List.of(), RotationSpec.NONE, CollisionSpec.bouncy(0f, 0f), floor,
                false, true, TrailSpec.NONE, new Random(1));
        Particle p = new Particle(new Vec3f(0f, 0.05f, 0f), new Vec3f(0f, -0.1f, 0f), 20);
        s.particles().add(p);

        s.tick();
        assertEquals(1, s.drainSpawnRequests().size());
        // sitting on the floor it keeps contacting, but the one-shot flag suppresses further requests
        s.tick();
        assertTrue(s.drainSpawnRequests().isEmpty());
    }

    @Test
    void withoutSubEmitterNeverRecordsRequests() {
        EmitterSpec spec = new EmitterSpec(ShapeSpec.sphere(2f), 5, 3, 0f,
                new CurveSpec(1f, 0f, Easings.LINEAR), new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFFFFF, 0x000000, Easings.LINEAR);
        ParticleSystem s = spec.build(new Random(7));
        while (!s.isDone()) {
            s.tick();
            assertTrue(s.drainSpawnRequests().isEmpty());
        }
    }
}
