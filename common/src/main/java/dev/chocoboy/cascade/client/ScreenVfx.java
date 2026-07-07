package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.platform.Window;
import dev.chocoboy.cascade.VfxEmitter;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import net.minecraft.client.Minecraft;

/**
 * Hud-layer particles: plays an emitter at a point in gui-scaled coordinates, client side only; screen
 * effects never touch the network. Positions and sizes are gui units and screen y grows downward. Of the
 * render spec, only blend, sprite, and flipbook animation apply on the hud; stretch, lit, soft, and mesh
 * are world features and are ignored, and sub-emitter spawns are dropped.
 */
public final class ScreenVfx {

    private ScreenVfx() {
    }

    /** Plays an emitter spec with its particle origin at gui-scaled point {@code (x, y)}. */
    public static void play(EmitterSpec spec, float x, float y) {
        ScreenVfxManager.get().spawn(spec, x, y);
    }

    /** Plays a built emitter with its particle origin at gui-scaled point {@code (x, y)}. */
    public static void play(VfxEmitter emitter, float x, float y) {
        play(emitter.spec(), x, y);
    }

    /** Plays a built emitter centered on the screen. */
    public static void playCentered(VfxEmitter emitter) {
        Window window = Minecraft.getInstance().getWindow();
        play(emitter.spec(), window.getGuiScaledWidth() / 2f, window.getGuiScaledHeight() / 2f);
    }
}
