package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.client.ParticleAtlas;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

// the neoforge render types, rebuilt for 26.1. the old RenderStateShard composite builder is gone, so each
// type is a RenderPipeline (see CascadePipelines) paired with a RenderSetup that binds the particle atlas.
//
// this class is loaded lazily, on the first draw that asks for a type, never at pipeline registration. that
// matters: a RenderSetup resolves its bound texture to a gpu view when it is built and caches it, so the
// atlas must already be registered by then. the static block below uploads it first, on the render thread,
// before any textured type is built. building these at registration instead would resolve the atlas before
// it exists and cache a blank texture, drawing every sprite as a flat coloured quad.
//
// first cut of the port: the soft (depth-fading) and lightmap-lit variants are not rebuilt yet, so the
// provider maps them onto their hard-edged and unlit twins. see the porting notes
final class VfxRenderTypes {

    private static final int BUFFER_BYTES = 1536;

    // upload the atlas before the textured types below resolve it. runs first because it is declared first
    static {
        ParticleAtlas.ensureUploaded();
    }

    // the atlas is a single mip level built for nearest sampling, so it must be bound with a nearest,
    // no-mipmap sampler. the default sampler mipmaps, and sampling a missing mip reads as white, which drew
    // every sprite as a flat vertex-coloured quad. clamp to edge so a cell cannot bleed into its neighbour.
    private static final java.util.function.Supplier<com.mojang.blaze3d.textures.GpuSampler> ATLAS_SAMPLER = () ->
            com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getSampler(
                    com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                    com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                    com.mojang.blaze3d.textures.FilterMode.NEAREST,
                    com.mojang.blaze3d.textures.FilterMode.NEAREST,
                    false);

    private VfxRenderTypes() {
    }

    // an untextured type: just the pipeline and a vertex buffer
    private static RenderType plain(String name, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, boolean sort) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).bufferSize(BUFFER_BYTES);
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }

    // a textured type: binds the particle atlas to Sampler0, and the lightmap too when the shader is lit
    private static RenderType textured(String name, com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
            boolean sort, boolean lightmap) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", ParticleAtlas.textureId(), ATLAS_SAMPLER)
                .bufferSize(BUFFER_BYTES);
        if (lightmap) {
            setup.useLightmap();
        }
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }

    static final RenderType ADDITIVE = plain("cascade_additive", CascadePipelines.ADDITIVE, false);
    static final RenderType SOLID = plain("cascade_solid", CascadePipelines.SOLID, false);
    static final RenderType TEXTURED_ADDITIVE = textured("cascade_textured_additive", CascadePipelines.TEXTURED_ADDITIVE, false, false);
    static final RenderType TEXTURED_ALPHA = textured("cascade_textured_alpha", CascadePipelines.TEXTURED_ALPHA, true, false);
    static final RenderType TEXTURED_ADDITIVE_LIT = textured("cascade_textured_additive_lit", CascadePipelines.TEXTURED_ADDITIVE_LIT, false, true);
    static final RenderType TEXTURED_ALPHA_LIT = textured("cascade_textured_alpha_lit", CascadePipelines.TEXTURED_ALPHA_LIT, true, true);
    static final RenderType GUI_TEXTURED_ADDITIVE = textured("cascade_gui_textured_additive", CascadePipelines.GUI_TEXTURED_ADDITIVE, false, false);
    static final RenderType GUI_TEXTURED_ALPHA = textured("cascade_gui_textured_alpha", CascadePipelines.GUI_TEXTURED_ALPHA, true, false);
}
