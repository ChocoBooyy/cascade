package dev.chocoboy.cascade.engine.effect;

/**
 * Optional second pass a {@link ParticleModifier} may also implement. Runs after position is integrated, for
 * behaviors that react to the moved particle rather than just its velocity.
 */
public interface PostUpdate {

    /** Called after {@code particle} has been moved for the tick. */
    void postUpdate(Particle particle);
}
