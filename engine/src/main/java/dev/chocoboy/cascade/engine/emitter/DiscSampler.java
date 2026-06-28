package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

public final class DiscSampler implements ShapeSampler {

    private final float radius;

    public DiscSampler(float radius) {
        this.radius = radius;
    }

    @Override
    public Vec3f sample(RandomGenerator rng) {
        // sqrt keeps the fill uniform by area rather than crowding the center
        float r = radius * (float) Math.sqrt(rng.nextFloat());
        float theta = (float) (rng.nextFloat() * Math.PI * 2.0);
        return new Vec3f(r * (float) Math.cos(theta), 0f, r * (float) Math.sin(theta));
    }
}
