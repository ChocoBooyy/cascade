package dev.chocoboy.cascade.engine.effect;

/**
 * How a system's particles are drawn: NONE billboards (the default), CUBE a solid box, SHARD an elongated
 * splinter built from the same box under a stretched scale, BLOCK a baked vanilla block model named by
 * {@code RenderSpec.meshModel}, ITEM a baked vanilla item model named the same way.
 */
public enum MeshId {
    NONE,
    CUBE,
    SHARD,
    BLOCK,
    ITEM
}
