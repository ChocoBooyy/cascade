package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Uniform-grid neighbor index over a snapshot of particle pos and vel, rebuilt each tick. Cells are
// keyed by floor(pos / cellSize) packed into a long. Vec3f is immutable, so keeping the reference is
// the snapshot. Index-free on purpose: the querying particle shows up in its own neighborhood and the
// FlockModifier accounts for that rather than tracking indices to skip self.
public final class SpatialHash {

    public interface NeighborConsumer {
        void accept(Vec3f pos, Vec3f vel);
    }

    private record Entry(Vec3f pos, Vec3f vel) {
    }

    private final float invCell;
    private final Map<Long, List<Entry>> cells = new HashMap<>();

    public SpatialHash(float cellSize) {
        this.invCell = 1f / cellSize;
    }

    public void add(Vec3f pos, Vec3f vel) {
        long key = key(cell(pos.x()), cell(pos.y()), cell(pos.z()));
        cells.computeIfAbsent(key, k -> new ArrayList<>()).add(new Entry(pos, vel));
    }

    // visit the 3x3x3 cell block around center in a fixed order, entries in insertion order, calling the
    // consumer for each within radius and stopping after cap accepted. requires radius <= cellSize, which
    // the caller guarantees by sizing cells to the max flock radius. cap bounds per-particle cost.
    public void forEachNeighbor(Vec3f center, float radius, int cap, NeighborConsumer consumer) {
        int cx = cell(center.x());
        int cy = cell(center.y());
        int cz = cell(center.z());
        float r2 = radius * radius;
        int accepted = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    List<Entry> bucket = cells.get(key(cx + dx, cy + dy, cz + dz));
                    if (bucket == null) {
                        continue;
                    }
                    for (int i = 0; i < bucket.size(); i++) {
                        Entry e = bucket.get(i);
                        if (e.pos.sub(center).lengthSq() <= r2) {
                            consumer.accept(e.pos, e.vel);
                            if (++accepted >= cap) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private int cell(float v) {
        return (int) Math.floor(v * invCell);
    }

    // pack three cell coords into one long, 21 bits each, so negative coords key cleanly
    private static long key(int x, int y, int z) {
        return ((x & 0x1FFFFFL) << 42) | ((y & 0x1FFFFFL) << 21) | (z & 0x1FFFFFL);
    }
}
