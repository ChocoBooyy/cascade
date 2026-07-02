package dev.chocoboy.cascade.engine.effect;

// Serializable description of one particle behavior. Open so third parties can register their own
// kinds alongside the built-ins; each spec knows how to build its runtime modifier and name its type.
public interface ComponentSpec {

    ParticleModifier toModifier();

    String typeId();
}
