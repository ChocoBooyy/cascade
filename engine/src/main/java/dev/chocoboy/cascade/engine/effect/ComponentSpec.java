package dev.chocoboy.cascade.engine.effect;

/**
 * Serializable description of one particle behavior. Open so third parties can register their own kinds
 * alongside the built-ins; each spec builds its runtime {@link ParticleModifier} and names its wire type.
 *
 * <p>Register an implementation with {@code Vfx.registerComponent} to make it usable from both the fluent
 * builder and effect JSON.
 */
public interface ComponentSpec {

    /** Builds the runtime modifier this spec describes. Called once when a particle system is created. */
    ParticleModifier toModifier();

    /** The stable id tagging this component on the wire and in JSON, e.g. {@code "mymod:swirl"}. */
    String typeId();
}
