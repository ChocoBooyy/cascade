package dev.chocoboy.cascade.engine.effect;

// names the child system spawned for a parent particle, and what fires it
public record SubEmitterSpec(EmitterSpec child, Trigger trigger) {

    public enum Trigger {
        DEATH,
        COLLISION
    }

    public SubEmitterSpec(EmitterSpec child) {
        this(child, Trigger.DEATH);
    }
}
