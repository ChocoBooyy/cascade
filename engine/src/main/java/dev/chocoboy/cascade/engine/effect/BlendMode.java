package dev.chocoboy.cascade.engine.effect;

// how a particle's color combines with what is behind it. ADDITIVE glows (fire, magic),
// ALPHA occludes (smoke, dust). The engine only carries the choice; the renderer applies it.
public enum BlendMode {
    ADDITIVE,
    ALPHA
}
