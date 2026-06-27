package dev.chocoboy.cascade.engine.effect;

public final class BurstSpawner implements Spawner {

    private final int count;

    public BurstSpawner(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = count;
    }

    @Override
    public int spawnCount(int tick) {
        return tick == 0 ? count : 0;
    }

    @Override
    public boolean exhausted(int tick) {
        return tick > 0;
    }
}
