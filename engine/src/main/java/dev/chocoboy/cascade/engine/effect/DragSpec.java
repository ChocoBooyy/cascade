package dev.chocoboy.cascade.engine.effect;

public record DragSpec(float drag) implements ComponentSpec {

    @Override
    public ParticleModifier toModifier() {
        return new DragModifier(drag);
    }

    @Override
    public String typeId() {
        return "drag";
    }
}
