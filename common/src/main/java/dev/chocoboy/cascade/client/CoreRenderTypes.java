package dev.chocoboy.cascade.client;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

// the render types, rebuilt for 26.1 and shared by both loaders: the old RenderStateShard composite builder
// is gone, so each type is a RenderPipeline (see CorePipelines) paired with a RenderSetup that binds the
// particle atlas, all public vanilla api. exposed to the shared render code through the provider seam.
//
// this class is loaded lazily, on the first draw that asks for a type, never at loader init or pipeline
// registration. that matters twice: a RenderSetup resolves its bound texture to a gpu view when it is
// built and caches it, so the atlas must already be registered by then (the static block below uploads it
// first); and the atlas upload itself needs the gpu device, which does not exist yet when loader
// entrypoints install the provider. the nested Provider exists exactly for that: instantiating a nested
// class does not initialize its enclosing class, so this class loads only when the first provider method
// runs, on the first draw.
public final class CoreRenderTypes {

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



    private CoreRenderTypes() {
    }

    // an untextured type: just the pipeline and a vertex buffer
    private static RenderType plain(String name, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, boolean sort) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).bufferSize(BUFFER_BYTES);
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }

    // an untextured lit type: no atlas, but the lightmap bound for the vendored lightmap vertex stage
    private static RenderType lightmapPlain(String name, com.mojang.blaze3d.pipeline.RenderPipeline pipeline) {
        return RenderType.create(name, RenderSetup.builder(pipeline)
                .useLightmap()
                .bufferSize(BUFFER_BYTES)
                .createRenderSetup());
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

    static final RenderType ADDITIVE = plain("cascade_additive", CorePipelines.ADDITIVE, false);
    static final RenderType SOLID = plain("cascade_solid", CorePipelines.SOLID, false);
    static final RenderType SOLID_LIT = lightmapPlain("cascade_solid_lit", CorePipelines.SOLID_LIT);
    static final RenderType TEXTURED_ADDITIVE = textured("cascade_textured_additive", CorePipelines.TEXTURED_ADDITIVE, false, false);
    static final RenderType TEXTURED_ALPHA = textured("cascade_textured_alpha", CorePipelines.TEXTURED_ALPHA, true, false);
    static final RenderType TEXTURED_ADDITIVE_LIT = textured("cascade_textured_additive_lit", CorePipelines.TEXTURED_ADDITIVE_LIT, false, true);
    static final RenderType TEXTURED_ALPHA_LIT = textured("cascade_textured_alpha_lit", CorePipelines.TEXTURED_ALPHA_LIT, true, true);
    static final RenderType GUI_TEXTURED_ADDITIVE = textured("cascade_gui_textured_additive", ScreenVfxPipelines.ADDITIVE, false, false);
    static final RenderType GUI_TEXTURED_ALPHA = textured("cascade_gui_textured_alpha", ScreenVfxPipelines.ALPHA, true, false);

    // the soft types bind cascade's scene depth copy, whose texture is recreated on window resize. a
    // RenderSetup caches the resolved gpu view when built, so these are rebuilt whenever the depth copy's
    // generation moves instead of living as static finals like the types above. the generation is stamped
    // into the name in case type names are registered anywhere unique
    private static RenderType texturedAlphaSoft;
    private static RenderType texturedAlphaLitSoft;
    private static int softGeneration = -1;

    private static RenderType softAlpha() {
        ensureSoft();
        return texturedAlphaSoft;
    }

    private static RenderType softAlphaLit() {
        ensureSoft();
        return texturedAlphaLitSoft;
    }

    private static void ensureSoft() {
        SoftDepth.ensureReady();
        int generation = SoftDepth.generation();
        if (softGeneration != generation) {
            softGeneration = generation;
            texturedAlphaSoft = softTextured("cascade_textured_alpha_soft_" + generation,
                    CorePipelines.TEXTURED_ALPHA_SOFT, false);
            texturedAlphaLitSoft = softTextured("cascade_textured_alpha_lit_soft_" + generation,
                    CorePipelines.TEXTURED_ALPHA_LIT_SOFT, true);
        }
    }

    private static RenderType softTextured(String name, com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
            boolean lightmap) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", ParticleAtlas.textureId(), ATLAS_SAMPLER)
                .withTexture("DepthSampler", SoftDepth.textureId(), ATLAS_SAMPLER)
                .sortOnUpload()
                .bufferSize(BUFFER_BYTES);
        if (lightmap) {
            setup.useLightmap();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }

    // the provider each loader installs at client init. a nested class so constructing it does not
    // class-load the enclosing types; they load when the first method here runs, on the first draw
    public static final class Provider implements CascadeRenderTypes.Provider {

        @Override
        public RenderType additive() {
            return ADDITIVE;
        }

        @Override
        public RenderType texturedAdditive() {
            return TEXTURED_ADDITIVE;
        }

        @Override
        public RenderType texturedAlpha() {
            return TEXTURED_ALPHA;
        }

        @Override
        public RenderType texturedAlphaSoft() {
            return softAlpha();
        }

        @Override
        public RenderType solid() {
            return SOLID;
        }

        @Override
        public RenderType solidLit() {
            return SOLID_LIT;
        }

        @Override
        public RenderType texturedAdditiveLit() {
            return TEXTURED_ADDITIVE_LIT;
        }

        @Override
        public RenderType texturedAlphaLit() {
            return TEXTURED_ALPHA_LIT;
        }

        @Override
        public RenderType texturedAlphaLitSoft() {
            return softAlphaLit();
        }

        @Override
        public RenderType guiTexturedAdditive() {
            return GUI_TEXTURED_ADDITIVE;
        }

        @Override
        public RenderType guiTexturedAlpha() {
            return GUI_TEXTURED_ALPHA;
        }
    }
}
