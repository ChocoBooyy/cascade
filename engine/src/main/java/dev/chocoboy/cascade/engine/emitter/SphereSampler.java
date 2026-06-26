package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

public final class SphereSampler implements ShapeSampler {

    private final float radius;

    public SphereSampler(float radius) {
        this.radius = radius;
    }

    @Override
    public Vec3f sample(RandomGenerator rng) {
        double u = rng.nextDouble() * 2.0 - 1.0;
        double a = rng.nextDouble() * 2.0 * Math.PI;
        double ring = Math.sqrt(1.0 - u * u);
        return new Vec3f(
                radius * (float) (ring * Math.cos(a)),
                radius * (float) u,
                radius * (float) (ring * Math.sin(a)));
    }
}
