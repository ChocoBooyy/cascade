package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.ShapeSampler;
import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

// spawn-state sampling for burst emitters, for renderers that keep particle state somewhere other than
// a ParticleSystem (a gpu buffer) and only need the launch data. draws from the rng in exactly the order
// ParticleSystem.spawn does, so a given seed produces the same launch state on either path; the
// equivalence is pinned by BurstSamplerTest
public final class BurstSampler {

    public interface Sink {
        void accept(float px, float py, float pz, float vx, float vy, float vz, float rotation, float spin);
    }

    private BurstSampler() {
    }

    public static void sample(EmitterSpec spec, RandomGenerator rng, int count, Sink sink) {
        ShapeSampler shape = spec.shape().sampler();
        for (int i = 0; i < count; i++) {
            Vec3f offset = shape.sample(rng);
            Vec3f vel = initialVelocity(spec, offset, rng);
            float rotation = spec.rotation().angleRange() * rng.nextFloat();
            float spin = (rng.nextFloat() * 2f - 1f) * spec.rotation().spinRange();
            sink.accept(offset.x(), offset.y(), offset.z(), vel.x(), vel.y(), vel.z(), rotation, spin);
        }
    }

    private static Vec3f initialVelocity(EmitterSpec spec, Vec3f offset, RandomGenerator rng) {
        float speed = spec.speed();
        return switch (spec.velocity().mode()) {
            case RADIAL -> offset.normalize().scale(speed);
            case INWARD -> offset.normalize().scale(-speed);
            case ORBITAL -> {
                Vec3f tangent = new Vec3f(-offset.z(), 0f, offset.x());
                yield tangent.length() < 1e-6f ? Vec3f.ZERO : tangent.normalize().scale(speed);
            }
            case DIRECTIONAL -> directionalVelocity(spec, rng);
        };
    }

    private static Vec3f directionalVelocity(EmitterSpec spec, RandomGenerator rng) {
        VelocitySpec velocity = spec.velocity();
        float speed = spec.speed();
        Vec3f dir = velocity.direction().length() < 1e-6f ? new Vec3f(0f, 1f, 0f) : velocity.direction().normalize();
        float spread = velocity.spread();
        if (spread <= 0f) {
            return dir.scale(speed);
        }
        float cosMax = (float) Math.cos(spread);
        float cosTheta = cosMax + (1f - cosMax) * rng.nextFloat();
        float sinTheta = (float) Math.sqrt(Math.max(0f, 1f - cosTheta * cosTheta));
        float phi = (float) (rng.nextFloat() * 2.0 * Math.PI);
        Vec3f helper = Math.abs(dir.y()) < 0.99f ? new Vec3f(0f, 1f, 0f) : new Vec3f(1f, 0f, 0f);
        Vec3f t1 = dir.cross(helper).normalize();
        Vec3f t2 = dir.cross(t1);
        Vec3f spreadComp = t1.scale(sinTheta * (float) Math.cos(phi)).add(t2.scale(sinTheta * (float) Math.sin(phi)));
        return dir.scale(cosTheta).add(spreadComp).scale(speed);
    }
}
