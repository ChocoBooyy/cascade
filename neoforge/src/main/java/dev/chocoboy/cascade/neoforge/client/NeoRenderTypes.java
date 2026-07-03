package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.client.CascadeRenderTypes;
import net.minecraft.client.renderer.RenderType;

// the neoforge-built render types, exposed to the shared code through the CascadeRenderTypes seam
public final class NeoRenderTypes implements CascadeRenderTypes.Provider {

    @Override
    public RenderType additive() {
        return VfxRenderTypes.ADDITIVE;
    }

    @Override
    public RenderType texturedAdditive() {
        return VfxRenderTypes.TEXTURED_ADDITIVE;
    }

    @Override
    public RenderType texturedAlpha() {
        return VfxRenderTypes.TEXTURED_ALPHA;
    }

    @Override
    public RenderType texturedAlphaSoft() {
        return VfxRenderTypes.TEXTURED_ALPHA_SOFT;
    }

    @Override
    public RenderType solid() {
        return VfxRenderTypes.SOLID;
    }

    @Override
    public RenderType solidLit() {
        return VfxRenderTypes.SOLID_LIT;
    }

    @Override
    public RenderType texturedAdditiveLit() {
        return VfxRenderTypes.TEXTURED_ADDITIVE_LIT;
    }

    @Override
    public RenderType texturedAlphaLit() {
        return VfxRenderTypes.TEXTURED_ALPHA_LIT;
    }

    @Override
    public RenderType texturedAlphaLitSoft() {
        return VfxRenderTypes.TEXTURED_ALPHA_LIT_SOFT;
    }

    @Override
    public RenderType guiTexturedAdditive() {
        return VfxRenderTypes.GUI_TEXTURED_ADDITIVE;
    }

    @Override
    public RenderType guiTexturedAlpha() {
        return VfxRenderTypes.GUI_TEXTURED_ALPHA;
    }
}
