package dev.chocoboy.cascade.engine.effect;

import dev.chocoboy.cascade.engine.math.Vec3f;

// how a freshly spawned particle is launched. RADIAL fires outward from the shape center (the historical
// default), INWARD fires toward it (implosions), DIRECTIONAL fires along a vector inside a cone (jets),
// ORBITAL fires tangent to the vertical axis so particles circle the center (swirls). spread is the cone
// half angle in radians and only applies to DIRECTIONAL.
public record VelocitySpec(Mode mode, Vec3f direction, float spread) {

    public enum Mode {
        RADIAL, INWARD, DIRECTIONAL, ORBITAL
    }

    public static final VelocitySpec RADIAL = new VelocitySpec(Mode.RADIAL, Vec3f.ZERO, 0f);

    public static VelocitySpec inward() {
        return new VelocitySpec(Mode.INWARD, Vec3f.ZERO, 0f);
    }

    public static VelocitySpec directional(Vec3f direction, float spread) {
        return new VelocitySpec(Mode.DIRECTIONAL, direction, spread);
    }

    public static VelocitySpec orbital() {
        return new VelocitySpec(Mode.ORBITAL, Vec3f.ZERO, 0f);
    }
}
