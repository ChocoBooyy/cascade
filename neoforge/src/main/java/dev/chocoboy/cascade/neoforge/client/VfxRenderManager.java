package dev.chocoboy.cascade.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VfxFrame frame = new VfxFrame(pose, buffers, camera.rotation(), camera.getPosition());
        List<RenderedEffect> failed = null;
        try {
            for (RenderedEffect effect : active) {
                try {
                    effect.render(frame);
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
