package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Noise;
import dev.chocoboy.cascade.engine.math.Vec3f;

public final class TurbulenceModifier implements ParticleModifier {

    // sample each axis from a different region of the field so the force vector is not self-correlated
    private static final float OX = 31.4f;
    private static final float OY = 58.6f;
    private static final float OZ = 17.1f;

    private final float strength;
    private final float frequency;

    public TurbulenceModifier(float strength, float frequency) {
        this.strength = strength;
        this.frequency = frequency;
    }

    @Override
    public void apply(Particle particle) {
        float x = particle.pos.x() * frequency;
        float y = particle.pos.y() * frequency;
        float z = particle.pos.z() * frequency;
        float fx = Noise.value(x, y, z);
        float fy = Noise.value(x + OX, y + OY, z + OZ);
        float fz = Noise.value(x + OZ, y + OX, z + OY);
        particle.vel = particle.vel.add(new Vec3f(fx, fy, fz).scale(strength));
    }
}
