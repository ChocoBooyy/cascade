package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

// the hud particle pipelines: the textured world pair with no depth test, so the gui pass draws them over
// whatever scene depth is left, and no depth write so they never disturb it. they live in common because
// the retained gui renderer draws elements straight from a pipeline, which the hud manager here hands it;
// each loader still registers them. the cutout fragment stage keeps sprite silhouettes identical to the
// world pass
public final class ScreenVfxPipelines {

    public static final RenderPipeline ADDITIVE = build("gui_textured_additive", BlendFunction.ADDITIVE);
    public static final RenderPipeline ALPHA = build("gui_textured_alpha", BlendFunction.TRANSLUCENT);

    private ScreenVfxPipelines() {
    }

    private static RenderPipeline build(String name, BlendFunction blend) {
        return CorePipelines.worldBuilder(name)
                .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("cascade", "core/position_tex_cutout"))
                .withSampler("Sampler0")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withCull(false)
                .build();
    }
}
