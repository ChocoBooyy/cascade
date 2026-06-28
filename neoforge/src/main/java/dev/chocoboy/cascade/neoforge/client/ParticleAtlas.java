package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.chocoboy.cascade.CascadeCommon;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.math.Noise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

// builds the particle sprite sheet in code so the library ships no image assets. Four 64px cells in a
// 2x2 grid: a soft glow, a noisy smoke puff, a tight spark, and a hollow ring. All white so the vertex
// color tints them. Built once, lazily, on the render thread the first time a particle wants it.
final class ParticleAtlas {

    private static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath(CascadeCommon.MOD_ID, "particle_atlas");

    private static final int CELL = 64;
    private static final int COLS = 2;
    private static final int SIZE = CELL * COLS;
    // half a texel, to keep nearest sampling from bleeding across cell seams
    private static final float INSET = 0.5f / SIZE;

    private static boolean uploaded;

    private ParticleAtlas() {
    }

    // the texture id, for binding a render type. Pure constant, safe at class load.
    static ResourceLocation textureId() {
        return LOCATION;
    }

    // builds and registers the atlas on first call. Must run on the render thread.
    static void ensureUploaded() {
        if (!uploaded) {
            upload();
            uploaded = true;
        }
    }

    // u0, v0, u1, v1 of the cell, inset so quads sample only their own sprite
    static float[] uv(SpriteId sprite) {
        int index = sprite.ordinal();
        int col = index % COLS;
        int row = index / COLS;
        float u0 = (float) col / COLS;
        float v0 = (float) row / COLS;
        return new float[] {u0 + INSET, v0 + INSET, u0 + 1f / COLS - INSET, v0 + 1f / COLS - INSET};
    }

    private static void upload() {
        NativeImage image = new NativeImage(SIZE, SIZE, true);
        paintCell(image, SpriteId.GLOW);
        paintCell(image, SpriteId.SMOKE);
        paintCell(image, SpriteId.SPARK);
        paintCell(image, SpriteId.RING);
        Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(image));
    }

    private static void paintCell(NativeImage image, SpriteId sprite) {
        int ox = (sprite.ordinal() % COLS) * CELL;
        int oy = (sprite.ordinal() / COLS) * CELL;
        for (int py = 0; py < CELL; py++) {
            for (int px = 0; px < CELL; px++) {
                float nx = (px + 0.5f) / (CELL / 2f) - 1f;
                float ny = (py + 0.5f) / (CELL / 2f) - 1f;
                float r = (float) Math.sqrt(nx * nx + ny * ny);
                int a = alpha(sprite, r, px, py);
                image.setPixelRGBA(ox + px, oy + py, (a << 24) | 0x00FFFFFF);
            }
        }
    }

    private static int alpha(SpriteId sprite, float r, int px, int py) {
        float a = switch (sprite) {
            case GLOW -> (float) Math.exp(-(r * r) * 4f);
            case SPARK -> (float) Math.exp(-(r * r) * 16f);
            case RING -> (float) Math.exp(-((r - 0.6f) * (r - 0.6f)) * 40f);
            case SMOKE -> {
                float disc = Math.max(0f, 1f - r);
                disc *= disc;
                float n = 0.5f + 0.5f * Noise.value(px * 0.18f, py * 0.18f, 0f);
                yield disc * (0.4f + 0.6f * n);
            }
        };
        return Math.round(Math.min(1f, Math.max(0f, a)) * 255f);
    }
}
