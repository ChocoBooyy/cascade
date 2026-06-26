package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class BeamState implements EffectSim {

    private final Vec3f from;
    private final Vec3f to;
    private final int duration;
    private final List<Vec3f> spine;
    private int elapsed;

    public BeamState(Vec3f from, Vec3f to, int duration, float arc, int segments, RandomGenerator rng) {
        if (duration < 1) {
            throw new IllegalArgumentException("duration < 1");
        }
        if (segments < 1) {
            throw new IllegalArgumentException("segments < 1");
        }
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.duration = duration;
        Objects.requireNonNull(rng, "rng");
        this.spine = buildSpine(from, to, arc, segments, rng);
    }

    private static List<Vec3f> buildSpine(Vec3f from, Vec3f to, float arc, int segments, RandomGenerator rng) {
        List<Vec3f> points = new ArrayList<>(segments + 1);
        Vec3f delta = to.add(from.scale(-1f));
        for (int i = 0; i <= segments; i++) {
            Vec3f base = from.add(delta.scale((float) i / segments));
            if (i == 0 || i == segments || arc == 0f) {
                points.add(base);
            } else {
                points.add(base.add(new Vec3f(
                        (rng.nextFloat() * 2f - 1f) * arc,
                        (rng.nextFloat() * 2f - 1f) * arc,
                        (rng.nextFloat() * 2f - 1f) * arc)));
            }
        }
        return points;
    }

    @Override
    public boolean tick() {
        if (elapsed >= duration) {
            return true;
        }
        elapsed++;
        return elapsed >= duration;
    }

    @Override
    public boolean isDone() {
        return elapsed >= duration;
    }

    public float progress() {
        return (float) elapsed / duration;
    }

    public List<Vec3f> spine() {
        return spine;
    }

    public Vec3f from() {
        return from;
    }

    public Vec3f to() {
        return to;
    }
}
