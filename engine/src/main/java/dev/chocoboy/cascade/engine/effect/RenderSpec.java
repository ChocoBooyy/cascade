package dev.chocoboy.cascade.engine.effect;

// the render-layer description of a system: how its sprites blend, which atlas cell they draw with, and
// how far they stretch along their velocity. Grouped so the emitter spec composes one render description
// instead of a growing run of loose fields. stretch 0 keeps round billboards; higher values elongate
// fast particles into streaks (sparks, rain).
public record RenderSpec(BlendMode blend, SpriteId sprite, float stretch) {

    public static final RenderSpec DEFAULT = new RenderSpec(BlendMode.ADDITIVE, SpriteId.GLOW, 0f);

    public RenderSpec(BlendMode blend, SpriteId sprite) {
        this(blend, sprite, 0f);
    }
}
