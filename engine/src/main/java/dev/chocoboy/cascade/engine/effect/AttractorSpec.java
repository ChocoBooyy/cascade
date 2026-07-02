package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public record AttractorSpec(Vec3f center, float strength) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new AttractorModifier(center, strength);
    }

    @Override
    public String typeId() {
        return "attractor";
    }
}
