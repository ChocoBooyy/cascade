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

class CollisionTest {

    private static final float EPS = 1e-4f;

    // a solid floor filling everything below y=0
    private static final CollisionProbe FLOOR = (x, y, z) -> y < 0f;

    private static ParticleSystem withCollision(Particle seed, CollisionSpec collision, CollisionProbe probe) {
        return withCollision(seed, collision, probe, List.of(), false);
    }

    private static ParticleSystem withCollision(Particle seed, CollisionSpec collision, CollisionProbe probe,
            List<ParticleModifier> modifiers, boolean emitsOnCollision) {
        ParticleConfig config = ParticleConfig.burst(20, 0f,
                        Curve.of(1f, 0f, Easings.LINEAR),
                        Curve.of(1f, 0f, Easings.LINEAR),
                        ColorCurve.of(0xFFFFFF, 0x000000, Easings.LINEAR))
                .withModifiers(modifiers)
                .withCollision(collision)
                .withEmitsOnCollision(emitsOnCollision);
        ParticleSystem s = new ParticleSystem(Shapes.point(), new BurstSpawner(0), config, probe, new Random(1));
        s.particles().add(seed);
        return s;
    }

    @Test
    void floorReflectsDownwardVelocityAndKeepsParticleAboveFloor() {
        Particle p = new Particle(new Vec3f(0f, 0.05f, 0f), new Vec3f(0f, -0.1f, 0f), 20);
        ParticleSystem s = withCollision(p, CollisionSpec.bouncy(0.5f, 0f), FLOOR);
        s.tick();
        assertTrue(p.vel.y() > 0f, "downward velocity should reflect upward");
        assertEquals(0.05f, p.vel.y(), EPS);
        assertTrue(p.pos.y() >= 0f, "particle should not end below the floor");
    }

    @Test
    void disabledCollisionLeavesMotionUnchanged() {
        Particle p = new Particle(new Vec3f(0f, 0.05f, 0f), new Vec3f(0f, -0.1f, 0f), 20);
        ParticleSystem s = withCollision(p, CollisionSpec.NONE, FLOOR);
        s.tick();
        assertEquals(-0.1f, p.vel.y(), EPS);
        assertEquals(-0.05f, p.pos.y(), EPS);
    }

    @Test
    void frictionReducesHorizontalSpeedOnFloorContact() {
        // sitting on the floor, sliding sideways while pressing down into it
        Particle p = new Particle(new Vec3f(0f, 0.05f, 0f), new Vec3f(1f, -0.1f, 0f), 20);
        ParticleSystem s = withCollision(p, CollisionSpec.bouncy(0f, 0.25f), FLOOR);
        s.tick();
        assertEquals(0.75f, p.vel.x(), EPS);
    }

    @Test
    void frictionDampsOnlyTheTangentialComponents() {
        // the hit axis keeps its full bounce reflection; friction touches the sliding axes only
        Particle p = new Particle(new Vec3f(0f, 0.05f, 0f), new Vec3f(1f, -0.1f, 0.4f), 20);
        ParticleSystem s = withCollision(p, CollisionSpec.bouncy(0.5f, 0.25f), FLOOR);
        s.tick();
        assertEquals(0.05f, p.vel.y(), EPS);
        assertEquals(0.75f, p.vel.x(), EPS);
        assertEquals(0.3f, p.vel.z(), EPS);
    }

    @Test
    void collidedFlagFiresTheSubEmitterExactlyOnceUnderSustainedContact() {
        // gravity keeps pressing the particle into the floor every tick, so contact never ends; the
        // one-shot flag must still yield a single spawn request over the whole life
        Particle p = new Particle(new Vec3f(0f, 0.05f, 0f), new Vec3f(0f, -0.1f, 0f), 20);
        ParticleSystem s = withCollision(p, CollisionSpec.bouncy(0f, 0f), FLOOR,
                List.of(new GravitySpec(new Vec3f(0f, -0.05f, 0f)).toModifier()), true);
        int requests = 0;
        for (int t = 0; t < 10; t++) {
            s.tick();
            requests += s.drainSpawnRequests().size();
        }
        assertEquals(1, requests);
        assertTrue(p.collided);
    }
}
