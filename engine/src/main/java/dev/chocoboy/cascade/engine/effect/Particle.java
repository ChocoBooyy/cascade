package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class Particle {

    public Vec3f pos;
    public Vec3f vel;
    public int age;
    public final int lifetime;
    // billboard roll in radians and its per-tick change, so sprites can spin
    public float rotation;
    public float spin;
    public Vec3f[] trail;   // null when the system has no trail; otherwise a ring of recent positions
    public int trailHead;   // next write slot
    public int trailCount;  // how many slots are filled, up to trail.length

    public Particle(Vec3f pos, Vec3f vel, int lifetime) {
        this.pos = pos;
        this.vel = vel;
        this.lifetime = lifetime;
    }

    // oldest-first read so the renderer does not need to know the ring layout
    public Vec3f trailPoint(int i) {
        int start = trailCount < trail.length ? 0 : trailHead;
        return trail[(start + i) % trail.length];
    }

    public boolean dead() {
        return age >= lifetime;
    }

    public float life() {
        return lifetime <= 0 ? 1f : (float) age / lifetime;
    }
}
