package dev.chocoboy.cascade.engine.seq;

import dev.chocoboy.cascade.engine.tween.Curve;
import dev.chocoboy.cascade.engine.tween.Easing;
import dev.chocoboy.cascade.engine.tween.FloatConsumer;
import java.util.Objects;

public final class TweenStep implements Step {

    private final Curve curve;
    private final int duration;
    private final FloatConsumer setter;
    private int elapsed;

    public TweenStep(float from, float to, int durationTicks, Easing easing, FloatConsumer setter) {
        if (durationTicks < 1) {
            throw new IllegalArgumentException("tween duration < 1");
        }
        this.curve = Curve.of(from, to, easing);
        this.duration = durationTicks;
        this.setter = Objects.requireNonNull(setter, "setter");
    }

    @Override
    public boolean tick() {
        if (elapsed >= duration) {
            return true;
        }
        elapsed++;
        setter.accept(curve.at((float) elapsed / duration));
        return elapsed >= duration;
    }

    @Override
    public boolean isDone() {
        return elapsed >= duration;
    }
}
