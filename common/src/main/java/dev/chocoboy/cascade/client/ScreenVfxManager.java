package dev.chocoboy.cascade.client;

import dev.chocoboy.cascade.engine.effect.EmitterSpec;

// holds and draws hud-layer particle systems. it drew them as immediate-mode quads through GuiGraphics'
// pose stack and buffer source. 26.1 rebuilt the gui around a retained GuiRenderState, so that immediate
// path is gone; the hud vfx backend is disabled on this port and spawns are dropped. the full
// implementation lives on the 1.0.0 branch for the re-port. see the porting notes
public final class ScreenVfxManager {

    private static final ScreenVfxManager INSTANCE = new ScreenVfxManager();

    private ScreenVfxManager() {
    }

    public static ScreenVfxManager get() {
        return INSTANCE;
    }

    public void spawn(EmitterSpec spec, float x, float y) {
        MeshDebris.warnOnce("screen-vfx");
    }

    public void tick() {
    }

    public void render() {
    }
}
