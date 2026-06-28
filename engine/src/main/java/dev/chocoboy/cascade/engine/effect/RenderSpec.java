package dev.chocoboy.cascade.engine.effect;

// the render-layer description of a system: how its sprites blend and which atlas cell they draw with.
// Grouped so the emitter spec composes one render description instead of a growing run of loose fields.
public record RenderSpec(BlendMode blend, SpriteId sprite) {

    public static final RenderSpec DEFAULT = new RenderSpec(BlendMode.ADDITIVE, SpriteId.GLOW);
}
