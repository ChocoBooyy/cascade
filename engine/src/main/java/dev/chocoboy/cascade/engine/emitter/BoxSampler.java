package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class BoxSampler implements ShapeSampler {

    private final Vec3f half;

    public BoxSampler(Vec3f half) {
        this.half = Objects.requireNonNull(half, "half");
    }

    @Override
    public Vec3f sample(RandomGenerator rng) {
        return new Vec3f(
                (rng.nextFloat() * 2f - 1f) * half.x(),
                (rng.nextFloat() * 2f - 1f) * half.y(),
                (rng.nextFloat() * 2f - 1f) * half.z());
    }
}
