package dev.chocoboy.cascade.fabric.client;

import dev.chocoboy.cascade.client.CascadeClientHandler;
import dev.chocoboy.cascade.client.CascadeRenderTypes;
import dev.chocoboy.cascade.client.CoreRenderTypes;
import dev.chocoboy.cascade.client.PostFx;
import dev.chocoboy.cascade.client.ScreenVfxManager;
import dev.chocoboy.cascade.client.VfxRenderManager;
import dev.chocoboy.cascade.net.BeamPayload;
import dev.chocoboy.cascade.net.DomePayload;
import dev.chocoboy.cascade.net.EffectPayload;
import dev.chocoboy.cascade.net.EmitterPayload;
import dev.chocoboy.cascade.net.LightPayload;
import dev.chocoboy.cascade.net.SdfPayload;
import dev.chocoboy.cascade.net.ShakePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

// on 26.1 the render types and pipelines are shared vanilla-api code (CoreRenderTypes/CorePipelines), so
// this glue only installs the provider and feeds the shared managers fabric's tick, level and hud hooks.
// pipelines compile lazily on first draw, so no registration hook is needed here
public final class CascadeFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CascadeRenderTypes.install(new CoreRenderTypes());
        registerReceivers();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            VfxRenderManager.get().clientTick();
            ScreenVfxManager.get().tick();
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("cascade", "screen_vfx"),
                (gui, tickCounter) -> ScreenVfxManager.get().render(gui));
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            VfxRenderManager.get().render(context.poseStack(),
                    Minecraft.getInstance().renderBuffers().bufferSource(), camera.rotation(), camera.position());
            // the bloom capture re-renders here, in the same stage, so the frame's matrices still match
            PostFx.captureVfx(context.poseStack(),
                    Minecraft.getInstance().renderBuffers().bufferSource(), camera.rotation(), camera.position());
        });
        // run post fx after the whole level, so it captures particles too
        LevelRenderEvents.END_MAIN.register(context ->
                PostFx.process(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
    }

    private static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(EmitterPayload.TYPE,
                (payload, context) -> CascadeClientHandler.handleEmitter(payload));
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE,
                (payload, context) -> CascadeClientHandler.handleEffect(payload));
        ClientPlayNetworking.registerGlobalReceiver(BeamPayload.TYPE,
                (payload, context) -> CascadeClientHandler.handleBeam(payload));
        ClientPlayNetworking.registerGlobalReceiver(LightPayload.TYPE,
                (payload, context) -> CascadeClientHandler.handleLight(payload));
        ClientPlayNetworking.registerGlobalReceiver(DomePayload.TYPE,
                (payload, context) -> CascadeClientHandler.handleDome(payload));
        ClientPlayNetworking.registerGlobalReceiver(SdfPayload.TYPE,
                (payload, context) -> CascadeClientHandler.handleSdf(payload));
        // shake is descoped on fabric v1: accept the payload so the server send stays valid, then ignore it
        ClientPlayNetworking.registerGlobalReceiver(ShakePayload.TYPE, (payload, context) -> {
        });
    }
}
