package dev.chocoboy.cascade.engine.seq;

public interface Step {

    // advance by up to one tick; return true once the step has finished and will do no more work
    boolean tick();

    boolean isDone();

    // false for instantaneous steps so a sequence can fire them without burning a tick
    default boolean consumesTick() {
        return true;
    }
}
