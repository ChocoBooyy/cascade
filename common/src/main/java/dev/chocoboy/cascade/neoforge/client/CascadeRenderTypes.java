package dev.chocoboy.cascade.neoforge.client;

import net.minecraft.client.renderer.RenderType;

// building a custom RenderType touches protected vanilla render state that each loader widens its own way,
// so the shared render code asks for its types through this seam and the loader installs the real set
public final class CascadeRenderTypes {

    private static Provider provider;

    private CascadeRenderTypes() {
    }

    public interface Provider {
        RenderType additive();

        RenderType texturedAdditive();

        RenderType texturedAlpha();

        RenderType texturedAlphaSoft();

        RenderType solid();

        RenderType solidLit();

        RenderType texturedAdditiveLit();

        RenderType texturedAlphaLit();

        RenderType texturedAlphaLitSoft();
    }

    public static void install(Provider p) {
        provider = p;
    }

    public static RenderType additive() {
        return provider.additive();
    }

    public static RenderType texturedAdditive() {
        return provider.texturedAdditive();
    }

    public static RenderType texturedAlpha() {
        return provider.texturedAlpha();
    }

    public static RenderType texturedAlphaSoft() {
        return provider.texturedAlphaSoft();
    }

    public static RenderType solid() {
        return provider.solid();
    }

    public static RenderType solidLit() {
        return provider.solidLit();
    }

    public static RenderType texturedAdditiveLit() {
        return provider.texturedAdditiveLit();
    }

    public static RenderType texturedAlphaLit() {
        return provider.texturedAlphaLit();
    }

    public static RenderType texturedAlphaLitSoft() {
        return provider.texturedAlphaLitSoft();
    }
}
