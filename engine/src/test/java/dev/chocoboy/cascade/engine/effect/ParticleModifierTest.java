package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.math.Vec3f;
import org.junit.jupiter.api.Test;

class ParticleModifierTest {

    @Test
    void gravityAddsAccelerationEachTick() {
        Particle p = new Particle(Vec3f.ZERO, Vec3f.ZERO, 10);
        GravityModifier g = new GravityModifier(new Vec3f(0f, -0.1f, 0f));
        g.apply(p);
        g.apply(p);
        assertEquals(-0.2f, p.vel.y(), 1e-6f);
        assertEquals(0f, p.vel.x(), 1e-6f);
    }

    @Test
    void dragShedsFractionOfVelocity() {
        Particle p = new Particle(Vec3f.ZERO, new Vec3f(10f, 0f, 0f), 10);
        new DragModifier(0.1f).apply(p);
        assertEquals(9f, p.vel.x(), 1e-5f);
    }

    @Test
    void zeroDragLeavesVelocityUntouched() {
        Particle p = new Particle(Vec3f.ZERO, new Vec3f(5f, -2f, 1f), 10);
        new DragModifier(0f).apply(p);
        assertEquals(5f, p.vel.x(), 1e-6f);
        assertEquals(-2f, p.vel.y(), 1e-6f);
    }

    @Test
    void turbulenceIsDeterministicForSamePosition() {
        TurbulenceModifier t = new TurbulenceModifier(0.1f, 0.5f);
        Particle a = new Particle(new Vec3f(1f, 2f, 3f), Vec3f.ZERO, 10);
        Particle b = new Particle(new Vec3f(1f, 2f, 3f), Vec3f.ZERO, 10);
        t.apply(a);
        t.apply(b);
        assertEquals(a.vel.x(), b.vel.x());
        assertEquals(a.vel.y(), b.vel.y());
        assertEquals(a.vel.z(), b.vel.z());
    }

    @Test
    void turbulenceForceBoundedByStrength() {
        float s = 0.2f;
        Particle p = new Particle(new Vec3f(3f, 5f, 7f), Vec3f.ZERO, 10);
        new TurbulenceModifier(s, 0.3f).apply(p);
        assertTrue(Math.abs(p.vel.x()) <= s + 1e-6f);
        assertTrue(Math.abs(p.vel.y()) <= s + 1e-6f);
        assertTrue(Math.abs(p.vel.z()) <= s + 1e-6f);
    }

    @Test
    void attractorPullsVelocityTowardCenter() {
        Particle p = new Particle(new Vec3f(2f, 0f, 0f), Vec3f.ZERO, 10);
        new AttractorModifier(Vec3f.ZERO, 0.5f).apply(p);
        assertEquals(-0.5f, p.vel.x(), 1e-6f);
        assertEquals(0f, p.vel.y(), 1e-6f);
        assertEquals(0f, p.vel.z(), 1e-6f);
    }

    @Test
    void attractorNegativeStrengthPushesAway() {
        Particle p = new Particle(new Vec3f(2f, 0f, 0f), Vec3f.ZERO, 10);
        new AttractorModifier(Vec3f.ZERO, -0.5f).apply(p);
        assertEquals(0.5f, p.vel.x(), 1e-6f);
    }

    @Test
    void vortexAddsTangentialVelocity() {
        Particle p = new Particle(new Vec3f(2f, 0f, 0f), Vec3f.ZERO, 10);
        new VortexModifier(Vec3f.ZERO, 0.3f).apply(p);
        assertEquals(0f, p.vel.x(), 1e-6f);
        assertEquals(0.3f, p.vel.z(), 1e-6f);
    }

    @Test
    void curlIsDeterministicForSameState() {
        CurlModifier c = new CurlModifier(0.1f, 0.5f);
        Particle a = new Particle(new Vec3f(1f, 2f, 3f), Vec3f.ZERO, 10);
        Particle b = new Particle(new Vec3f(1f, 2f, 3f), Vec3f.ZERO, 10);
        c.apply(a);
        c.apply(b);
        assertEquals(a.vel.x(), b.vel.x());
        assertEquals(a.vel.y(), b.vel.y());
        assertEquals(a.vel.z(), b.vel.z());
    }

    @Test
    void curlChangesVelocityAndScalesWithStrength() {
        Particle weak = new Particle(new Vec3f(4f, 1f, 6f), Vec3f.ZERO, 10);
        Particle strong = new Particle(new Vec3f(4f, 1f, 6f), Vec3f.ZERO, 10);
        new CurlModifier(0.1f, 0.4f).apply(weak);
        new CurlModifier(0.2f, 0.4f).apply(strong);
        assertTrue(weak.vel.length() > 0f);
        assertEquals(2f * weak.vel.x(), strong.vel.x(), 1e-6f);
        assertEquals(2f * weak.vel.y(), strong.vel.y(), 1e-6f);
        assertEquals(2f * weak.vel.z(), strong.vel.z(), 1e-6f);
    }
}
