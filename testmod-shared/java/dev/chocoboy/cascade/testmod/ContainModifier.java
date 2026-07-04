package dev.chocoboy.cascade.testmod;

import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleModifier;
import dev.chocoboy.cascade.engine.effect.PostUpdate;
import dev.chocoboy.cascade.engine.math.Vec3f;

public final class ContainModifier implements ParticleModifier, PostUpdate {

    private final float radius;

    public ContainModifier(float radius) {
        this.radius = radius;
    }

    @Override
    public void apply(Particle particle) {
    }

    // catch particles on an invisible sphere: snap any that escape back onto the shell and drop the
    // outward part of their velocity, so they stay contained instead of tunneling through
    @Override
    public void postUpdate(Particle particle) {
        float d = particle.pos.length();
        if (d <= radius || d <= 1e-6f) {
            return;
        }
        Vec3f n = particle.pos.normalize();
        particle.pos = n.scale(radius);
        float outward = particle.vel.x() * n.x() + particle.vel.y() * n.y() + particle.vel.z() * n.z();
        if (outward > 0f) {
            particle.vel = particle.vel.add(n.scale(-outward));
        }
    }
}
