package dev.chocoboy.cascade.engine.effect;

public final class DragModifier implements ParticleModifier {

    private final float retain;

    // drag is the fraction of velocity shed each tick: 0 leaves motion untouched, 1 halts it
    public DragModifier(float drag) {
        this.retain = 1f - drag;
    }

    @Override
    public void apply(Particle particle) {
        particle.vel = particle.vel.scale(retain);
    }
}
