package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

public final class FlockModifier implements ParticleModifier, NeighborAware {

    // per-particle neighbor budget so a dense swarm cannot make steering quadratic
    private static final int CAP = 32;

    // below this squared distance a neighbor is self or coincident, so its separation push is skipped
    private static final float EPS = 1e-6f;

    private final float radius;
    private final float separation;
    private final float alignment;
    private final float cohesion;
    private final float maxSpeed;

    public FlockModifier(FlockSpec spec) {
        this.radius = spec.radius();
        this.separation = spec.separation();
        this.alignment = spec.alignment();
        this.cohesion = spec.cohesion();
        this.maxSpeed = spec.maxSpeed();
    }

    @Override
    public void apply(Particle particle) {
    }

    @Override
    public float queryRadius() {
        return radius;
    }

    // three urges over the local neighborhood: separation pushes off close neighbors by inverse distance,
    // alignment matches their average heading, cohesion drifts toward their average position
    @Override
    public void steer(Particle particle, SpatialHash neighbors) {
        float[] acc = new float[9];
        int[] count = {0};
        neighbors.forEachNeighbor(particle.pos, radius, CAP, (pos, vel) -> {
            Vec3f away = particle.pos.sub(pos);
            float d2 = away.lengthSq();
            if (d2 > EPS) {
                float w = 1f / d2;
                acc[0] += away.x() * w;
                acc[1] += away.y() * w;
                acc[2] += away.z() * w;
            }
            acc[3] += vel.x();
            acc[4] += vel.y();
            acc[5] += vel.z();
            acc[6] += pos.x();
            acc[7] += pos.y();
            acc[8] += pos.z();
            count[0]++;
        });
        if (count[0] == 0) {
            return;
        }
        float inv = 1f / count[0];
        Vec3f sep = new Vec3f(acc[0], acc[1], acc[2]).normalize();
        Vec3f align = new Vec3f(acc[3], acc[4], acc[5]).scale(inv).normalize();
        Vec3f center = new Vec3f(acc[6], acc[7], acc[8]).scale(inv);
        Vec3f coh = center.sub(particle.pos).normalize();
        particle.vel = particle.vel
                .add(sep.scale(separation))
                .add(align.scale(alignment))
                .add(coh.scale(cohesion));
        float speed = particle.vel.length();
        if (speed > maxSpeed) {
            particle.vel = particle.vel.scale(maxSpeed / speed);
        }
    }
}
