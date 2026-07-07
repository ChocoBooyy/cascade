package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.chocoboy.cascade.CascadeCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

// owns a screen sized depth copy of the main framebuffer, so a soft particle shader can read scene depth
// without sampling the framebuffer it is currently drawing into (which would be a feedback loop). on 26.1
// the copy is a pure texture-to-texture blit, so unlike the pre-26.1 version there is no framebuffer
// binding to restore afterwards.
//
// the copy is exposed to render types through a texture manager entry, because a RenderSetup binds
// textures by Identifier. a RenderSetup resolves that entry to a gpu view when built and caches it, and a
// resize recreates the underlying texture, so every resize bumps a generation; the soft render types are
// rebuilt whenever the generation moves (see the loader's render types). public because the loader's
// render-type setup lives in another package
public final class SoftDepth {

    private static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath(CascadeCommon.MOD_ID, "soft_depth");

    private static RenderTarget target;
    private static int generation;

    private SoftDepth() {
    }

    public static Identifier textureId() {
        return TEXTURE_ID;
    }

    // bumped whenever the depth texture is recreated, so cached resolutions of it can be rebuilt
    public static int generation() {
        return generation;
    }

    static void copyFromMain() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        ensureTarget(main.width, main.height);
        target.copyDepthFrom(main);
    }

    // a soft render type being built before any soft effect has refreshed the copy still needs a texture
    // to resolve; whatever depth the main target holds right now is the best available answer
    public static void ensureReady() {
        if (target == null) {
            copyFromMain();
        }
    }

    private static void ensureTarget(int width, int height) {
        if (target == null) {
            target = new TextureTarget("cascade-soft-depth", width, height, true);
            generation++;
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, new DepthTexture());
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
            generation++;
        }
    }

    // the texture manager entry: delegates to the current depth copy, so a RenderSetup built after a
    // resize resolves the fresh texture
    private static final class DepthTexture extends AbstractTexture {

        @Override
        public GpuTexture getTexture() {
            return target.getDepthTexture();
        }

        @Override
        public GpuTextureView getTextureView() {
            return target.getDepthTextureView();
        }
    }
}
