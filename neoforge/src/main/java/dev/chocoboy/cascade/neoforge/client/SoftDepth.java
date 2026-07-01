package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

// owns a screen sized depth copy of the main framebuffer, so a soft particle shader can read scene depth
// without sampling the framebuffer it is currently drawing into (which would be a feedback loop)
final class SoftDepth {

    private static RenderTarget target;

    private SoftDepth() {
    }

    static void copyFromMain() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (target == null) {
            target = new TextureTarget(main.width, main.height, true, false);
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height, false);
        }
        target.copyDepthFrom(main);
    }

    static int depthTextureId() {
        return target != null ? target.getDepthTextureId() : 0;
    }
}
