package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public record VortexSpec(Vec3f center, float strength) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new VortexModifier(center, strength);
    }

    @Override
    public String typeId() {
        return "vortex";
    }
}
