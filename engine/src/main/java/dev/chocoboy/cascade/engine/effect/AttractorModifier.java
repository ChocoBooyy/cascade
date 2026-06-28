package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class AttractorModifier implements ParticleModifier {

    private final Vec3f center;
    private final float strength;

    public AttractorModifier(Vec3f center, float strength) {
        this.center = center;
        this.strength = strength;
    }

    @Override
    public void apply(Particle particle) {
        Vec3f d = new Vec3f(
            center.x() - particle.pos.x(),
            center.y() - particle.pos.y(),
            center.z() - particle.pos.z());
        float dist = d.length();
        if (dist < 1e-4f) {
            return;
        }
        particle.vel = particle.vel.add(d.scale(strength / dist));
    }
}
