package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Noise;
import dev.chocoboy.cascade.engine.math.Vec3f;

public final class CurlModifier implements ParticleModifier {

    // offset each potential component into a distinct field region so its three parts decorrelate
    private static final float OX = 31.4f;
    private static final float OY = 58.6f;
    private static final float OZ = 17.1f;
    private static final float OX2 = 113.7f;
    private static final float OY2 = 271.9f;
    private static final float OZ2 = 421.3f;

    private static final float E = 1e-3f;

    private final float strength;
    private final float frequency;

    public CurlModifier(float strength, float frequency) {
        this.strength = strength;
        this.frequency = frequency;
    }

    @Override
    public void apply(Particle particle) {
        float sx = particle.pos.x() * frequency;
        float sy = particle.pos.y() * frequency;
        float sz = particle.pos.z() * frequency;

        float curlX = ((pz(sx, sy + E, sz) - pz(sx, sy - E, sz))
            - (py(sx, sy, sz + E) - py(sx, sy, sz - E))) / (2f * E);
        float curlY = ((px(sx, sy, sz + E) - px(sx, sy, sz - E))
            - (pz(sx + E, sy, sz) - pz(sx - E, sy, sz))) / (2f * E);
        float curlZ = ((py(sx + E, sy, sz) - py(sx - E, sy, sz))
            - (px(sx, sy + E, sz) - px(sx, sy - E, sz))) / (2f * E);

        particle.vel = particle.vel.add(new Vec3f(curlX, curlY, curlZ).scale(strength));
    }

    private static float px(float x, float y, float z) {
        return Noise.value(x, y, z);
    }

    private static float py(float x, float y, float z) {
        return Noise.value(x + OX, y + OY, z + OZ);
    }

    private static float pz(float x, float y, float z) {
        return Noise.value(x + OX2, y + OY2, z + OZ2);
    }
}
