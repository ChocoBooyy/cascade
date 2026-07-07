package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

// the bloom composite: adds the blurred capture over the frame. vanilla's blitAndBlendToTexture does this
// exact draw with the entity outline pipeline, whose alpha blend would dim the frame under the capture's
// empty pixels; this is the same fullscreen-triangle draw on an additive twin of that pipeline, so black
// stays invisible and glow only ever brightens. public because the loader's pipeline registration lives in
// another package
public final class BloomBlit {

    public static final RenderPipeline PIPELINE = RenderPipelines.ENTITY_OUTLINE_BLIT.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("cascade", "bloom_blit"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .build();

    private BloomBlit() {
    }

    static void addTo(RenderTarget source, RenderTarget target) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "cascade bloom composite", target.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("InSampler", source.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(0, 3);
        }
    }
}
