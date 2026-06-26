package dev.chocoboy.cascade.neoforge.client;

public interface RenderedEffect {

    // advance one tick; return true once the effect has finished and should be removed
    boolean tick();

    void render(VfxFrame frame);
}
