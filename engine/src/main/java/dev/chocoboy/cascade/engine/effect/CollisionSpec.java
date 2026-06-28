package dev.chocoboy.cascade.engine.effect;

// world collision settings. bounce is the fraction of speed kept after a hit (0 stops, 1 is elastic);
// friction is the fraction of the sliding speed shed on contact. Disabled means particles pass through.
public record CollisionSpec(boolean enabled, float bounce, float friction) {

    public static final CollisionSpec NONE = new CollisionSpec(false, 0f, 0f);

    public static CollisionSpec bouncy(float bounce, float friction) {
        return new CollisionSpec(true, bounce, friction);
    }
}
