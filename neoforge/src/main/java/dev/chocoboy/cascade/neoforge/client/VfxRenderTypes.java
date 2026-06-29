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

    // lit twins of the textured types, tinted by the world lightmap so particles sit in scene lighting
    static final RenderType TEXTURED_ADDITIVE_LIT = litTextured("cascade_textured_additive_lit",
            RenderStateShard.ADDITIVE_TRANSPARENCY);

    static final RenderType TEXTURED_ALPHA_LIT = litTextured("cascade_textured_alpha_lit",
            RenderStateShard.TRANSLUCENT_TRANSPARENCY);

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
