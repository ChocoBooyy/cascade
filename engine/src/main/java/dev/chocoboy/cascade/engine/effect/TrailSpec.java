package dev.chocoboy.cascade.engine.effect;

// length is how many recent positions each particle keeps for a ribbon
public record TrailSpec(boolean enabled, int length) {

    public static final TrailSpec NONE = new TrailSpec(false, 0);

    public static TrailSpec of(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length < 1");
        }
        return new TrailSpec(true, length);
    }
}
