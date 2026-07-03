package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.Particle;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.effect.RenderSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// holds and draws hud-layer particle systems, positioned in gui-scaled coords. loader-agnostic like
// VfxRenderManager: each loader feeds it a client tick and its gui render pass. the engine sim is unitless,
// so screen specs just author in gui units (size 8 is 8 scaled pixels, speed in units per tick)
public final class ScreenVfxManager {

    private static final ScreenVfxManager INSTANCE = new ScreenVfxManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    // screen effects are cosmetic, so cap them rather than risk unbounded growth under spam
    private static final int MAX_EFFECTS = 64;

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

    public void render(GuiGraphics gui) {
        if (active.isEmpty()) {
            return;
        }
        ParticleAtlas.ensureUploaded();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Matrix4f pose = gui.pose().last().pose();
        List<Entry> failed = null;
        try {
            for (Entry entry : active) {
                try {
                    renderEntry(entry, pose, buffers);
                } catch (RuntimeException e) {
                    if (failed == null) {
                        failed = new ArrayList<>();
                    }
                    failed.add(entry);
                    logOnce("rendering a screen effect", e);
                }
            }
        } finally {
            buffers.endBatch();
        }
        if (failed != null) {
            active.removeAll(failed);
        }
    }

    // one rotated quad per particle, flat at z 0. of the render spec only blend, sprite, and animate apply
    // on the hud; stretch, lit, soft, and mesh are world features and are ignored here. engine y maps
    // straight to screen y, which grows down; authors write screen-space specs knowing that
    private void renderEntry(Entry entry, Matrix4f pose, MultiBufferSource.BufferSource buffers) {
        RenderSpec render = entry.spec().render();
        RenderType type = render.blend() == BlendMode.ADDITIVE
                ? CascadeRenderTypes.texturedAdditive()
                : CascadeRenderTypes.texturedAlpha();
        VertexConsumer vc = buffers.getBuffer(type);
        ParticleSystem sim = entry.system();
        boolean animate = render.animate();
        float[] still = ParticleAtlas.uv(render.sprite());
        for (Particle p : sim.particles()) {
            int color = sim.colorOf(p);
            int a = (int) (sim.alphaOf(p) * 255f);
            int cr = (color >> 16) & 0xFF;
            int cg = (color >> 8) & 0xFF;
            int cb = color & 0xFF;
            float s = sim.sizeOf(p) / 2f;
            float cx = entry.x() + p.pos.x();
            float cy = entry.y() + p.pos.y();
            float[] uv = animate ? ParticleAtlas.uv(render.sprite(), frameOf(p)) : still;
            // rotate the corners in plane, the same trick as Billboards. screen y grows down, so the +y
            // corners sample the cell's bottom edge to keep the sprite upright
            float cs = (float) Math.cos(p.rotation);
            float sn = (float) Math.sin(p.rotation);
            vc.addVertex(pose, cx - s * cs + s * sn, cy - s * sn - s * cs, 0f).setUv(uv[0], uv[1]).setColor(cr, cg, cb, a);
            vc.addVertex(pose, cx - s * cs - s * sn, cy - s * sn + s * cs, 0f).setUv(uv[0], uv[3]).setColor(cr, cg, cb, a);
            vc.addVertex(pose, cx + s * cs - s * sn, cy + s * sn + s * cs, 0f).setUv(uv[2], uv[3]).setColor(cr, cg, cb, a);
            vc.addVertex(pose, cx + s * cs + s * sn, cy + s * sn - s * cs, 0f).setUv(uv[2], uv[1]).setColor(cr, cg, cb, a);
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
