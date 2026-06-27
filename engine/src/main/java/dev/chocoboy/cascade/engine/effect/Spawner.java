package dev.chocoboy.cascade.engine.effect;

// Decides how many particles a system emits on a given tick. Burst emits everything at tick 0;
// rate emission spreads spawns across many ticks for sustained effects like fire and smoke.
public interface Spawner {

    int spawnCount(int tick);

    // true once no further particles will ever be emitted
    boolean exhausted(int tick);
}
