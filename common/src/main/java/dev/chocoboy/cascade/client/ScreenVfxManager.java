package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// holds and draws hud-layer particle systems, positioned in gui-scaled coords. loader-agnostic like
// VfxRenderManager: each loader feeds it a client tick and its gui render pass. the engine sim is unitless,
// so screen specs just author in gui units (size 8 is 8 scaled pixels, speed in units per tick).
//
// the 26.1 gui is retained, and the only vanilla-public way to feed it custom-pipeline quads is the blit
// family, so each particle draws as one blit of its atlas cell under a pushed 2d pose carrying the
// particle's translation, rotation and scale; the gui renderer still batches blits that share a pipeline.
// the blit takes integer corners, so the quad spans a fixed base extent and the pose scales it to size,
// keeping sub-pixel motion smooth
public final class ScreenVfxManager {


    private static final ScreenVfxManager INSTANCE = new ScreenVfxManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    // screen effects are cosmetic, so cap them rather than risk unbounded growth under spam
    private static final int MAX_EFFECTS = 64;

    // the half extent of the unscaled blit quad; the 2d pose scales it down to the particle size
    private static final int BASE_EXTENT = 64;

    private final List<Entry> active = new ArrayList<>();
    // screen particles are client-local cosmetics, so a plain counter seed is enough; there is no
    // networked spawn to stay deterministic with
    private long seed;
    private boolean loggedError;

    // a live system and the gui-scaled point its particle positions offset from
    private record Entry(ParticleSystem system, EmitterSpec spec, float x, float y) {
    }

    private ScreenVfxManager() {
    }

    public static ScreenVfxManager get() {
        return INSTANCE;
    }

    public void spawn(EmitterSpec spec, float x, float y) {
        if (active.size() >= MAX_EFFECTS) {
            active.remove(0);
        }
        active.add(new Entry(spec.build(new Random(seed++)), spec, x, y));
    }

    public void tick() {
        // spawn requests are dropped: burstOnDeath stays a world feature, the hud keeps one flat list
        active.removeIf(entry -> {
            try {
                return entry.system().tick();
            } catch (RuntimeException e) {
                logOnce("ticking a screen effect", e);
                return true;
            }
        });
    }

    public void render(GuiGraphicsExtractor gui) {
        if (active.isEmpty()) {
            return;
        }
        try {
            ParticleAtlas.ensureUploaded();
            for (Entry entry : active) {
                renderEntry(gui, entry);
            }
        } catch (RuntimeException e) {
            logOnce("rendering a screen effect", e);
        }
    }

    // one blit per particle. of the render spec only blend, sprite, and animate apply on the hud; stretch,
    // lit, soft, and mesh are world features and are ignored here. engine y maps straight to screen y,
    // which grows down; authors write screen-space specs knowing that
    private void renderEntry(GuiGraphicsExtractor gui, Entry entry) {
        RenderSpec render = entry.spec().render();
        RenderPipeline pipeline = render.blend() == BlendMode.ADDITIVE
                ? ScreenVfxPipelines.ADDITIVE
                : ScreenVfxPipelines.ALPHA;
        ParticleSystem sim = entry.system();
        boolean animate = render.animate();
        float[] still = ParticleAtlas.uv(render.sprite());
        int texW = ParticleAtlas.width();
        int texH = ParticleAtlas.height();
        for (Particle p : sim.particles()) {
            int color = sim.colorOf(p);
            int a = (int) (sim.alphaOf(p) * 255f);
            int argb = (a << 24) | (color & 0xFFFFFF);
            float s = sim.sizeOf(p) / 2f;
            if (s <= 0f) {
                continue;
            }
            float[] uv = animate ? ParticleAtlas.uv(render.sprite(), frameOf(p)) : still;
            float u = uv[0] * texW;
            float v = uv[1] * texH;
            int uvW = Math.round((uv[2] - uv[0]) * texW);
            int uvH = Math.round((uv[3] - uv[1]) * texH);
            // the quad spans a fixed base extent and the pose carries position, spin, and size, so the
            // integer blit corners never quantize the motion
            gui.pose().pushMatrix();
            gui.pose().translate(entry.x() + p.pos.x(), entry.y() + p.pos.y());
            gui.pose().rotate(p.rotation);
            gui.pose().scale(s / BASE_EXTENT);
            gui.blit(pipeline, ParticleAtlas.textureId(), -BASE_EXTENT, -BASE_EXTENT,
                    u, v, BASE_EXTENT * 2, BASE_EXTENT * 2, uvW, uvH, texW, texH, argb);
            gui.pose().popMatrix();
        }
    }

    // map a particle's life fraction onto an atlas frame, clamped to the last frame at end of life
    private static int frameOf(Particle p) {
        int frame = (int) (p.life() * ParticleAtlas.FRAMES);
        return frame >= ParticleAtlas.FRAMES ? ParticleAtlas.FRAMES - 1 : frame;
    }

    private void logOnce(String what, RuntimeException e) {
        if (!loggedError) {
            loggedError = true;
            LOGGER.error("Cascade error while {}, further errors suppressed", what, e);
        }
    }
}
