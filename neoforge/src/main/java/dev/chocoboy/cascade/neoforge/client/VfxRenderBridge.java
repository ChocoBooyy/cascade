package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.client.PostFx;
import dev.chocoboy.cascade.client.ScreenVfxManager;
import dev.chocoboy.cascade.client.VfxRenderManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// feeds neoforge's client tick and render pass into the shared render manager. 26.1 split the render
// level stages into their own event subclasses and dropped the camera and partial tick from the event, so
// those are read from the client instead
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
    public void onAfterTranslucent(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VfxRenderManager.get().render(event.getPoseStack(), buffers, camera.rotation(), camera.position());
        // the bloom capture re-renders here, in the same stage, so the frame's matrices still match
        PostFx.captureVfx(event.getPoseStack(), buffers, camera.rotation(), camera.position());
    }

    @SubscribeEvent
    public void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        // run post fx after the whole level, so it captures particles too
        PostFx.process(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
