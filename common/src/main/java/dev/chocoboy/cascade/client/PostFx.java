package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.slf4j.Logger;

// Selective bloom: cascade's effects are re-rendered into an offscreen capture (with the scene's depth,
// so hidden effects do not glow through walls), blurred, and added back over the frame. Only our own
// draw calls feed the capture, so the sun and lamps stay untouched and no mixin is needed. Opt in and
// off by default; when off the frame is untouched and nothing here allocates or runs.
//
// on 26.1 the capture rides RenderSystem's output texture overrides, which every RenderType draw honours,
// instead of the removed framebuffer bind; the blur chain is a post_effect json loaded through the shader
// manager, processed over the capture target; and the composite is a fullscreen additive pass (BloomBlit)
// instead of the removed blitToScreen gl state dance.
public final class PostFx {

    private static final Logger LOGGER = LogUtils.getLogger();
    // the post_effect id: assets/cascade/post_effect/bloom.json
    private static final Identifier CHAIN = Identifier.fromNamespaceAndPath("cascade", "bloom");

    private static boolean enabled;
    private static RenderTarget capture;
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
        ensureCapture(main);
        // transparent black, so pixels no effect touches add nothing in the composite
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(capture.getColorTexture(), 0);
        // scene depth so the capture pass depth-tests against the world
        capture.copyDepthFrom(main);
        RenderSystem.outputColorTextureOverride = capture.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = capture.getDepthTextureView();
        try {
            captured = VfxRenderManager.get().render(pose, buffers, camRot, camPos, false);
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
        }
    }

    // blur the capture and add it over the frame, after the level is done rendering. a broken chain
    // disables itself instead of crashing the client; post fx is decoration, not load bearing
    public static void process(float partialTick) {
        if (!enabled || failed || !captured) {
            return;
        }
        captured = false;
        Minecraft mc = Minecraft.getInstance();
        try {
            PostChain chain = mc.getShaderManager().getPostChain(CHAIN, Set.of());
            if (chain == null) {
                failed = true;
                LOGGER.error("Cascade bloom post chain failed to load, post fx disabled");
                return;
            }
            // the chain runs over the capture, not the frame: its "minecraft:main" is our capture target
            chain.process(capture, GraphicsResourceAllocator.UNPOOLED);
            BloomBlit.addTo(capture, mc.getMainRenderTarget());
        } catch (RuntimeException e) {
            failed = true;
            LOGGER.error("Cascade bloom failed, post fx disabled", e);
        }
    }

    private static void ensureCapture(RenderTarget main) {
        if (capture == null) {
            capture = new TextureTarget("cascade-bloom-capture", main.width, main.height, true);
        } else if (capture.width != main.width || capture.height != main.height) {
            capture.resize(main.width, main.height);
        }
    }
}
