package dev.chocoboy.cascade.client;

import dev.chocoboy.cascade.engine.effect.MeshId;
import org.joml.Vector3f;

// a unit cube centered on the origin, half extent 1, as six quads (24 vertices) wound for outward faces.
// SHARD reuses this under an elongating scale, so there is one mesh table and two looks.
final class MeshGeometry {

    private MeshGeometry() {
    }

    // 6 faces, 4 corners each, in QUADS order
    static final float[][] CUBE_FACES = {
            // +Y top
            {-1,  1, -1,  -1,  1,  1,   1,  1,  1,   1,  1, -1},
            // -Y bottom
            {-1, -1,  1,  -1, -1, -1,   1, -1, -1,   1, -1,  1},
            // +X east
            { 1, -1, -1,   1,  1, -1,   1,  1,  1,   1, -1,  1},
            // -X west
            {-1, -1,  1,  -1,  1,  1,  -1,  1, -1,  -1, -1, -1},
            // +Z south
            {-1, -1,  1,   1, -1,  1,   1,  1,  1,  -1,  1,  1},
            // -Z north
            { 1, -1, -1,  -1, -1, -1,  -1,  1, -1,   1,  1, -1}
    };

    private static final Vector3f CUBE_SCALE = new Vector3f(1f, 1f, 1f);
    private static final Vector3f SHARD_SCALE = new Vector3f(0.45f, 1.8f, 0.45f);

    // per mesh local scale applied before the orientation matrix. CUBE is uniform, SHARD a tall splinter.
    // returns a shared constant, so callers must read it not mutate it
    static Vector3f scaleFor(MeshId mesh) {
        return mesh == MeshId.SHARD ? SHARD_SCALE : CUBE_SCALE;
    }
}
