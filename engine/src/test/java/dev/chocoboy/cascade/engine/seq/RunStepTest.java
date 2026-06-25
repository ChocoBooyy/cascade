package dev.chocoboy.cascade.engine.seq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RunStepTest {

    @Test
    void firesOnceThenIsDone() {
        AtomicInteger calls = new AtomicInteger();
        RunStep step = new RunStep(calls::incrementAndGet);

        assertTrue(step.tick());
        assertTrue(step.isDone());
        assertEquals(1, calls.get());

        step.tick();
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotConsumeTick() {
        assertFalse(new RunStep(() -> {}).consumesTick());
    }
}
