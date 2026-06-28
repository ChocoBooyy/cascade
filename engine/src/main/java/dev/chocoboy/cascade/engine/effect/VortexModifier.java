package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class VortexModifier implements ParticleModifier {

    private final Vec3f center;
    private final float strength;

    public VortexModifier(Vec3f center, float strength) {
        this.center = center;
        this.strength = strength;
    }

    @Override
    public void apply(Particle particle) {
        float rx = particle.pos.x() - center.x();
        float rz = particle.pos.z() - center.z();
        float len = (float) Math.sqrt(rx * rx + rz * rz);
        if (len < 1e-4f) {
            return;
        }
        particle.vel = particle.vel.add(new Vec3f(-rz / len * strength, 0f, rx / len * strength));
    }
}
