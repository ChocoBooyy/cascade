package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class Particle {

    public Vec3f pos;
    public Vec3f vel;
    public int age;
    public int lifetime;
    // billboard roll in radians and its per-tick change, so sprites can spin
    public float rotation;
    public float spin;
    // mesh tumble: pitch and yaw with their per-tick spins. roll reuses rotation/spin above. zero for billboards
    public float pitch;
    public float yaw;
    public float pitchSpin;
    public float yawSpin;
    public Vec3f[] trail;   // null when the system has no trail; otherwise a ring of recent positions
    public int trailHead;   // next write slot
    public int trailCount;  // how many slots are filled, up to trail.length
    public boolean collided;  // set on first block contact so a collision sub-emitter fires once

    public Particle(Vec3f pos, Vec3f vel, int lifetime) {
        reset(pos, vel, lifetime);
    }

    // recycle a dead instance back to spawn state; trail array is left for the caller to reuse
    public void reset(Vec3f pos, Vec3f vel, int lifetime) {
        this.pos = pos;
        this.vel = vel;
        this.lifetime = lifetime;
        this.age = 0;
        this.rotation = 0;
        this.spin = 0;
        this.pitch = 0;
        this.yaw = 0;
        this.pitchSpin = 0;
        this.yawSpin = 0;
        this.trailHead = 0;
        this.trailCount = 0;
        this.collided = false;
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
