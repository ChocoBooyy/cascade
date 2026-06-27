package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.random.RandomGenerator;

public record BeamSpec(int color, float width, float arc, int duration, int segments) {

    public BeamState build(Vec3f from, Vec3f to, RandomGenerator rng) {
        return new BeamState(from, to, duration, arc, segments, rng);
    }

    public static BeamSpec defaultBolt() {
        return new BeamSpec(0x78C8FF, 0.12f, 0.35f, 16, 12);
    }
}
