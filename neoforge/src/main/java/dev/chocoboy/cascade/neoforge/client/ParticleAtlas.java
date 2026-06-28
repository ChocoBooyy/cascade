package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.chocoboy.cascade.CascadeCommon;
import dev.chocoboy.cascade.engine.effect.SpriteId;
import dev.chocoboy.cascade.engine.math.Noise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

// builds the particle sprite sheet in code so the library ships no image assets. The sheet is a grid of
// FRAMES columns by one row per SpriteId, each cell 64px and all white so the vertex color tints it.
// Frame 0 is the still sprite; the later frames animate it (smoke boils and thins, the spark twinkles
// out, the ring expands like a shockwave) so a system can flipbook across them over a particle's life.
// Built once, lazily, on the render thread the first time a particle wants it.
final class ParticleAtlas {

    static final int FRAMES = 4;

    private static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath(CascadeCommon.MOD_ID, "particle_atlas");

    private static final int CELL = 64;
    private static final int SPRITES = SpriteId.values().length;
    private static final int WIDTH = CELL * FRAMES;
    private static final int HEIGHT = CELL * SPRITES;
    // half a texel, to keep nearest sampling from bleeding across cell seams
    private static final float INSET_U = 0.5f / WIDTH;
    private static final float INSET_V = 0.5f / HEIGHT;

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

    static float[] uv(SpriteId sprite) {
        return uv(sprite, 0);
    }

    // u0, v0, u1, v1 of one frame's cell, inset so quads sample only their own sprite
    static float[] uv(SpriteId sprite, int frame) {
        float u0 = (float) frame / FRAMES;
        float v0 = (float) sprite.ordinal() / SPRITES;
        return new float[] {u0 + INSET_U, v0 + INSET_V, u0 + 1f / FRAMES - INSET_U, v0 + 1f / SPRITES - INSET_V};
    }

    private static void upload() {
        NativeImage image = new NativeImage(WIDTH, HEIGHT, true);
        for (SpriteId sprite : SpriteId.values()) {
            for (int frame = 0; frame < FRAMES; frame++) {
                paintCell(image, sprite, frame);
            }
        }
        Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(image));
    }

    private static void paintCell(NativeImage image, SpriteId sprite, int frame) {
        int ox = frame * CELL;
        int oy = sprite.ordinal() * CELL;
        float t = FRAMES <= 1 ? 0f : (float) frame / (FRAMES - 1);
        for (int py = 0; py < CELL; py++) {
            for (int px = 0; px < CELL; px++) {
                float nx = (px + 0.5f) / (CELL / 2f) - 1f;
                float ny = (py + 0.5f) / (CELL / 2f) - 1f;
                float r = (float) Math.sqrt(nx * nx + ny * ny);
                int a = alpha(sprite, r, px, py, t);
                image.setPixelRGBA(ox + px, oy + py, (a << 24) | 0x00FFFFFF);
            }
        }
    }

    private static int alpha(SpriteId sprite, float r, int px, int py, float t) {
        float a = switch (sprite) {
            case GLOW -> (float) Math.exp(-(r * r) * 4f);
            case SPARK -> (float) Math.exp(-(r * r) * 16f) * (1f - 0.7f * t);
            case RING -> {
                float center = 0.2f + 0.6f * t;
                yield (float) Math.exp(-((r - center) * (r - center)) * 40f) * (1f - 0.4f * t);
            }
            case SMOKE -> {
                float disc = Math.max(0f, 1f - r);
                disc *= disc;
                float n = 0.5f + 0.5f * Noise.value(px * 0.18f, py * 0.18f, t * 4f);
                yield disc * (0.4f + 0.6f * n) * (1f - 0.3f * t);
            }
        };
        return Math.round(Math.min(1f, Math.max(0f, a)) * 255f);
    }
}
