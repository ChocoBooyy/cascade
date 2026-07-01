package dev.chocoboy.cascade.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenderSpecTest {

    @Test
    void defaultHasNoMeshModel() {
        assertEquals(MeshId.NONE, RenderSpec.DEFAULT.mesh());
        assertEquals("", RenderSpec.DEFAULT.meshModel());
    }

    @Test
    void carriesAnOpaqueModelString() {
        RenderSpec r = new RenderSpec(BlendMode.ADDITIVE, SpriteId.GLOW, 0f, false, true, MeshId.BLOCK, "minecraft:stone");
        assertEquals(MeshId.BLOCK, r.mesh());
        assertEquals("minecraft:stone", r.meshModel());
    }
}
