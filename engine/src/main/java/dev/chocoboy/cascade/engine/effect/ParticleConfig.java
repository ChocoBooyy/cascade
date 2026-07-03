package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.tween.ColorCurve;
import dev.chocoboy.cascade.engine.tween.Curve;
import java.util.List;

// the resolved runtime description a ParticleSystem ticks against. distinct from the *Spec records: those are
// the serializable authoring layer, this holds their built forms (Curve not CurveSpec, ParticleModifier not
// ComponentSpec) so the sim never reaches back into the spec layer.
public record ParticleConfig(int lifetime, float speed, VelocitySpec velocity,
        Curve size, Curve alpha, ColorCurve color, List<ParticleModifier> modifiers,
        RotationSpec rotation, CollisionSpec collision,
        boolean emitsOnDeath, boolean emitsOnCollision, boolean tumble, TrailSpec trail) {

    public ParticleConfig {
        if (lifetime < 1) {
            throw new IllegalArgumentException("lifetime < 1");
        }
        modifiers = List.copyOf(modifiers);
    }

    // the common case: a radial burst with no steering, rotation, collision, sub-emitters or trail. callers
    // layer the parts they need on top with the with* copies.
    public static ParticleConfig burst(int lifetime, float speed, Curve size, Curve alpha, ColorCurve color) {
        return new ParticleConfig(lifetime, speed, VelocitySpec.RADIAL, size, alpha, color, List.of(),
                RotationSpec.NONE, CollisionSpec.NONE, false, false, false, TrailSpec.NONE);
    }

    public ParticleConfig withVelocity(VelocitySpec velocity) {
        return new ParticleConfig(lifetime, speed, velocity, size, alpha, color, modifiers, rotation, collision,
                emitsOnDeath, emitsOnCollision, tumble, trail);
    }

    public ParticleConfig withModifiers(List<ParticleModifier> modifiers) {
        return new ParticleConfig(lifetime, speed, velocity, size, alpha, color, modifiers, rotation, collision,
                emitsOnDeath, emitsOnCollision, tumble, trail);
    }

    public ParticleConfig withRotation(RotationSpec rotation) {
        return new ParticleConfig(lifetime, speed, velocity, size, alpha, color, modifiers, rotation, collision,
                emitsOnDeath, emitsOnCollision, tumble, trail);
    }

    public ParticleConfig withCollision(CollisionSpec collision) {
        return new ParticleConfig(lifetime, speed, velocity, size, alpha, color, modifiers, rotation, collision,
                emitsOnDeath, emitsOnCollision, tumble, trail);
    }

    public ParticleConfig withEmitsOnCollision(boolean emitsOnCollision) {
        return new ParticleConfig(lifetime, speed, velocity, size, alpha, color, modifiers, rotation, collision,
                emitsOnDeath, emitsOnCollision, tumble, trail);
    }
}
