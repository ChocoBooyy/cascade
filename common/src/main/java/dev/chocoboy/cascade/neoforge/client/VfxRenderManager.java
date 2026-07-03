package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// holds and draws the live effects. loader-agnostic: each loader feeds it a client tick and a render pass
// with the frame's camera. it never touches a loader api, so both neoforge and fabric drive the same code
public final class VfxRenderManager {

    private static final VfxRenderManager INSTANCE = new VfxRenderManager();
    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    // effects are cosmetic, so cap them rather than risk unbounded growth under spam
    private static final int MAX_EFFECTS = 256;

    // effects past this range are not drawn at all; they still tick so they stay in sync if you approach
    private static final double CULL_DISTANCE = 96.0;
    private static final double CULL_DISTANCE_SQ = CULL_DISTANCE * CULL_DISTANCE;

    // soft ceiling on primitives submitted per frame. effects render nearest first, so under heavy load
    // the closest work is kept and the far tail is dropped for that frame
    private static final int MAX_PRIMITIVES_PER_FRAME = 12000;

    private final List<RenderedEffect> active = new ArrayList<>();
    // effects spawned while a tick pass is running (sub-emitters), held until the pass finishes
    private final List<RenderedEffect> pending = new ArrayList<>();
    private boolean ticking;
    private boolean loggedError;

    private VfxRenderManager() {
    }

    public static VfxRenderManager get() {
        return INSTANCE;
    }

    public void spawn(RenderedEffect effect) {
        // a sub-emitter spawns mid-tick; queue it so we do not mutate active while it is being iterated
        if (ticking) {
            if (active.size() + pending.size() < MAX_EFFECTS) {
                pending.add(effect);
            }
            return;
        }
        if (active.size() >= MAX_EFFECTS) {
            active.remove(0);
        }
        active.add(effect);
    }

    public void clientTick() {
        ticking = true;
        try {
            active.removeIf(effect -> {
                try {
                    return effect.tick();
                } catch (RuntimeException e) {
                    logOnce("ticking an effect", e);
                    return true;
                }
            });
        } finally {
            ticking = false;
        }
        if (!pending.isEmpty()) {
            active.addAll(pending);
            pending.clear();
        }
    }

    // draw the visible effects for this frame. the loader supplies the frame's pose, shared buffer source
    // and camera, since it reads those from its own render event
    public void render(PoseStack pose, MultiBufferSource.BufferSource buffers, Quaternionf camRot, Vec3 camPos) {
        render(pose, buffers, camRot, camPos, true);
    }

    // the bloom capture pass re-renders the same frame into an offscreen target and must not refresh the
    // soft depth copy: copyFromMain rebinds the main target mid-pass, which would send the capture draw there
    boolean render(PoseStack pose, MultiBufferSource.BufferSource buffers, Quaternionf camRot, Vec3 camPos,
            boolean refreshSoftDepth) {
        if (active.isEmpty()) {
            return false;
        }
        VfxRenderQueue queue = new VfxRenderQueue();
        VfxFrame frame = new VfxFrame(pose, buffers, camRot, camPos, queue);

        // cull beyond range, then draw nearest first so the per-frame budget keeps the closest effects
        List<RenderedEffect> visible = new ArrayList<>();
        for (RenderedEffect effect : active) {
            if (effect.position().distanceToSqr(camPos) <= CULL_DISTANCE_SQ) {
                visible.add(effect);
            }
        }
        visible.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(camPos)));

        // only refresh the scene depth copy when a soft effect is actually on screen, so ordinary effects
        // never pay for the blit. soft effects sample it to fade where they meet geometry
        boolean anySoft = false;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).soft()) {
                anySoft = true;
                break;
            }
        }
        if (anySoft && refreshSoftDepth) {
            SoftDepth.copyFromMain();
            if (CascadeShaders.soft() != null) {
                CascadeShaders.soft().setSampler("DepthSampler", SoftDepth.depthTextureId());
            }
            if (CascadeShaders.softLit() != null) {
                CascadeShaders.softLit().setSampler("DepthSampler", SoftDepth.depthTextureId());
            }
        }

        // effects only submit work here; the queue draws it grouped by render type in the playback below
        int primitives = 0;
        List<RenderedEffect> failed = new ArrayList<>();
        for (RenderedEffect effect : visible) {
            if (primitives >= MAX_PRIMITIVES_PER_FRAME) {
                break;
            }
            queue.owner(effect);
            try {
                effect.render(frame);
                primitives += effect.drawCount();
            } catch (RuntimeException e) {
                failed.add(effect);
                logOnce("rendering an effect", e);
            }
        }
        try {
            queue.play(buffers, (owner, e) -> {
                if (owner instanceof RenderedEffect effect) {
                    failed.add(effect);
                }
                logOnce("rendering an effect", e);
            });
        } finally {
            buffers.endBatch();
        }
        if (!failed.isEmpty()) {
            active.removeAll(failed);
        }
        return !visible.isEmpty();
    }

    private void logOnce(String what, RuntimeException e) {
        if (!loggedError) {
            loggedError = true;
            LOGGER.error("Cascade error while {}, further errors suppressed", what, e);
        }
    }
}
