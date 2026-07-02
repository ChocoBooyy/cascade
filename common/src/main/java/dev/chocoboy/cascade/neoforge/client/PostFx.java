package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

// Bloom over the frame after the level renders: bright pixels above a luminance threshold are blurred
// and added back, so hot vfx glow. Opt in and off by default; when off the frame is untouched and
// nothing here allocates or runs.
public final class PostFx {

    private static final Logger LOGGER = LogUtils.getLogger();
    // PostChain wants the full resource path, the way vanilla passes shaders/post/<name>.json
    private static final ResourceLocation CHAIN =
            ResourceLocation.fromNamespaceAndPath("cascade", "shaders/post/bloom.json");

    private static boolean enabled;
    private static PostChain chain;
    private static boolean failed;
    private static int width;
    private static int height;

    private PostFx() {
    }

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void process(float partialTick) {
        if (!enabled || failed) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (chain == null) {
            chain = load(mc, main);
            if (chain == null) {
                return;
            }
            width = main.width;
            height = main.height;
            chain.resize(width, height);
        }
        if (main.width != width || main.height != height) {
            width = main.width;
            height = main.height;
            chain.resize(width, height);
        }
        chain.process(partialTick);
        // the chain rebinds its own targets, so restore the main target for whatever draws next
        main.bindWrite(false);
    }

    // a broken chain disables itself instead of crashing the client; post fx is decoration, not load bearing
    private static PostChain load(Minecraft mc, RenderTarget main) {
        try {
            return new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, CHAIN);
        } catch (IOException | RuntimeException e) {
            failed = true;
            LOGGER.error("Cascade bloom post chain failed to load, post fx disabled", e);
            return null;
        }
    }
}
