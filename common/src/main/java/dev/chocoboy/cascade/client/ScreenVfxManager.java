package dev.chocoboy.cascade.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// holds and draws hud-layer particle systems, positioned in gui-scaled coords. loader-agnostic like
// VfxRenderManager: each loader feeds it a client tick and its gui render pass. the engine sim is unitless,
// so screen specs just author in gui units (size 8 is 8 scaled pixels, speed in units per tick).
//
// the 26.1 gui is retained: instead of writing quads into a buffer source, each system submits one
// GuiElementRenderState carrying its pipeline and the atlas, and the gui renderer batches and draws it
// with the rest of the hud later in the frame. the vertices are built at that point, still within the
// same frame, so the referenced sim state has not ticked on
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

    public void render(GuiGraphicsExtractor gui) {
        if (active.isEmpty()) {
            return;
        }
        try {
            ParticleAtlas.ensureUploaded();
            TextureSetup atlas = TextureSetup.singleTexture(
                    Minecraft.getInstance().getTextureManager()
                            .getTexture(ParticleAtlas.textureId()).getTextureView(),
                    RenderSystem.getSamplerCache().getSampler(
                            AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                            FilterMode.NEAREST, FilterMode.NEAREST, false));
            ScreenRectangle bounds = new ScreenRectangle(0, 0, gui.guiWidth(), gui.guiHeight());
            Matrix3x2f pose = new Matrix3x2f(gui.pose());
            for (Entry entry : active) {
                RenderPipeline pipeline = entry.spec().render().blend() == BlendMode.ADDITIVE
                        ? ScreenVfxPipelines.ADDITIVE
                        : ScreenVfxPipelines.ALPHA;
                gui.submitGuiElementRenderState(new HudParticles(entry, pipeline, atlas, pose, bounds));
            }
        } catch (RuntimeException e) {
            logOnce("rendering a screen effect", e);
        }
    }

    // one system's particles as a retained gui element; the gui renderer batches elements that share a
    // pipeline and texture setup, so all additive systems still land in one draw
    private record HudParticles(Entry entry, RenderPipeline pipeline, TextureSetup textureSetup,
            Matrix3x2f pose, ScreenRectangle bounds) implements GuiElementRenderState {

        // one rotated quad per particle. of the render spec only blend, sprite, and animate apply on the
        // hud; stretch, lit, soft, and mesh are world features and are ignored here. engine y maps
        // straight to screen y, which grows down; authors write screen-space specs knowing that
        @Override
        public void buildVertices(VertexConsumer vc) {
            RenderSpec render = entry.spec().render();
            ParticleSystem sim = entry.system();
            boolean animate = render.animate();
            float[] still = ParticleAtlas.uv(render.sprite());
            for (Particle p : sim.particles()) {
                int color = sim.colorOf(p);
                int a = (int) (sim.alphaOf(p) * 255f);
                int argb = (a << 24) | (color & 0xFFFFFF);
                float s = sim.sizeOf(p) / 2f;
                float cx = entry.x() + p.pos.x();
                float cy = entry.y() + p.pos.y();
                float[] uv = animate ? ParticleAtlas.uv(render.sprite(), frameOf(p)) : still;
                // rotate the corners in plane, the same trick as Billboards. screen y grows down, so the
                // +y corners sample the cell's bottom edge to keep the sprite upright
                float cs = (float) Math.cos(p.rotation);
                float sn = (float) Math.sin(p.rotation);
                vc.addVertexWith2DPose(pose, cx - s * cs + s * sn, cy - s * sn - s * cs).setUv(uv[0], uv[1]).setColor(argb);
                vc.addVertexWith2DPose(pose, cx - s * cs - s * sn, cy - s * sn + s * cs).setUv(uv[0], uv[3]).setColor(argb);
                vc.addVertexWith2DPose(pose, cx + s * cs - s * sn, cy + s * sn + s * cs).setUv(uv[2], uv[3]).setColor(argb);
                vc.addVertexWith2DPose(pose, cx + s * cs + s * sn, cy + s * sn - s * cs).setUv(uv[2], uv[1]).setColor(argb);
            }
        }

        @Override
        public ScreenRectangle scissorArea() {
            return null;
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
