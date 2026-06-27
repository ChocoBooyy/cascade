package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class GravityModifier implements ParticleModifier {

    private final Vec3f accel;

    public GravityModifier(Vec3f accel) {
        this.accel = accel;
    }

    @Override
    public void apply(Particle particle) {
        particle.vel = particle.vel.add(accel);
    }
}
