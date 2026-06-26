package dev.chocoboy.cascade.engine.emitter;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

@FunctionalInterface
public interface ShapeSampler {

    Vec3f sample(RandomGenerator rng);
}
