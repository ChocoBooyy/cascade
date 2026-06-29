package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.ShapeSampler;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class ParticleSystem implements EffectSim {

    // hard ceiling so a misconfigured rate emitter cannot grow without bound
    private static final int CAP = 4000;

    private final List<Particle> particles = new ArrayList<>();
    // dead instances kept for reuse so steady-state spawning does not allocate
    private final ArrayDeque<Particle> pool = new ArrayDeque<>();
    private final List<ParticleModifier> modifiers;
    private final ShapeSampler shape;
    private final float speed;
    private final VelocitySpec velocity;
    private final int particleLifetime;
    private final Spawner spawner;
    private final RandomGenerator rng;
    private final Curve size;
    private final Curve alpha;
    private final ColorCurve color;
    private final RotationSpec rotation;
    private final CollisionSpec collision;
    private final CollisionProbe probe;
    private final boolean emitsOnDeath;
    private final TrailSpec trail;
    private final List<Vec3f> spawnRequests = new ArrayList<>();
    private int tick;

    public ParticleSystem(ShapeSampler shape, int count, int particleLifetime, float speed,
            Curve size, Curve alpha, ColorCurve color, RandomGenerator rng) {
        this(shape, count, particleLifetime, speed, size, alpha, color, List.of(), rng);
    }

    public ParticleSystem(ShapeSampler shape, int count, int particleLifetime, float speed,
            Curve size, Curve alpha, ColorCurve color, List<ParticleModifier> modifiers, RandomGenerator rng) {
        this(shape, new BurstSpawner(count), particleLifetime, speed, VelocitySpec.RADIAL, size, alpha, color, modifiers,
                RotationSpec.NONE, CollisionSpec.NONE, null, false, TrailSpec.NONE, rng);
    }

    public ParticleSystem(ShapeSampler shape, Spawner spawner, int particleLifetime, float speed,
            VelocitySpec velocity, Curve size, Curve alpha, ColorCurve color, List<ParticleModifier> modifiers,
            RotationSpec rotation, CollisionSpec collision, CollisionProbe probe, boolean emitsOnDeath,
            TrailSpec trail, RandomGenerator rng) {
        if (particleLifetime < 1) {
            throw new IllegalArgumentException("lifetime < 1");
        }
        this.shape = Objects.requireNonNull(shape, "shape");
        this.spawner = Objects.requireNonNull(spawner, "spawner");
        this.velocity = Objects.requireNonNull(velocity, "velocity");
        this.size = Objects.requireNonNull(size, "size");
        this.alpha = Objects.requireNonNull(alpha, "alpha");
        this.color = Objects.requireNonNull(color, "color");
        this.modifiers = List.copyOf(modifiers);
        this.rotation = Objects.requireNonNull(rotation, "rotation");
        this.collision = Objects.requireNonNull(collision, "collision");
        this.probe = probe;
        this.emitsOnDeath = emitsOnDeath;
        this.trail = Objects.requireNonNull(trail, "trail");
        this.rng = Objects.requireNonNull(rng, "rng");
        this.speed = speed;
        this.particleLifetime = particleLifetime;
        // emit the first batch up front so burst systems are populated the moment they are built
        spawn(spawner.spawnCount(0));
        tick = 1;
    }

    private void spawn(int n) {
        for (int i = 0; i < n && particles.size() < CAP; i++) {
            Vec3f offset = shape.sample(rng);
            Vec3f vel = initialVelocity(offset);
            Particle p = pool.poll();
            if (p == null) {
                p = new Particle(offset, vel, particleLifetime);
            } else {
                p.reset(offset, vel, particleLifetime);
            }
            p.rotation = rotation.angleRange() * rng.nextFloat();
            p.spin = (rng.nextFloat() * 2f - 1f) * rotation.spinRange();
            if (trail.enabled() && (p.trail == null || p.trail.length != trail.length())) {
                p.trail = new Vec3f[trail.length()];
            }
            particles.add(p);
        }
    }

    // the launch velocity for a particle at the given shape-local offset. RADIAL consumes no rng, so
    // existing seeded systems are byte for byte unchanged; only DIRECTIONAL with spread draws from it.
    private Vec3f initialVelocity(Vec3f offset) {
        return switch (velocity.mode()) {
            case RADIAL -> offset.normalize().scale(speed);
            case INWARD -> offset.normalize().scale(-speed);
            case ORBITAL -> {
                Vec3f tangent = new Vec3f(-offset.z(), 0f, offset.x());
                yield tangent.length() < 1e-6f ? Vec3f.ZERO : tangent.normalize().scale(speed);
            }
            case DIRECTIONAL -> directionalVelocity();
        };
    }

    private Vec3f directionalVelocity() {
        Vec3f dir = velocity.direction().length() < 1e-6f ? new Vec3f(0f, 1f, 0f) : velocity.direction().normalize();
        float spread = velocity.spread();
        if (spread <= 0f) {
            return dir.scale(speed);
        }
        float cosMax = (float) Math.cos(spread);
        float cosTheta = cosMax + (1f - cosMax) * rng.nextFloat();
        float sinTheta = (float) Math.sqrt(Math.max(0f, 1f - cosTheta * cosTheta));
        float phi = (float) (rng.nextFloat() * 2.0 * Math.PI);
        // an orthonormal basis around dir, so the cone sample rotates onto the chosen direction
        Vec3f helper = Math.abs(dir.y()) < 0.99f ? new Vec3f(0f, 1f, 0f) : new Vec3f(1f, 0f, 0f);
        Vec3f t1 = dir.cross(helper).normalize();
        Vec3f t2 = dir.cross(t1);
        Vec3f spreadComp = t1.scale(sinTheta * (float) Math.cos(phi)).add(t2.scale(sinTheta * (float) Math.sin(phi)));
        return dir.scale(cosTheta).add(spreadComp).scale(speed);
    }

    @Override
    public boolean tick() {
        spawn(spawner.spawnCount(tick));
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            for (int m = 0; m < modifiers.size(); m++) {
                modifiers.get(m).apply(p);
            }
            integrate(p);
            p.rotation += p.spin;
            if (p.trail != null) {
                p.trail[p.trailHead] = p.pos;
                p.trailHead = (p.trailHead + 1) % p.trail.length;
                if (p.trailCount < p.trail.length) {
                    p.trailCount++;
                }
            }
            p.age++;
            if (p.dead()) {
                if (emitsOnDeath) {
                    spawnRequests.add(p.pos);
                }
                // swap-remove since order does not matter; the moved element sits above i and is already processed
                particles.set(i, particles.get(particles.size() - 1));
                particles.remove(particles.size() - 1);
                pool.push(p);
            }
        }
        tick++;
        return isDone();
    }

    // advance one particle by its velocity, resolving block collisions per axis so it can slide along a
    // wall instead of stopping dead. bounce is the speed kept on a hit, friction sheds the rest
    private void integrate(Particle p) {
        if (!collision.enabled() || probe == null) {
            p.pos = p.pos.add(p.vel);
            return;
        }
        float px = p.pos.x(), py = p.pos.y(), pz = p.pos.z();
        float vx = p.vel.x(), vy = p.vel.y(), vz = p.vel.z();
        boolean hitX = probe.solid(px + vx, py, pz);
        boolean hitY = probe.solid(px, py + vy, pz);
        boolean hitZ = probe.solid(px, py, pz + vz);
        if (hitX) vx = -vx * collision.bounce();
        if (hitY) vy = -vy * collision.bounce();
        if (hitZ) vz = -vz * collision.bounce();
        if (hitX || hitY || hitZ) {
            float keep = 1f - collision.friction();
            if (!hitX) vx *= keep;
            if (!hitY) vy *= keep;
            if (!hitZ) vz *= keep;
        }
        p.vel = new Vec3f(vx, vy, vz);
        p.pos = new Vec3f(hitX ? px : px + vx, hitY ? py : py + vy, hitZ ? pz : pz + vz);
    }

    @Override
    public boolean isDone() {
        return spawner.exhausted(tick) && particles.isEmpty();
    }

    public List<Particle> particles() {
        return particles;
    }

    public List<Vec3f> drainSpawnRequests() {
        List<Vec3f> drained = new ArrayList<>(spawnRequests);
        spawnRequests.clear();
        return drained;
    }

    public float sizeOf(Particle p) {
        return size.at(p.life());
    }

    public float alphaOf(Particle p) {
        return alpha.at(p.life());
    }

    public int colorOf(Particle p) {
        return color.at(p.life());
    }
}
