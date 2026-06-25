package dev.chocoboy.cascade.engine.seq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TweenStepTest {

    @Test
    void drivesSetterToFinalValue() {
        List<Float> seen = new ArrayList<>();
        TweenStep step = new TweenStep(0f, 10f, 2, Easings.LINEAR, seen::add);

        assertEquals(false, step.tick());
        assertEquals(true, step.tick());
        assertTrue(step.isDone());

        assertEquals(2, seen.size());
        assertEquals(5f, seen.get(0), 1e-5f);
        assertEquals(10f, seen.get(1), 1e-5f);
    }

    @Test
    void rejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new TweenStep(0f, 1f, 0, Easings.LINEAR, v -> {}));
    }
}
