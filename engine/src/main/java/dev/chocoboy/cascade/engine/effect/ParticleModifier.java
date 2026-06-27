package dev.chocoboy.cascade.engine.effect;

// A per-particle update step. Runs once per particle per tick before position is integrated,
// so modifiers typically adjust velocity. Compose several to build organic motion.
public interface ParticleModifier {

    void apply(Particle particle);
}
