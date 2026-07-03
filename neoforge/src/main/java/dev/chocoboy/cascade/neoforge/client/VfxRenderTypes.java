package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

final class VfxRenderTypes {

    // no predefined position-tex-color shard exists, so wrap the matching core shader once
    private static final RenderStateShard.ShaderStateShard POSITION_TEX_COLOR =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);

    // the vanilla particle shader samples the lightmap, which is what we want for lit particles
    private static final RenderStateShard.ShaderStateShard PARTICLE =
            new RenderStateShard.ShaderStateShard(GameRenderer::getParticleShader);

    private VfxRenderTypes() {
    }

    // main-target additive POSITION_COLOR. Depth tested so effects are occluded by terrain, but no
    // depth write (COLOR_WRITE) so overlapping particles blend instead of fighting. Used by beams.
    static final RenderType ADDITIVE = RenderType.create(
            "cascade_additive",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    // textured atlas sprites, additive: glows that brighten what is behind them (fire, sparks, magic)
    static final RenderType TEXTURED_ADDITIVE = textured("cascade_textured_additive",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    // textured atlas sprites, alpha blended: occlude what is behind them (smoke, dust)
    static final RenderType TEXTURED_ALPHA = textured("cascade_textured_alpha",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

    // soft twin of TEXTURED_ALPHA: same alpha blend, but the cascade_soft core shader fades the quad out
    // where it nears scene geometry, reading the depth copy bound each frame, so smoke has no hard clip line
    private static final RenderStateShard.ShaderStateShard SOFT_SHADER =
            new RenderStateShard.ShaderStateShard(CascadeShaders::soft);

    static final RenderType TEXTURED_ALPHA_SOFT = RenderType.create(
            "cascade_textured_alpha_soft",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(SOFT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ParticleAtlas.textureId(), false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private static RenderType textured(String name, RenderStateShard.TransparencyStateShard transparency) {
        return RenderType.create(
                name,
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_TEX_COLOR)
                        .setTextureState(new RenderStateShard.TextureStateShard(ParticleAtlas.textureId(), false, false))
                        .setTransparencyState(transparency)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    // gui twins of the textured types: NO_DEPTH_TEST so the hud pass draws them over whatever scene depth is
    // left in the buffer, and no depth write so they never disturb it for the rest of the gui
    static final RenderType GUI_TEXTURED_ADDITIVE = guiTextured("cascade_gui_textured_additive",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    static final RenderType GUI_TEXTURED_ALPHA = guiTextured("cascade_gui_textured_alpha",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

    private static RenderType guiTextured(String name, RenderStateShard.TransparencyStateShard transparency) {
        return RenderType.create(
                name,
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_TEX_COLOR)
                        .setTextureState(new RenderStateShard.TextureStateShard(ParticleAtlas.textureId(), false, false))
                        .setTransparencyState(transparency)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    // solid double sided geometry for mesh particles. NO_CULL so a winding mistake cannot hide a face.
    // COLOR_DEPTH_WRITE and LEQUAL depth so cubes occlude correctly and read as solid 3D
    static final RenderType SOLID = RenderType.create(
            "cascade_solid",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    private static final RenderStateShard.ShaderStateShard POSITION_COLOR_LIGHTMAP =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader);

    // lit twin: POSITION_COLOR_LIGHTMAP carries a lightmap coord per vertex so debris sits in scene light
    static final RenderType SOLID_LIT = RenderType.create(
            "cascade_solid_lit",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_LIGHTMAP)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    // lit twins of the textured types, tinted by the world lightmap so particles sit in scene lighting
    static final RenderType TEXTURED_ADDITIVE_LIT = litTextured("cascade_textured_additive_lit",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    static final RenderType TEXTURED_ALPHA_LIT = litTextured("cascade_textured_alpha_lit",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

    // soft twin of TEXTURED_ALPHA_LIT: PARTICLE format so the lightmap tints the vertex color, plus the
    // cascade_soft_lit core shader fades the quad where it nears geometry. LIGHTMAP supplies Sampler2; the
    // DepthSampler is bound per frame in VfxRenderManager, exactly like the unlit soft type
    private static final RenderStateShard.ShaderStateShard SOFT_LIT_SHADER =
            new RenderStateShard.ShaderStateShard(CascadeShaders::softLit);

    static final RenderType TEXTURED_ALPHA_LIT_SOFT = RenderType.create(
            "cascade_textured_alpha_lit_soft",
            DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(SOFT_LIT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(ParticleAtlas.textureId(), false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    // PARTICLE format carries a lightmap coord per vertex; the particle shader samples it
    private static RenderType litTextured(String name, RenderStateShard.TransparencyStateShard transparency) {
        return RenderType.create(
                name,
                DefaultVertexFormat.PARTICLE,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(PARTICLE)
                        .setTextureState(new RenderStateShard.TextureStateShard(ParticleAtlas.textureId(), false, false))
                        .setTransparencyState(transparency)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }
}
