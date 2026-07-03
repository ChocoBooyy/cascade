package dev.chocoboy.cascade.engine.effect;

/**
 * A per-particle update step. Runs once per particle per tick before position is integrated, so modifiers
 * typically adjust velocity. Compose several to build organic motion.
 */
public interface ParticleModifier {

    /** Applies one tick of this behavior to {@code particle}, in place. */
    void apply(Particle particle);
}
