package dev.chocoboy.cascade.engine.effect;

/**
 * Optional pass for behaviors that steer by nearby particles, implemented alongside {@link ParticleModifier}.
 * Runs in the pre-update phase against a start-of-tick snapshot of the swarm, so every particle sees the same
 * neighborhood regardless of iteration order.
 */
public interface NeighborAware {

    /** Radius, in blocks, within which neighbors are considered. Sizes the spatial grid. */
    float queryRadius();

    /** Steers {@code particle} using the neighbors reachable through {@code neighbors}. */
    void steer(Particle particle, SpatialHash neighbors);
}
