package dev.chocoboy.cascade.engine.effect;

// Optional pass for behaviors that steer by nearby particles. Runs in the pre-update phase against a
// start-of-tick snapshot of the swarm, so every particle sees the same neighborhood regardless of order.
public interface NeighborAware {

    float queryRadius();

    void steer(Particle particle, SpatialHash neighbors);
}
