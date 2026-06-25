package dev.chocoboy.cascade.engine.seq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SequentialStepTest {

    @Test
    void runsChildrenInOrder() {
        StringBuilder log = new StringBuilder();
        SequentialStep seq = new SequentialStep(List.of(
                new RunStep(() -> log.append('a')),
                new DelayStep(2),
                new RunStep(() -> log.append('b'))));

        // tick 1: 'a' fires, delay starts
        assertFalse(seq.tick());
        assertEquals("a", log.toString());

        // tick 2: delay completes, trailing 'b' drains in the same tick
        assertTrue(seq.tick());
        assertEquals("ab", log.toString());
        assertTrue(seq.isDone());
    }

    @Test
    void allInstantChildrenFinishInOneTick() {
        StringBuilder log = new StringBuilder();
        SequentialStep seq = new SequentialStep(List.of(
                new RunStep(() -> log.append('x')),
                new RunStep(() -> log.append('y'))));

        assertTrue(seq.tick());
        assertEquals("xy", log.toString());
    }

    @Test
    void emptySequenceIsDone() {
        assertTrue(new SequentialStep(List.of()).isDone());
    }
}
