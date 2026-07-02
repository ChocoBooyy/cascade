package dev.chocoboy.cascade.engine.effect;

public record FlockSpec(float radius, float separation, float alignment, float cohesion, float maxSpeed)
        implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new FlockModifier(this);
    }

    @Override
    public String typeId() {
        return "flock";
    }
}
