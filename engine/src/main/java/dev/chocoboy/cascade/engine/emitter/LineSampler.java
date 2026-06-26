package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class LineSampler implements ShapeSampler {

    private final Vec3f from;
    private final Vec3f to;

    public LineSampler(Vec3f from, Vec3f to) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    @Override
    public Vec3f sample(RandomGenerator rng) {
        float t = rng.nextFloat();
        return from.add(to.add(from.scale(-1f)).scale(t));
    }
}
