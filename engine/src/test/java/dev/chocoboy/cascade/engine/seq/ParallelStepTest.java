package dev.chocoboy.cascade.engine.seq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParallelStepTest {

    @Test
    void finishesWhenLongestChildFinishes() {
        ParallelStep par = new ParallelStep(List.of(
                new DelayStep(1),
                new DelayStep(3)));

        assertFalse(par.tick());
        assertFalse(par.tick());
        assertTrue(par.tick());
        assertTrue(par.isDone());
    }

    @Test
    void emptyParallelIsDone() {
        assertTrue(new ParallelStep(List.of()).isDone());
    }
}
