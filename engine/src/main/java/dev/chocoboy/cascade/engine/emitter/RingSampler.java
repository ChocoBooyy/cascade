package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

public final class RingSampler implements ShapeSampler {

    private final float radius;

    public RingSampler(float radius) {
        this.radius = radius;
    }

    @Override
    public Vec3f sample(RandomGenerator rng) {
        double a = rng.nextDouble() * 2.0 * Math.PI;
        return new Vec3f(radius * (float) Math.cos(a), 0f, radius * (float) Math.sin(a));
    }
}
