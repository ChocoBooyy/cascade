package dev.chocoboy.cascade.engine.effect;

// Serializable emission settings. Burst ignores rate and duration; rate emission uses both.
public record EmissionSpec(Mode mode, float rate, int duration) {

    public enum Mode {
        BURST, RATE
    }

    public static EmissionSpec burst() {
        return new EmissionSpec(Mode.BURST, 0f, 0);
    }

    public static EmissionSpec rate(float perTick, int duration) {
        return new EmissionSpec(Mode.RATE, perTick, duration);
    }

    public Spawner spawner(int burstCount) {
        return mode == Mode.RATE ? new RateSpawner(rate, duration) : new BurstSpawner(burstCount);
    }
}
