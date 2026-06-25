package dev.chocoboy.cascade.engine.seq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DelayStepTest {

    @Test
    void completesAfterDuration() {
        DelayStep step = new DelayStep(3);
        assertFalse(step.tick());
        assertFalse(step.tick());
        assertTrue(step.tick());
        assertTrue(step.isDone());
    }

    @Test
    void zeroDelayIsDoneImmediately() {
        DelayStep step = new DelayStep(0);
        assertTrue(step.isDone());
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> new DelayStep(-1));
    }

    @Test
    void consumesItsTick() {
        assertTrue(new DelayStep(1).consumesTick());
    }
}
