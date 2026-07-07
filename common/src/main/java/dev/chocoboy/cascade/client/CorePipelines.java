package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.resources.Identifier;

// the render pipelines for the cascade render types, kept apart from CoreRenderTypes on purpose. the gpu
// device compiles pipelines lazily on first draw (or upfront where a loader pre-registers them), while
// CoreRenderTypes builds its RenderTypes on the first draw so the particle atlas is registered before a
// RenderSetup resolves it; touching this class must not drag the RenderTypes along, so the two live in
// separate classes.
//
// everything here is built from scratch on the public vanilla builder: the vanilla snippets and the
// toBuilder derivation are loader-widened api (neoforge access transformers) that fabric cannot see, so
// the uniform blocks and shader stages the snippets would contribute are declared explicitly instead,
// matching the vanilla pipelines they mirror (position_color for the untextured types, particle for the
// lit ones)
public final class CorePipelines {

    static final DepthStencilState DEPTH_NO_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false);
    static final DepthStencilState DEPTH_WRITE = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);

    private static final Identifier POSITION_COLOR = Identifier.withDefaultNamespace("core/position_color");
    private static final Identifier POSITION_TEX_COLOR = Identifier.withDefaultNamespace("core/position_tex_color");
    private static final Identifier PARTICLE = Identifier.withDefaultNamespace("core/particle");

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

    private CorePipelines() {
    }

    // the world matrix and projection blocks every world pipeline reads, vanilla's matrices snippet inlined
    static RenderPipeline.Builder worldBuilder(String name) {
        return RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("cascade", name))
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER);
    }

    // untextured vertex-colored quads, the vanilla position_color stages
    private static RenderPipeline plain(String name, ColorTargetState color, DepthStencilState depth) {
        return worldBuilder(name)
                .withVertexShader(POSITION_COLOR)
                .withFragmentShader(POSITION_COLOR)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                .withColorTargetState(color)
                .withDepthStencilState(depth)
                .withCull(false)
                .build();
    }

    private static RenderPipeline textured(String name, ColorTargetState color, DepthStencilState depth) {
        return worldBuilder(name)
                .withVertexShader(POSITION_TEX_COLOR)
                .withFragmentShader(POSITION_TEX_CUTOUT)
                .withSampler("Sampler0")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .withColorTargetState(color)
                .withDepthStencilState(depth)
                .withCull(false)
                .build();
    }

    // lit sprites ride the vanilla particle stages, whose vertex shader folds the lightmap into the vertex
    // color; the fog block is part of that shader's interface even though cascade leaves fog defaults alone
    private static RenderPipeline.Builder litBuilder(String name) {
        return worldBuilder(name)
                .withUniform("Fog", UniformType.UNIFORM_BUFFER)
                .withVertexShader(PARTICLE)
                .withSampler("Sampler0")
                .withSampler("Sampler2")
                .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
                .withDepthStencilState(DEPTH_NO_WRITE)
                .withCull(false);
    }

    // untextured additive quads for beams: additive blend, depth tested so terrain occludes, no depth write
    static final RenderPipeline ADDITIVE = plain("additive",
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    // textured atlas sprites in world, additive (glows) and translucent (smoke); depth tested, no depth write
    static final RenderPipeline TEXTURED_ADDITIVE = textured("textured_additive",
            new ColorTargetState(BlendFunction.ADDITIVE), DEPTH_NO_WRITE);

    static final RenderPipeline TEXTURED_ALPHA = textured("textured_alpha",
            new ColorTargetState(BlendFunction.TRANSLUCENT), DEPTH_NO_WRITE);

    // double sided solid geometry for mesh particles: opaque, depth tested and written so cubes read as 3D
    static final RenderPipeline SOLID = plain("solid", ColorTargetState.DEFAULT, DEPTH_WRITE);

    // its lit twin: vanilla dropped position_color_lightmap in 26.1, so a vendored vertex stage folds the
    // lightmap into the vertex color and the mesh takes scene light
    static final RenderPipeline SOLID_LIT = worldBuilder("solid_lit")
            .withVertexShader(Identifier.fromNamespaceAndPath("cascade", "core/position_color_lightmap"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("cascade", "core/position_color_lightmap"))
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DEPTH_WRITE)
            .withCull(false)
            .build();

    static final RenderPipeline TEXTURED_ADDITIVE_LIT = litBuilder("textured_additive_lit")
            .withFragmentShader(PARTICLE)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .build();

    static final RenderPipeline TEXTURED_ALPHA_LIT = litBuilder("textured_alpha_lit")
            .withFragmentShader(PARTICLE)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    // soft twins of the alpha types: the soft fragment stage fades the quad near scene geometry, reading
    // cascade's depth copy through the extra DepthSampler
    static final RenderPipeline TEXTURED_ALPHA_SOFT = worldBuilder("textured_alpha_soft")
            .withVertexShader(POSITION_TEX_COLOR)
            .withFragmentShader(POSITION_TEX_SOFT)
            .withSampler("Sampler0")
            .withSampler("DepthSampler")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DEPTH_NO_WRITE)
            .withCull(false)
            .build();

    static final RenderPipeline TEXTURED_ALPHA_LIT_SOFT = litBuilder("textured_alpha_lit_soft")
            .withFragmentShader(POSITION_TEX_SOFT)
            .withSampler("DepthSampler")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    private static final List<RenderPipeline> ALL = List.of(
            ADDITIVE, TEXTURED_ADDITIVE, TEXTURED_ALPHA, SOLID, SOLID_LIT,
            TEXTURED_ADDITIVE_LIT, TEXTURED_ALPHA_LIT,
            TEXTURED_ALPHA_SOFT, TEXTURED_ALPHA_LIT_SOFT,
            ScreenVfxPipelines.ADDITIVE, ScreenVfxPipelines.ALPHA,
            BloomBlit.PIPELINE, SdfFx.PIPELINE, GpuSim.PIPELINE);

    // the gpu device compiles pipelines lazily on first draw; a loader that can pre-register them for an
    // upfront compile (neoforge's register event) feeds this list through
    public static List<RenderPipeline> all() {
        return ALL;
    }
}
