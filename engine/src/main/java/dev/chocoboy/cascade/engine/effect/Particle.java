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

    public Particle(Vec3f pos, Vec3f vel, int lifetime) {
        this.pos = pos;
        this.vel = vel;
        this.lifetime = lifetime;
    }

    public boolean dead() {
        return age >= lifetime;
    }

    public float life() {
        return lifetime <= 0 ? 1f : (float) age / lifetime;
    }
}
