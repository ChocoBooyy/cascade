package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

public final class PointSampler implements ShapeSampler {

    @Override
    public Vec3f sample(RandomGenerator rng) {
        return Vec3f.ZERO;
    }
}
