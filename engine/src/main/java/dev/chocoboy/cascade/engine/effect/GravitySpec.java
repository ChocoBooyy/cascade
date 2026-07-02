package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public record GravitySpec(Vec3f accel) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new GravityModifier(accel);
    }

    @Override
    public String typeId() {
        return "gravity";
    }
}
