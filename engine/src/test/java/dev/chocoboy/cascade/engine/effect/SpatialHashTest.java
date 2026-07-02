package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpatialHashTest {

    private static SpatialHash build(List<Vec3f> points) {
        SpatialHash hash = new SpatialHash(2f);
        for (Vec3f p : points) {
            hash.add(p, Vec3f.ZERO);
        }
        return hash;
    }

    private static List<Vec3f> collect(SpatialHash hash, Vec3f center, float radius, int cap) {
        List<Vec3f> hits = new ArrayList<>();
        hash.forEachNeighbor(center, radius, cap, (pos, vel) -> hits.add(pos));
        return hits;
    }

    @Test
    void returnsOnlyPointsWithinRadius() {
        List<Vec3f> points = List.of(
                new Vec3f(0f, 0f, 0f),
                new Vec3f(1f, 0f, 0f),
                new Vec3f(0f, 1.5f, 0f),
                new Vec3f(3f, 0f, 0f));
        List<Vec3f> hits = collect(build(points), Vec3f.ZERO, 2f, 64);
        assertEquals(List.of(points.get(0), points.get(1), points.get(2)), hits);
    }

    @Test
    void iterationOrderIsDeterministic() {
        List<Vec3f> points = List.of(
                new Vec3f(0.2f, 0f, 0f),
                new Vec3f(-0.3f, 0.1f, 0f),
                new Vec3f(0.1f, -0.2f, 0.4f),
                new Vec3f(0f, 0.5f, -0.1f));
        List<Vec3f> first = collect(build(points), Vec3f.ZERO, 2f, 64);
        List<Vec3f> second = collect(build(points), Vec3f.ZERO, 2f, 64);
        assertEquals(first, second);
    }

    @Test
    void capBoundsAcceptedCount() {
        List<Vec3f> points = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            points.add(new Vec3f(i * 0.1f, 0f, 0f));
        }
        List<Vec3f> hits = collect(build(points), Vec3f.ZERO, 2f, 4);
        assertEquals(4, hits.size());
        assertEquals(points.subList(0, 4), hits);
    }
}
