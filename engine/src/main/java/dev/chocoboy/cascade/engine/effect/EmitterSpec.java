package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.emitter.ShapeSpec;
import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.CurveSpec;
import dev.chocoboy.cascade.engine.tween.Easings;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public record EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
        CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase,
        List<ModifierSpec> modifiers, EmissionSpec emission, RenderSpec render,
        RotationSpec rotation, CollisionSpec collision, SubEmitterSpec subEmitter, TrailSpec trail) {

    public EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
            CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase) {
        this(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase, List.of(), EmissionSpec.burst());
    }

    public EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
            CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase,
            List<ModifierSpec> modifiers) {
        this(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase, modifiers, EmissionSpec.burst());
    }

    public EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
            CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase,
            List<ModifierSpec> modifiers, EmissionSpec emission) {
        this(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase, modifiers, emission,
                RenderSpec.DEFAULT, RotationSpec.NONE, CollisionSpec.NONE);
    }

    public EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
            CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase,
            List<ModifierSpec> modifiers, EmissionSpec emission, RenderSpec render, RotationSpec rotation) {
        this(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase, modifiers, emission,
                render, rotation, CollisionSpec.NONE);
    }

    public EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
            CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase,
            List<ModifierSpec> modifiers, EmissionSpec emission, RenderSpec render,
            RotationSpec rotation, CollisionSpec collision) {
        this(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase, modifiers, emission,
                render, rotation, collision, null, TrailSpec.NONE);
    }

    public EmitterSpec(ShapeSpec shape, int count, int lifetime, float speed,
            CurveSpec size, CurveSpec alpha, int colorStart, int colorEnd, Easings colorEase,
            List<ModifierSpec> modifiers, EmissionSpec emission, RenderSpec render,
            RotationSpec rotation, CollisionSpec collision, SubEmitterSpec subEmitter) {
        this(shape, count, lifetime, speed, size, alpha, colorStart, colorEnd, colorEase, modifiers, emission,
                render, rotation, collision, subEmitter, TrailSpec.NONE);
    }

    public ParticleSystem build(RandomGenerator rng) {
        return build(rng, null);
    }

    public ParticleSystem build(RandomGenerator rng, CollisionProbe probe) {
        List<ParticleModifier> built = new ArrayList<>(modifiers.size());
        for (ModifierSpec m : modifiers) {
            built.add(m.toModifier());
        }
        return new ParticleSystem(shape.sampler(), emission.spawner(count), lifetime, speed,
                size.toCurve(), alpha.toCurve(),
                ColorCurve.of(colorStart, colorEnd, colorEase), built, rotation, collision, probe,
                subEmitter != null, trail, rng);
    }

    public static EmitterSpec defaultBurst() {
        return new EmitterSpec(
                ShapeSpec.sphere(1.5f), 120, 30, 0.08f,
                new CurveSpec(0.25f, 0f, Easings.EASE_OUT_QUAD),
                new CurveSpec(1f, 0f, Easings.LINEAR),
                0xFFCC33, 0xFF3300, Easings.LINEAR);
    }
}
