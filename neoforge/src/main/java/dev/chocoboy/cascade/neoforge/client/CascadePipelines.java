package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import dev.chocoboy.cascade.client.BloomBlit;
import dev.chocoboy.cascade.client.ScreenVfxPipelines;
import dev.chocoboy.cascade.client.SdfFx;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

// the render pipelines for the cascade render types, kept apart from VfxRenderTypes on purpose. the gpu
// device compiles pipelines at registration time, which happens during client startup, before the game
// loop; VfxRenderTypes, by contrast, builds its RenderTypes lazily on the first draw so the particle atlas
// is registered before a RenderSetup resolves it. touching this class at registration must not drag the
// RenderTypes (and the atlas resolve) along with it, so the two live in separate classes.
final class CascadePipelines {

    static final DepthStencilState DEPTH_NO_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false);
    static final DepthStencilState DEPTH_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);

    private static final Identifier POSITION_TEX_COLOR = Identifier.withDefaultNamespace("core/position_tex_color");

    // the pre-26.1 position_tex_color fragment stage, vendored: it discarded texels below alpha 0.1 where
    // the 26.1 one only discards exact zero. cascade's sprites carry their shape in the alpha channel over
    // white rgb and blend one-one, so without that cutout the faint halo tail of a soft sprite like GLOW
    // adds full white across its whole quad and reads as a square
    private static final Identifier POSITION_TEX_CUTOUT =
            Identifier.fromNamespaceAndPath("cascade", "core/position_tex_cutout");

    // the soft particle fragment stage: fades alpha where the quad nears scene geometry, read from
    // cascade's depth copy bound as DepthSampler. one fragment serves the lit and unlit soft pipelines
    private static final Identifier POSITION_TEX_SOFT =
            Identifier.fromNamespaceAndPath("cascade", "core/position_tex_soft");

    private CascadePipelines() {
    }

    private static RenderPipeline derived(String name, RenderPipeline base, VertexFormat format,
            ColorTargetState color, DepthStencilState depth) {
        return base.toBuilder()
                .withLocation(Identifier.fromNamespaceAndPath("cascade", name))
                .withVertexFormat(format, VertexFormat.Mode.QUADS)
                .withColorTargetState(color)
                .withDepthStencilState(depth)
                .withCull(false)
                .build();
    }

    // the unlit textured world types are built from scratch: the closest stock textured pipeline, gui_textured,
    // targets the orthographic hud and does not sample a bound world texture, so it drew sprites as flat quads
    private static RenderPipeline textured(String name, ColorTargetState color, DepthStencilState depth) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("cascade", name))
                .withVertexShader(POSITION_TEX_COLOR)
                .withFragmentShader(POSITION_TEX_CUTOUT)
                .withSampler("Sampler0")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .withColorTargetState(color)
                .withDepthStencilState(depth)
                .withCull(false)
                .build();
    }

    // untextured additive quads for beams: additive blend, depth tested so terrain occludes, no depth write
    static final RenderPipeline ADDITIVE = derived("additive",
            RenderPipelines.DEBUG_QUADS, DefaultVertexFormat.POSITION_COLOR,
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    // textured atlas sprites in world, additive (glows) and translucent (smoke); depth tested, no depth write
    static final RenderPipeline TEXTURED_ADDITIVE = textured("textured_additive",
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    static final RenderPipeline TEXTURED_ALPHA = textured("textured_alpha",
            new ColorTargetState(BlendFunction.TRANSLUCENT), DEPTH_NO_WRITE);

    // double sided solid geometry for mesh particles: opaque, depth tested and written so cubes read as 3D
    static final RenderPipeline SOLID = derived("solid",
            RenderPipelines.DEBUG_QUADS, DefaultVertexFormat.POSITION_COLOR,
            ColorTargetState.DEFAULT, DEPTH_WRITE);

    // its lit twin: vanilla dropped position_color_lightmap in 26.1, so a vendored vertex stage folds the
    // lightmap into the vertex color and the mesh takes scene light
    static final RenderPipeline SOLID_LIT = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("cascade", "solid_lit"))
            .withVertexShader(Identifier.fromNamespaceAndPath("cascade", "core/position_color_lightmap"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("cascade", "core/position_color_lightmap"))
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DEPTH_WRITE)
            .withCull(false)
            .build();

    // lit textured sprites use the particle format and shader, so the world lightmap tints them
    static final RenderPipeline TEXTURED_ADDITIVE_LIT = derived("textured_additive_lit",
            RenderPipelines.TRANSLUCENT_PARTICLE, DefaultVertexFormat.PARTICLE,
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    static final RenderPipeline TEXTURED_ALPHA_LIT = derived("textured_alpha_lit",
            RenderPipelines.TRANSLUCENT_PARTICLE, DefaultVertexFormat.PARTICLE,
            new ColorTargetState(BlendFunction.TRANSLUCENT), DEPTH_NO_WRITE);

    // soft twins of the alpha types: the soft fragment stage fades the quad near scene geometry, reading
    // cascade's depth copy through the extra DepthSampler
    static final RenderPipeline TEXTURED_ALPHA_SOFT =
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("cascade", "textured_alpha_soft"))
                    .withVertexShader(POSITION_TEX_COLOR)
                    .withFragmentShader(POSITION_TEX_SOFT)
                    .withSampler("Sampler0")
                    .withSampler("DepthSampler")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(DEPTH_NO_WRITE)
                    .withCull(false)
                    .build();

    static final RenderPipeline TEXTURED_ALPHA_LIT_SOFT = RenderPipelines.TRANSLUCENT_PARTICLE.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("cascade", "textured_alpha_lit_soft"))
            .withFragmentShader(POSITION_TEX_SOFT)
            .withSampler("DepthSampler")
            .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DEPTH_NO_WRITE)
            .withCull(false)
            .build();

    private static final List<RenderPipeline> ALL = List.of(
            ADDITIVE, TEXTURED_ADDITIVE, TEXTURED_ALPHA, SOLID, SOLID_LIT,
            TEXTURED_ADDITIVE_LIT, TEXTURED_ALPHA_LIT,
            TEXTURED_ALPHA_SOFT, TEXTURED_ALPHA_LIT_SOFT,
            ScreenVfxPipelines.ADDITIVE, ScreenVfxPipelines.ALPHA,
            BloomBlit.PIPELINE, SdfFx.PIPELINE);

    // the gpu device only compiles pipelines it knows about, so every custom pipeline is registered here
    static void register(RegisterRenderPipelinesEvent event) {
        ALL.forEach(event::registerPipeline);
    }
}
