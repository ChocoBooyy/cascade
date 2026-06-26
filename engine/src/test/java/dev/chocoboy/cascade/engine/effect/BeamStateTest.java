package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BeamStateTest {

    private static final float EPS = 1e-4f;
    private static final Vec3f A = new Vec3f(0f, 0f, 0f);
    private static final Vec3f B = new Vec3f(10f, 0f, 0f);

    @Test
    void rejectsBadArgs() {
        assertThrows(IllegalArgumentException.class, () -> new BeamState(A, B, 0, 0f, 4, new Random(1)));
        assertThrows(IllegalArgumentException.class, () -> new BeamState(A, B, 5, 0f, 0, new Random(1)));
    }

    @Test
    void spineHasSegmentPlusOnePointsWithExactEndpoints() {
        BeamState beam = new BeamState(A, B, 5, 0.5f, 4, new Random(1));
        List<Vec3f> spine = beam.spine();
        assertEquals(5, spine.size());
        assertEquals(A, spine.get(0));
        assertEquals(B, spine.get(4));
    }

    @Test
    void arcZeroIsStraight() {
        BeamState beam = new BeamState(A, B, 5, 0f, 4, new Random(1));
        List<Vec3f> spine = beam.spine();
        for (int i = 0; i < spine.size(); i++) {
            assertEquals(2.5f * i, spine.get(i).x(), EPS);
            assertEquals(0f, spine.get(i).y(), EPS);
            assertEquals(0f, spine.get(i).z(), EPS);
        }
    }

    @Test
    void interiorPointsStayWithinArc() {
        float arc = 0.5f;
        BeamState beam = new BeamState(A, B, 5, arc, 8, new Random(3));
        List<Vec3f> spine = beam.spine();
        for (int i = 1; i < spine.size() - 1; i++) {
            float baseX = (10f / 8f) * i;
            Vec3f p = spine.get(i);
            assertTrue(Math.abs(p.x() - baseX) <= arc + EPS, "x");
            assertTrue(Math.abs(p.y()) <= arc + EPS, "y");
            assertTrue(Math.abs(p.z()) <= arc + EPS, "z");
        }
    }

    @Test
    void progressAdvancesToDone() {
        BeamState beam = new BeamState(A, B, 3, 0f, 4, new Random(1));
        assertEquals(0f, beam.progress(), EPS);
        assertFalse(beam.tick());
        assertFalse(beam.tick());
        assertTrue(beam.tick());
        assertEquals(1f, beam.progress(), EPS);
        assertTrue(beam.isDone());
    }

    @Test
    void deterministicSpine() {
        assertEquals(
                new BeamState(A, B, 5, 0.5f, 8, new Random(9)).spine(),
                new BeamState(A, B, 5, 0.5f, 8, new Random(9)).spine());
    }
}
