package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.random.RandomGenerator;

public record EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
        CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase) {

    public ParticleSystem build(RandomGenerator rng) {
        return new ParticleSystem(shape.sampler(), count, lifetime, speed,
                size.toCurve(), alpha.toCurve(),
                ColorCurve.of(colorStart, colorEnd, colorEase), rng);
    }

    public static EmitterSpec defaultBurst() {
        return new EmitterSpec(
                ShapeSpec.sphere(1.5f), 120, 30, 0.08f,
                new CurveSpec(0.25f, 0f, Easings.EASE_OUT_QUAD),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFCC33, 0xFF3300, Easings.LINEAR);
    }
}
