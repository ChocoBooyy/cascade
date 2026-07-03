package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

// pins the documented hard ceiling (4000, the reason BurstSampler exists for gpu-scale counts) and the
// pool recycling invariants that keep steady-state emission allocation-free
class CapAndPoolingTest {

    private static final int CAP = 4000;

    private static ParticleSystem system(Spawner spawner, int lifetime) {
        return new ParticleSystem(Shapes.sphere(1f), spawner,
                ParticleConfig.burst(lifetime, 0.05f,
                                Curve.of(1f, 0f, Easings.LINEAR),
                                Curve.of(1f, 0f, Easings.LINEAR),
                                ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR))
                        .withRotation(RotationSpec.spin(0.1f)),
                new Random(11));
    }

    @Test
    void absurdBurstCountClampsAtTheCap() {
        assertEquals(CAP, system(new BurstSpawner(100000), 20).particles().size());
    }

    @Test
    void rateEmissionNeverGrowsPastTheCap() {
        ParticleSystem s = system(new RateSpawner(1000f, 50), 40);
        for (int t = 0; t < 50; t++) {
            s.tick();
            assertTrue(s.particles().size() <= CAP, "tick " + t + " exceeded the cap");
        }
    }

    @Test
    void churnRecyclesInstancesInsteadOfAllocating() {
        // 20 spawns a tick, 3-tick lives: ~2000 spawns over the run but only ~60 alive at once, so a
        // recycling pool needs well under 200 distinct instances; a fresh allocation per spawn needs 2000
        ParticleSystem s = system(new RateSpawner(20f, 100), 3);
        Set<Particle> everLive = Collections.newSetFromMap(new IdentityHashMap<>());
        everLive.addAll(s.particles());
        for (int t = 0; t < 100; t++) {
            s.tick();
            everLive.addAll(s.particles());
        }
        assertTrue(everLive.size() < 200,
                "expected instance reuse, saw " + everLive.size() + " distinct instances");
    }

    @Test
    void liveListNeverHoldsTheSameInstanceTwice() {
        ParticleSystem s = system(new RateSpawner(15f, 60), 4);
        for (int t = 0; t < 60; t++) {
            s.tick();
            Set<Particle> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Particle p : s.particles()) {
                assertTrue(seen.add(p), "tick " + t + " has a duplicate live instance");
            }
        }
    }

    @Test
    void recycledParticlesComeBackFullyReset() {
        ParticleSystem s = system(new RateSpawner(10f, 80), 3);
        int checked = 0;
        for (int t = 0; t < 30; t++) {
            s.tick();
            for (Particle p : s.particles()) {
                assertTrue(p.age >= 0 && p.age < p.lifetime, "live particle age out of range");
                // a spawn ages once inside the same tick, so age 1 right after tick() marks it fresh
                if (p.age == 1) {
                    assertFalse(p.collided, "recycled particle kept a stale collided flag");
                    assertEquals(0, p.trailCount, "recycled particle kept stale trail state");
                    checked++;
                }
            }
        }
        assertTrue(checked > 20, "expected fresh spawns to be observed, saw " + checked);
    }
}
