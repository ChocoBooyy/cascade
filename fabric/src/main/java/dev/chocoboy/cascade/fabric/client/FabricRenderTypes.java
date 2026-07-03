package dev.chocoboy.cascade.fabric.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.chocoboy.cascade.client.CascadeRenderTypes;
import dev.chocoboy.cascade.client.CascadeShaders;
import dev.chocoboy.cascade.client.ParticleAtlas;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

// the fabric-built render types, matching the neoforge set. the construction touches vanilla render state
// that this loader reaches through its access widener; the shared code only sees the seam
public final class FabricRenderTypes implements CascadeRenderTypes.Provider {

    private static final RenderStateShard.ShaderStateShard POSITION_TEX_COLOR =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);

    private static final RenderStateShard.ShaderStateShard PARTICLE =
            new RenderStateShard.ShaderStateShard(GameRenderer::getParticleShader);

    private static final RenderStateShard.ShaderStateShard POSITION_COLOR_LIGHTMAP =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader);

    private static final RenderStateShard.ShaderStateShard SOFT_SHADER =
            new RenderStateShard.ShaderStateShard(CascadeShaders::soft);

    private static final RenderStateShard.ShaderStateShard SOFT_LIT_SHADER =
            new RenderStateShard.ShaderStateShard(CascadeShaders::softLit);

    private static final RenderType ADDITIVE = RenderType.create(
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

    private static final RenderType TEXTURED_ADDITIVE = textured("cascade_textured_additive",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    private static final RenderType TEXTURED_ALPHA = textured("cascade_textured_alpha",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

    private static final RenderType TEXTURED_ALPHA_SOFT = RenderType.create(
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

    private static final RenderType SOLID = RenderType.create(
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

    private static final RenderType SOLID_LIT = RenderType.create(
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

    private static final RenderType TEXTURED_ADDITIVE_LIT = litTextured("cascade_textured_additive_lit",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    private static final RenderType TEXTURED_ALPHA_LIT = litTextured("cascade_textured_alpha_lit",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

    private static final RenderType TEXTURED_ALPHA_LIT_SOFT = RenderType.create(
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

    private static final RenderType GUI_TEXTURED_ADDITIVE = guiTextured("cascade_gui_textured_additive",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    private static final RenderType GUI_TEXTURED_ALPHA = guiTextured("cascade_gui_textured_alpha",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

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

    // gui twins: NO_DEPTH_TEST so the hud pass draws them regardless of leftover scene depth, no depth write
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
        return TEXTURED_ALPHA_SOFT;
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
        return TEXTURED_ALPHA_LIT_SOFT;
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
