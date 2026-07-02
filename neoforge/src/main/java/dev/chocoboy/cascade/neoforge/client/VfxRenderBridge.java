package dev.chocoboy.cascade.neoforge.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// feeds neoforge's client tick and render pass into the shared render manager
public final class VfxRenderBridge {

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        VfxRenderManager.get().clientTick();
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VfxRenderManager.get().render(event.getPoseStack(), buffers, camera.rotation(), camera.getPosition());
    }
}
