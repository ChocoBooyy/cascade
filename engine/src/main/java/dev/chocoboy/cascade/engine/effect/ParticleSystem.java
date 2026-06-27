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

    private final List<Particle> particles = new ArrayList<>();
    private final List<ParticleModifier> modifiers;
    private final Curve size;
    private final Curve alpha;
    private final ColorCurve color;

    public ParticleSystem(ShapeSampler shape, int count, int particleLifetime, float speed,
            Curve size, Curve alpha, ColorCurve color, RandomGenerator rng) {
        this(shape, count, particleLifetime, speed, size, alpha, color, List.of(), rng);
    }

    public ParticleSystem(ShapeSampler shape, int count, int particleLifetime, float speed,
            Curve size, Curve alpha, ColorCurve color, List<ParticleModifier> modifiers, RandomGenerator rng) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        if (particleLifetime < 1) {
            throw new IllegalArgumentException("lifetime < 1");
        }
        this.size = Objects.requireNonNull(size, "size");
        this.alpha = Objects.requireNonNull(alpha, "alpha");
        this.color = Objects.requireNonNull(color, "color");
        this.modifiers = List.copyOf(modifiers);
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(rng, "rng");
        for (int i = 0; i < count; i++) {
            Vec3f offset = shape.sample(rng);
            particles.add(new Particle(offset, offset.normalize().scale(speed), particleLifetime));
        }
    }

    @Override
    public boolean tick() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            for (int m = 0; m < modifiers.size(); m++) {
                modifiers.get(m).apply(p);
            }
            p.pos = p.pos.add(p.vel);
            p.age++;
            if (p.dead()) {
                particles.remove(i);
            }
        }
        return particles.isEmpty();
    }

    @Override
    public boolean isDone() {
        return particles.isEmpty();
    }

    public List<Particle> particles() {
        return particles;
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
