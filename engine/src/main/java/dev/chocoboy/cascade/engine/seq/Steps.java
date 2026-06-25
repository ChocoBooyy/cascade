package dev.chocoboy.cascade.engine.seq;

import dev.chocoboy.cascade.engine.tween.Easing;
import dev.chocoboy.cascade.engine.tween.FloatConsumer;
import java.util.List;

public final class Steps {

    private Steps() {
    }

    public static Step delay(int ticks) {
        return new DelayStep(ticks);
    }

    public static Step run(Runnable action) {
        return new RunStep(action);
    }

    public static Step tween(float from, float to, int ticks, Easing easing, FloatConsumer setter) {
        return new TweenStep(from, to, ticks, easing, setter);
    }

    public static Step sequence(Step... steps) {
        return new SequentialStep(List.of(steps));
    }

    public static Step parallel(Step... steps) {
        return new ParallelStep(List.of(steps));
    }
}
