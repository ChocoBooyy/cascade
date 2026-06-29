package dev.chocoboy.cascade.engine.effect;

// the render-layer description of a system: how its sprites blend, which atlas cell they draw with, how
// far they stretch along their velocity, whether they flipbook through the sprite's animation frames
// over a particle's life, and whether they are tinted by world light. Grouped so the emitter spec composes
// one render description instead of a growing run of loose fields. stretch 0 keeps round billboards; higher
// values elongate fast particles into streaks. lit tints the particle by world light (false keeps it full
// bright), so smoke and dust can sit in shadow while glows stay bright.
public record RenderSpec(BlendMode blend, SpriteId sprite, float stretch, boolean animate, boolean lit) {

    public static final RenderSpec DEFAULT = new RenderSpec(BlendMode.ADDITIVE, SpriteId.GLOW, 0f, false, false);

    public RenderSpec(BlendMode blend, SpriteId sprite) {
        this(blend, sprite, 0f, false, false);
    }
}
