package dev.chocoboy.cascade.testmod;

import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.ParticleModifier;

public record ContainSpec(float radius) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new ContainModifier(radius);
    }

    @Override
    public String typeId() {
        return "contain";
    }
}
