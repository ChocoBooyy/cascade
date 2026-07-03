package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.slf4j.Logger;

// Selective bloom: cascade's effects are re-rendered into an offscreen capture (with the scene's depth,
// so hidden effects do not glow through walls), blurred, and added back over the frame. Only our own
// draw calls feed the capture, so the sun and lamps stay untouched and no mixin is needed. Opt in and
// off by default; when off the frame is untouched and nothing here allocates or runs.
public final class PostFx {

    private static final Logger LOGGER = LogUtils.getLogger();
    // PostChain wants the full resource path, the way vanilla passes shaders/post/<name>.json
    private static final ResourceLocation CHAIN =
            ResourceLocation.fromNamespaceAndPath("cascade", "shaders/post/bloom.json");

    private static boolean enabled;
    private static RenderTarget capture;
    private static PostChain chain;
    private static boolean failed;
    private static boolean captured;

    private PostFx() {
    }

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static boolean enabled() {
        return enabled;
    }

    // called by the loader right after the normal effect draw, while the frame's matrices still match it.
    // renders the same effects again into the capture target that the blur chain reads later in the frame
    public static void captureVfx(PoseStack pose, MultiBufferSource.BufferSource buffers,
            Quaternionf camRot, Vec3 camPos) {
        if (!enabled || failed) {
            return;
        }
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        ensureTargets(main);
        if (failed) {
            return;
        }
        capture.clear(Minecraft.ON_OSX);
        // scene depth so the capture pass depth-tests against the world. copyDepthFrom leaves framebuffer
        // 0 bound, so the capture bind below is load bearing
        capture.copyDepthFrom(main);
        capture.bindWrite(false);
        captured = VfxRenderManager.get().render(pose, buffers, camRot, camPos, false);
        main.bindWrite(false);
    }

    // blur the capture and add it over the frame, after the level is done rendering
    public static void process(float partialTick) {
        if (!enabled || failed || !captured) {
            return;
        }
        captured = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        chain.process(partialTick);
        // the chain rebinds its own targets, so restore the main target for the composite and whatever draws next
        main.bindWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        capture.blitToScreen(main.width, main.height, false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        // blitToScreen leaves depth writes and depth test off; put them back for the rest of the frame
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        main.bindWrite(true);
    }

    private static void ensureTargets(RenderTarget main) {
        if (capture == null) {
            capture = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            capture.setClearColor(0f, 0f, 0f, 0f);
            chain = load(main);
            if (chain != null) {
                chain.resize(main.width, main.height);
            }
        } else if (capture.width != main.width || capture.height != main.height) {
            capture.resize(main.width, main.height, Minecraft.ON_OSX);
            chain.resize(main.width, main.height);
        }
    }

    // a broken chain disables itself instead of crashing the client; post fx is decoration, not load bearing
    private static PostChain load(RenderTarget main) {
        Minecraft mc = Minecraft.getInstance();
        try {
            // the chain runs over the capture, not the frame: its "minecraft:main" is our capture target
            return new PostChain(mc.getTextureManager(), mc.getResourceManager(), capture, CHAIN);
        } catch (IOException | RuntimeException e) {
            failed = true;
            LOGGER.error("Cascade bloom post chain failed to load, post fx disabled", e);
            return null;
        }
    }
}
