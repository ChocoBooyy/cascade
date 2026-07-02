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
public final class ParticleAtlas {

    static final int FRAMES = 4;

    private static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath(CascadeCommon.MOD_ID, "particle_atlas");

    private static final int CELL = 128;
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
    public static ResourceLocation textureId() {
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
                int a = alpha(sprite, nx, ny, r, t);
                image.setPixelRGBA(ox + px, oy + py, (a << 24) | 0x00FFFFFF);
            }
        }
    }

    // alpha is shaped in normalized [-1,1] space so it is resolution independent. RGB is white, the vertex
    // color tints it, so the silhouette is everything: a hot core that saturates and blooms under additive
    // blend, diffraction spikes, crisp shells, faceted edges.
    private static int alpha(SpriteId sprite, float nx, float ny, float r, float t) {
        float a = switch (sprite) {
            case GLOW -> gauss(r, 18f) + gauss(r, 2.2f) * 0.5f;
            case SPARK -> {
                float core = gauss(r, 40f);
                float spikes = cross(nx, ny, 500f) * 0.5f + diagonal(nx, ny, 500f) * 0.22f;
                yield core + spikes * (1f - 0.6f * t);
            }
            case RING -> {
                // a thin, crisp shell that expands across the frames like a shockwave
                float center = 0.25f + 0.65f * t;
                yield gauss(r - center, 90f) * (1f - 0.5f * t);
            }
            case SMOKE -> {
                float disc = Math.max(0f, 1f - r * 0.85f);
                disc *= disc;
                float fx = (nx + 1f) * 2.5f;
                float fy = (ny + 1f) * 2.5f;
                float turb = 0.6f * (0.5f + 0.5f * Noise.value(fx, fy, t * 3f))
                        + 0.4f * (0.5f + 0.5f * Noise.value(fx * 2.1f, fy * 2.1f, 5f + t * 3f));
                yield disc * (0.55f + 0.45f * turb) * (1f - 0.3f * t);
            }
            case STAR -> {
                // a lens flare: bright core, wide halo, and long anamorphic streaks that twinkle out
                float core = gauss(r, 22f);
                float halo = gauss(r, 1.6f) * 0.4f;
                float streaks = cross(nx, ny, 240f) * 0.85f;
                yield core + halo + streaks * (1f - 0.4f * t);
            }
            case SHARD -> {
                // a faceted crystal: a sharp diamond edge, a soft fill, and a seam highlight down the middle
                float diamond = Math.abs(nx) + Math.abs(ny);
                float size = 0.85f;
                float rim = gauss(diamond - size, 70f);
                float fill = Math.max(0f, 1f - diamond / size);
                fill *= fill;
                float seam = gauss(nx, 120f) * fill;
                yield (rim * 0.9f + fill * 0.3f + seam * 0.4f) * (1f - 0.3f * t);
            }
        };
        return Math.round(Math.min(1f, Math.max(0f, a)) * 255f);
    }

    private static float gauss(float d, float hardness) {
        return (float) Math.exp(-(d * d) * hardness);
    }

    // a four point flare: thin spikes along the axes that fade out toward the cell edge
    private static float cross(float nx, float ny, float hardness) {
        float h = gauss(ny, hardness) * Math.max(0f, 1f - Math.abs(nx) * 0.9f);
        float v = gauss(nx, hardness) * Math.max(0f, 1f - Math.abs(ny) * 0.9f);
        return h + v;
    }

    // the same flare rotated 45 degrees, for the in-between sparkle points
    private static float diagonal(float nx, float ny, float hardness) {
        float du = (nx + ny) * 0.70710677f;
        float dv = (nx - ny) * 0.70710677f;
        float d1 = gauss(dv, hardness) * Math.max(0f, 1f - Math.abs(du) * 0.9f);
        float d2 = gauss(du, hardness) * Math.max(0f, 1f - Math.abs(dv) * 0.9f);
        return d1 + d2;
    }
}
