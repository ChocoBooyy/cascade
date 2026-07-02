package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.emitter.Shapes;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FlockTest {

    private static ParticleSystem flockSystem() {
        return new ParticleSystem(
                Shapes.sphere(3f), new BurstSpawner(40), 50, 1f, VelocitySpec.RADIAL,
                Curve.of(1f, 0f, Easings.LINEAR),
                Curve.of(1f, 0f, Easings.LINEAR),
                ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR),
                List.of(new FlockSpec(4f, 1f, 1f, 1f, 2f).toModifier()),
                RotationSpec.NONE, CollisionSpec.NONE, null, false, false, false, TrailSpec.NONE, new Random(9));
    }

    @Test
    void deterministicWithSeed() {
        ParticleSystem a = flockSystem();
        ParticleSystem b = flockSystem();
        for (int t = 0; t < 8; t++) {
            a.tick();
            b.tick();
        }
        assertEquals(a.particles().size(), b.particles().size());
        for (int i = 0; i < a.particles().size(); i++) {
            assertEquals(a.particles().get(i).pos, b.particles().get(i).pos);
            assertEquals(a.particles().get(i).vel, b.particles().get(i).vel);
        }
    }

    @Test
    void cohesionPullsTowardNeighbor() {
        Particle a = new Particle(Vec3f.ZERO, Vec3f.ZERO, 10);
        Particle b = new Particle(new Vec3f(4f, 0f, 0f), Vec3f.ZERO, 10);
        SpatialHash hash = new SpatialHash(10f);
        hash.add(a.pos, a.vel);
        hash.add(b.pos, b.vel);
        FlockModifier flock = new FlockModifier(new FlockSpec(10f, 0f, 0f, 1f, 5f));
        flock.steer(a, hash);
        assertTrue(a.vel.x() > 0f, "cohesion should steer toward the neighbor");
    }

    @Test
    void separationPushesOffCloseNeighbor() {
        Particle a = new Particle(Vec3f.ZERO, Vec3f.ZERO, 10);
        Particle b = new Particle(new Vec3f(0.5f, 0f, 0f), Vec3f.ZERO, 10);
        SpatialHash hash = new SpatialHash(10f);
        hash.add(a.pos, a.vel);
        hash.add(b.pos, b.vel);
        FlockModifier flock = new FlockModifier(new FlockSpec(10f, 1f, 0f, 0f, 5f));
        flock.steer(a, hash);
        assertTrue(a.vel.x() < 0f, "separation should push away from the close neighbor");
    }
}
