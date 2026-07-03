package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.platform.Window;
import dev.chocoboy.cascade.VfxEmitter;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import net.minecraft.client.Minecraft;

// hud-layer particles: plays an emitter at a point in gui-scaled coords, client side only. there is no
// server call here, screen effects never touch the network
public final class ScreenVfx {

    private ScreenVfx() {
    }

    public static void play(EmitterSpec spec, float x, float y) {
        ScreenVfxManager.get().spawn(spec, x, y);
    }

    public static void play(VfxEmitter emitter, float x, float y) {
        play(emitter.spec(), x, y);
    }

    public static void playCentered(VfxEmitter emitter) {
        Window window = Minecraft.getInstance().getWindow();
        play(emitter.spec(), window.getGuiScaledWidth() / 2f, window.getGuiScaledHeight() / 2f);
    }
}
