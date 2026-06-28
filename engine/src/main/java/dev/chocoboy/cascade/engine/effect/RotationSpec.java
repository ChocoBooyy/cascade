package dev.chocoboy.cascade.engine.effect;

// initial roll and spin for a system's particles. angleRange is the upper bound of a particle's
// random starting roll (use a full turn for no preferred orientation); spinRange bounds its random
// per-tick spin in either direction. Both zero means sprites never rotate.
public record RotationSpec(float angleRange, float spinRange) {

    public static final RotationSpec NONE = new RotationSpec(0f, 0f);

    // random starting roll plus a spin within the given magnitude
    public static RotationSpec spin(float spinRange) {
        return new RotationSpec((float) (Math.PI * 2.0), spinRange);
    }
}
