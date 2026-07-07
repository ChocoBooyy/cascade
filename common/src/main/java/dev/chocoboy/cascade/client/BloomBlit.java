package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalInt;
import net.minecraft.resources.Identifier;

// the bloom composite: adds the blurred capture over the frame. vanilla's blitAndBlendToTexture does this
// exact draw with the entity outline pipeline, whose alpha blend would dim the frame under the capture's
// empty pixels; this is the same fullscreen-triangle draw on an additive twin of that pipeline, so black
// stays invisible and glow only ever brightens. public because the loader's pipeline registration lives in
// another package
public final class BloomBlit {

    // vanilla's entity_outline_blit config (screenquad + blit_screen over a fullscreen triangle, sampling
    // InSampler) rebuilt on the public builder, with the blend swapped to additive. the derivation cannot
    // ride toBuilder, which is loader-widened api fabric does not see
    public static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("cascade", "bloom_blit"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/blit_screen"))
            .withSampler("InSampler")
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
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
