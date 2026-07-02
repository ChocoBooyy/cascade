package dev.chocoboy.cascade.neoforge.client;

import net.minecraft.world.phys.Vec3;

public interface RenderedEffect {

    // advance one tick; return true once the effect has finished and should be removed
    boolean tick();

    void render(VfxFrame frame);

    // world center, so the manager can distance cull and order effects nearest first
    Vec3 position();

    // primitive count this effect would draw, so a per-frame budget can drop the farthest work
    int drawCount();

    // whether this effect samples the scene depth copy, so the manager only refreshes it when one is visible
    default boolean soft() {
        return false;
    }
}
