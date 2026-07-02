package dev.chocoboy.cascade.engine.effect;

public record TurbulenceSpec(float strength, float frequency) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new TurbulenceModifier(strength, frequency);
    }

    @Override
    public String typeId() {
        return "turbulence";
    }
}
