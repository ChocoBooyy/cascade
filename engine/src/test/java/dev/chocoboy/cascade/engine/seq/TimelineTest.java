package dev.chocoboy.cascade.engine.seq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.tween.Easings;
import org.junit.jupiter.api.Test;

class TimelineTest {

    @Test
    void runsToCompletion() {
        StringBuilder log = new StringBuilder();
        Timeline timeline = new Timeline(Steps.sequence(
                Steps.run(() -> log.append('a')),
                Steps.delay(2),
                Steps.run(() -> log.append('b'))));

        assertFalse(timeline.tick());
        assertTrue(timeline.tick());
        assertTrue(timeline.isDone());
        assertEquals("ab", log.toString());
    }

    @Test
    void tickAfterDoneIsHarmless() {
        Timeline timeline = new Timeline(Steps.run(() -> {}));
        assertTrue(timeline.tick());
        assertTrue(timeline.tick());
    }

    @Test
    void parallelTweensAdvanceTogether() {
        float[] a = new float[1];
        float[] b = new float[1];
        Timeline timeline = new Timeline(Steps.parallel(
                Steps.tween(0f, 4f, 2, Easings.LINEAR, v -> a[0] = v),
                Steps.tween(0f, 8f, 2, Easings.LINEAR, v -> b[0] = v)));

        timeline.tick();
        assertEquals(2f, a[0], 1e-5f);
        assertEquals(4f, b[0], 1e-5f);

        timeline.tick();
        assertEquals(4f, a[0], 1e-5f);
        assertEquals(8f, b[0], 1e-5f);
        assertTrue(timeline.isDone());
    }
}
