package dev.chocoboy.cascade.engine.effect;

// world seam for collision. coordinates are emitter-local, the implementation adds the world origin.
public interface CollisionProbe {

    boolean solid(float x, float y, float z);
}
