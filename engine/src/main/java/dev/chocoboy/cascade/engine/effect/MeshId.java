package dev.chocoboy.cascade.engine.effect;

// how a system's particles are drawn: NONE billboards (the default), CUBE a solid box, SHARD an
// elongated splinter built from the same box under a stretched scale, BLOCK draws a baked vanilla
// block model named by RenderSpec.meshModel, ITEM draws a baked vanilla item model named by
// RenderSpec.meshModel
public enum MeshId {
    NONE,
    CUBE,
    SHARD,
    BLOCK,
    ITEM
}
