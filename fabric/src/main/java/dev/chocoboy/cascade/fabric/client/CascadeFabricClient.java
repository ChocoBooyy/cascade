package dev.chocoboy.cascade.fabric.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.chocoboy.cascade.neoforge.client.CascadeClientHandler;
import dev.chocoboy.cascade.neoforge.client.CascadeRenderTypes;
import dev.chocoboy.cascade.neoforge.client.CascadeShaders;
import dev.chocoboy.cascade.neoforge.client.VfxRenderManager;
import dev.chocoboy.cascade.neoforge.net.BeamPayload;
import dev.chocoboy.cascade.neoforge.net.DomePayload;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import dev.chocoboy.cascade.neoforge.net.LightPayload;
import dev.chocoboy.cascade.neoforge.net.ShakePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class CascadeFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CascadeRenderTypes.install(new FabricRenderTypes());
        registerShaders();
        registerReceivers();
        ClientTickEvents.END_CLIENT_TICK.register(client -> VfxRenderManager.get().clientTick());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            Camera cam = context.camera();
            VfxRenderManager.get().render(context.matrixStack(),
                    Minecraft.getInstance().renderBuffers().bufferSource(), cam.rotation(), cam.getPosition());
        });
    }

    private static void registerShaders() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(ResourceLocation.fromNamespaceAndPath("cascade", "cascade_soft"),
                    DefaultVertexFormat.POSITION_TEX_COLOR, CascadeShaders::setSoft);
            context.register(ResourceLocation.fromNamespaceAndPath("cascade", "cascade_soft_lit"),
                    DefaultVertexFormat.PARTICLE, CascadeShaders::setSoftLit);
        });
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
        // shake is descoped on fabric v1: accept the payload so the server send stays valid, then ignore it
        ClientPlayNetworking.registerGlobalReceiver(ShakePayload.TYPE, (payload, context) -> {
        });
    }
}
