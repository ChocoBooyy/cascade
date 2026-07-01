package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
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

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || active.isEmpty()) {
            return;
        }
        Camera camera = event.getCamera();
        PoseStack pose = event.getPoseStack();
        Vec3 camPos = camera.getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VfxFrame frame = new VfxFrame(pose, buffers, camera.rotation(), camPos);

        // cull beyond range, then draw nearest first so the per-frame budget keeps the closest effects
        List<RenderedEffect> visible = new ArrayList<>();
        for (RenderedEffect effect : active) {
            if (effect.position().distanceToSqr(camPos) <= CULL_DISTANCE_SQ) {
                visible.add(effect);
            }
        }
        visible.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(camPos)));

        // refresh the depth copy and point the soft shader at it before any draw. soft effects sample this
        // to fade where they meet geometry; non soft effects ignore it, so the cost is one depth blit a frame
        SoftDepth.copyFromMain();
        if (CascadeShaders.soft() != null) {
            CascadeShaders.soft().setSampler("DepthSampler", SoftDepth.depthTextureId());
        }
        if (CascadeShaders.softLit() != null) {
            CascadeShaders.softLit().setSampler("DepthSampler", SoftDepth.depthTextureId());
        }

        int primitives = 0;
        List<RenderedEffect> failed = null;
        try {
            for (RenderedEffect effect : visible) {
                if (primitives >= MAX_PRIMITIVES_PER_FRAME) {
                    break;
                }
                try {
                    effect.render(frame);
                    primitives += effect.drawCount();
                } catch (RuntimeException e) {
                    if (failed == null) {
                        failed = new ArrayList<>();
                    }
                    failed.add(effect);
                    logOnce("rendering an effect", e);
                }
            }
        } finally {
            buffers.endBatch();
        }
        if (failed != null) {
            active.removeAll(failed);
        }
    }

    private void logOnce(String what, RuntimeException e) {
        if (!loggedError) {
            loggedError = true;
            LOGGER.error("Cascade error while {}, further errors suppressed", what, e);
        }
    }
}
