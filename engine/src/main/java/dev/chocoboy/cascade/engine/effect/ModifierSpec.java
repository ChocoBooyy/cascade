package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

// Serializable description of a modifier. Kept flat (one record, fields reused per kind) so the
// network codec stays a fixed-shape composite instead of a dispatched union.
public record ModifierSpec(Kind kind, Vec3f vec, float a, float b) {

    public enum Kind {
        GRAVITY, DRAG, TURBULENCE, ATTRACTOR, VORTEX, CURL
    }

    public static ModifierSpec gravity(Vec3f accel) {
        return new ModifierSpec(Kind.GRAVITY, accel, 0f, 0f);
    }

    public static ModifierSpec drag(float drag) {
        return new ModifierSpec(Kind.DRAG, Vec3f.ZERO, drag, 0f);
    }

    public static ModifierSpec turbulence(float strength, float frequency) {
        return new ModifierSpec(Kind.TURBULENCE, Vec3f.ZERO, strength, frequency);
    }

    public static ModifierSpec attractor(Vec3f center, float strength) {
        return new ModifierSpec(Kind.ATTRACTOR, center, strength, 0f);
    }

    public static ModifierSpec vortex(Vec3f center, float strength) {
        return new ModifierSpec(Kind.VORTEX, center, strength, 0f);
    }

    public static ModifierSpec curl(float strength, float frequency) {
        return new ModifierSpec(Kind.CURL, Vec3f.ZERO, strength, frequency);
    }

    public ParticleModifier toModifier() {
        return switch (kind) {
            case GRAVITY -> new GravityModifier(vec);
            case DRAG -> new DragModifier(a);
            case TURBULENCE -> new TurbulenceModifier(a, b);
            case ATTRACTOR -> new AttractorModifier(vec, a);
            case VORTEX -> new VortexModifier(vec, a);
            case CURL -> new CurlModifier(a, b);
        };
    }
}
