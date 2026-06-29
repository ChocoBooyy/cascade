package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.ShapeSampler;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class ParticleSystem implements EffectSim {

    // hard ceiling so a misconfigured rate emitter cannot grow without bound
    private static final int CAP = 4000;

    private final List<Particle> particles = new ArrayList<>();
    private final List<ParticleModifier> modifiers;
    private final ShapeSampler shape;
    private final float speed;
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
        this(shape, new BurstSpawner(count), particleLifetime, speed, size, alpha, color, modifiers,
                RotationSpec.NONE, CollisionSpec.NONE, null, false, TrailSpec.NONE, rng);
    }

    public ParticleSystem(ShapeSampler shape, Spawner spawner, int particleLifetime, float speed,
            Curve size, Curve alpha, ColorCurve color, List<ParticleModifier> modifiers,
            RotationSpec rotation, CollisionSpec collision, CollisionProbe probe, boolean emitsOnDeath,
            TrailSpec trail, RandomGenerator rng) {
        if (particleLifetime < 1) {
            throw new IllegalArgumentException("lifetime < 1");
        }
        this.shape = Objects.requireNonNull(shape, "shape");
        this.spawner = Objects.requireNonNull(spawner, "spawner");
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
            Particle p = new Particle(offset, offset.normalize().scale(speed), particleLifetime);
            p.rotation = rotation.angleRange() * rng.nextFloat();
            p.spin = (rng.nextFloat() * 2f - 1f) * rotation.spinRange();
            if (trail.enabled()) {
                p.trail = new Vec3f[trail.length()];
            }
            particles.add(p);
        }
    }

    @Override
    public boolean tick() {
        spawn(spawner.spawnCount(tick));
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            for (int m = 0; m < modifiers.size(); m++) {
                modifiers.get(m).apply(p);
            }
            if (collision.enabled() && probe != null) {
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
            } else {
                p.pos = p.pos.add(p.vel);
            }
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
                particles.remove(i);
            }
        }
        tick++;
        return isDone();
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
