package dev.chocoboy.cascade.engine.seq;

import java.util.Objects;

public final class RunStep implements Step {

    private final Runnable action;
    private boolean done;

    public RunStep(Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public boolean tick() {
        if (done) {
            return true;
        }
        action.run();
        done = true;
        return true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean consumesTick() {
        return false;
    }
}
