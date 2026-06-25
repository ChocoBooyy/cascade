package dev.chocoboy.cascade.engine.seq;

public final class DelayStep implements Step {

    private final int duration;
    private int elapsed;

    public DelayStep(int durationTicks) {
        if (durationTicks < 0) {
            throw new IllegalArgumentException("delay < 0");
        }
        this.duration = durationTicks;
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
}
