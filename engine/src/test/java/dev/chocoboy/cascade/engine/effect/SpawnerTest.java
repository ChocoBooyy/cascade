package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnerTest {

    @Test
    void burstEmitsAllAtTickZeroThenStops() {
        BurstSpawner s = new BurstSpawner(10);
        assertEquals(10, s.spawnCount(0));
        assertEquals(0, s.spawnCount(1));
        assertFalse(s.exhausted(0));
        assertTrue(s.exhausted(1));
    }

    @Test
    void rateEmitsEachTickUntilDuration() {
        RateSpawner s = new RateSpawner(2f, 3);
        assertEquals(2, s.spawnCount(0));
        assertEquals(2, s.spawnCount(1));
        assertEquals(2, s.spawnCount(2));
        assertEquals(0, s.spawnCount(3));
        assertTrue(s.exhausted(3));
    }

    @Test
    void fractionalRateAccumulatesAcrossTicks() {
        RateSpawner s = new RateSpawner(0.5f, 10);
        int total = 0;
        for (int t = 0; t < 10; t++) {
            total += s.spawnCount(t);
        }
        assertEquals(5, total);
    }

    @Test
    void burstRejectsNegativeCount() {
        try {
            new BurstSpawner(-1);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
