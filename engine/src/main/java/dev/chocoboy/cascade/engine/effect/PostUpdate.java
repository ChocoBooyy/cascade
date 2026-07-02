package dev.chocoboy.cascade.engine.effect;

// Optional second pass a ParticleModifier may also implement. Post-update runs after position is
// integrated, for behaviors that react to the moved particle rather than just its velocity.
public interface PostUpdate {

    void postUpdate(Particle particle);
}
