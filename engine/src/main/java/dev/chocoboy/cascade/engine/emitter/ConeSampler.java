package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

public final class ConeSampler implements ShapeSampler {

    private final float radius;
    private final float height;

    public ConeSampler(float radius, float height) {
        this.radius = radius;
        this.height = height;
    }

    @Override
    public Vec3f sample(RandomGenerator rng) {
        float t = rng.nextFloat();
        float y = t * height;
        float rr = radius * t * (float) Math.sqrt(rng.nextDouble());
        double a = rng.nextDouble() * 2.0 * Math.PI;
        return new Vec3f(rr * (float) Math.cos(a), y, rr * (float) Math.sin(a));
    }
}
