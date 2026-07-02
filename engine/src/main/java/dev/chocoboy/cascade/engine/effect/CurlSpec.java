package dev.chocoboy.cascade.engine.effect;

public record CurlSpec(float strength, float frequency) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new CurlModifier(strength, frequency);
    }

    @Override
    public String typeId() {
        return "curl";
    }
}
