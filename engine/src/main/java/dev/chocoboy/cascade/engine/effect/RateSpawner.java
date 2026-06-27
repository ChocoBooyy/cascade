package dev.chocoboy.cascade.engine.effect;

public final class RateSpawner implements Spawner {

    private final float rate;
    private final int duration;
    private float carry;

    // rate is particles per tick; fractional rates accumulate so emission below one per tick still works
    public RateSpawner(float rate, int duration) {
        this.rate = Math.max(0f, rate);
        this.duration = duration;
    }

    @Override
    public int spawnCount(int tick) {
        if (tick >= duration) {
            return 0;
        }
        carry += rate;
        int n = (int) carry;
        carry -= n;
        return n;
    }

    @Override
    public boolean exhausted(int tick) {
        return tick >= duration;
    }
}
