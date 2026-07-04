package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.chocoboy.cascade.client.ParticleAtlas;
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

// the neoforge render types, rebuilt for 26.1. the old RenderStateShard composite builder is gone, so each
// type is a RenderPipeline (derived from the closest vanilla pipeline so it inherits that shader's uniform
// and sampler wiring) paired with a RenderSetup that binds the particle atlas. custom pipelines must be
// registered with the gpu device, which happens through registerPipelines below.
//
// first cut of the port: the soft (depth-fading) and lightmap-lit variants are not rebuilt yet, so the
// provider maps them onto their hard-edged and unlit twins. see the porting notes
final class VfxRenderTypes {

    private static final int BUFFER_BYTES = 1536;

    private static final DepthStencilState DEPTH_NO_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false);
    private static final DepthStencilState DEPTH_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);
    private static final DepthStencilState NO_DEPTH = new DepthStencilState(CompareOp.ALWAYS_PASS, false);

    private VfxRenderTypes() {
    }

    private static RenderPipeline pipeline(String name, RenderPipeline base, VertexFormat format,
            ColorTargetState color, DepthStencilState depth) {
        return base.toBuilder()
                .withLocation(Identifier.fromNamespaceAndPath("cascade", name))
                .withVertexFormat(format, VertexFormat.Mode.QUADS)
                .withColorTargetState(color)
                .withDepthStencilState(depth)
                .withCull(false)
                .build();
    }

    // untextured additive quads for beams: additive blend, depth tested so terrain occludes, no depth write
    private static final RenderPipeline ADDITIVE_PIPE = pipeline("additive",
            RenderPipelines.DEBUG_QUADS, DefaultVertexFormat.POSITION_COLOR,
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    // textured atlas sprites in world, additive (glows) and translucent (smoke); depth tested, no depth write
    private static final RenderPipeline TEXTURED_ADDITIVE_PIPE = pipeline("textured_additive",
            RenderPipelines.GUI_TEXTURED, DefaultVertexFormat.POSITION_TEX_COLOR,
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    private static final RenderPipeline TEXTURED_ALPHA_PIPE = pipeline("textured_alpha",
            RenderPipelines.GUI_TEXTURED, DefaultVertexFormat.POSITION_TEX_COLOR,
            new ColorTargetState(BlendFunction.TRANSLUCENT), DEPTH_NO_WRITE);

    // double sided solid geometry for mesh particles: opaque, depth tested and written so cubes read as 3D
    private static final RenderPipeline SOLID_PIPE = pipeline("solid",
            RenderPipelines.DEBUG_QUADS, DefaultVertexFormat.POSITION_COLOR,
            ColorTargetState.DEFAULT, DEPTH_WRITE);

    // lit textured sprites use the particle format and shader, so the world lightmap tints them
    private static final RenderPipeline TEXTURED_ADDITIVE_LIT_PIPE = pipeline("textured_additive_lit",
            RenderPipelines.TRANSLUCENT_PARTICLE, DefaultVertexFormat.PARTICLE,
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    private static final RenderPipeline TEXTURED_ALPHA_LIT_PIPE = pipeline("textured_alpha_lit",
            RenderPipelines.TRANSLUCENT_PARTICLE, DefaultVertexFormat.PARTICLE,
            new ColorTargetState(BlendFunction.TRANSLUCENT), DEPTH_NO_WRITE);

    // gui twins: no depth test, so the hud pass draws over whatever scene depth is left, and no depth write
    private static final RenderPipeline GUI_TEXTURED_ADDITIVE_PIPE = pipeline("gui_textured_additive",
            RenderPipelines.GUI_TEXTURED, DefaultVertexFormat.POSITION_TEX_COLOR,
            new ColorTargetState(BlendFunction.ADDITIVE), NO_DEPTH);

    private static final RenderPipeline GUI_TEXTURED_ALPHA_PIPE = pipeline("gui_textured_alpha",
            RenderPipelines.GUI_TEXTURED, DefaultVertexFormat.POSITION_TEX_COLOR,
            new ColorTargetState(BlendFunction.TRANSLUCENT), NO_DEPTH);

    private static final List<RenderPipeline> ALL = List.of(
            ADDITIVE_PIPE, TEXTURED_ADDITIVE_PIPE, TEXTURED_ALPHA_PIPE, SOLID_PIPE,
            TEXTURED_ADDITIVE_LIT_PIPE, TEXTURED_ALPHA_LIT_PIPE,
            GUI_TEXTURED_ADDITIVE_PIPE, GUI_TEXTURED_ALPHA_PIPE);

    // the gpu device only compiles pipelines it knows about, so every custom pipeline is registered here
    static void registerPipelines(RegisterRenderPipelinesEvent event) {
        ALL.forEach(event::registerPipeline);
    }

    // an untextured type: just the pipeline and a vertex buffer
    private static RenderType plain(String name, RenderPipeline pipeline, boolean sort) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).bufferSize(BUFFER_BYTES);
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }

    // a textured type: binds the particle atlas to Sampler0, and the lightmap too when the shader is lit
    private static RenderType textured(String name, RenderPipeline pipeline, boolean sort, boolean lightmap) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", ParticleAtlas.textureId())
                .bufferSize(BUFFER_BYTES);
        if (lightmap) {
            setup.useLightmap();
        }
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }

    static final RenderType ADDITIVE = plain("cascade_additive", ADDITIVE_PIPE, false);
    static final RenderType SOLID = plain("cascade_solid", SOLID_PIPE, false);
    static final RenderType TEXTURED_ADDITIVE = textured("cascade_textured_additive", TEXTURED_ADDITIVE_PIPE, false, false);
    static final RenderType TEXTURED_ALPHA = textured("cascade_textured_alpha", TEXTURED_ALPHA_PIPE, true, false);
    static final RenderType TEXTURED_ADDITIVE_LIT = textured("cascade_textured_additive_lit", TEXTURED_ADDITIVE_LIT_PIPE, false, true);
    static final RenderType TEXTURED_ALPHA_LIT = textured("cascade_textured_alpha_lit", TEXTURED_ALPHA_LIT_PIPE, true, true);
    static final RenderType GUI_TEXTURED_ADDITIVE = textured("cascade_gui_textured_additive", GUI_TEXTURED_ADDITIVE_PIPE, false, false);
    static final RenderType GUI_TEXTURED_ALPHA = textured("cascade_gui_textured_alpha", GUI_TEXTURED_ALPHA_PIPE, true, false);
}
