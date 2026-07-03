package dev.chocoboy.cascade.neoforge.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// feeds neoforge's client tick and render pass into the shared render manager
public final class VfxRenderBridge {

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        VfxRenderManager.get().clientTick();
        ScreenVfxManager.get().tick();
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        ScreenVfxManager.get().render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Camera camera = event.getCamera();
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            VfxRenderManager.get().render(event.getPoseStack(), buffers, camera.rotation(), camera.getPosition());
            // the bloom capture re-renders here, in the same stage, so the frame's matrices still match
            PostFx.captureVfx(event.getPoseStack(), buffers, camera.rotation(), camera.getPosition());
            return;
        }
        // run post fx after the whole level, so it captures particles too
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            PostFx.process(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }
}
