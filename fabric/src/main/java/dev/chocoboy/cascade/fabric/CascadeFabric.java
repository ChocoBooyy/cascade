package dev.chocoboy.cascade.fabric;

import dev.chocoboy.cascade.CascadeCommon;
import dev.chocoboy.cascade.Vfx;
import dev.chocoboy.cascade.VfxSequencer;
import dev.chocoboy.cascade.net.BeamPayload;
import dev.chocoboy.cascade.net.DomePayload;
import dev.chocoboy.cascade.net.EffectPayload;
import dev.chocoboy.cascade.net.EmitterPayload;
import dev.chocoboy.cascade.net.LightPayload;
import dev.chocoboy.cascade.net.SdfPayload;
import dev.chocoboy.cascade.net.ShakePayload;
import dev.chocoboy.cascade.testmod.CascadeDemos;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;

public final class CascadeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CascadeCommon.init();
        Vfx.sender(new FabricNetworkSender());
        registerPayloads();
        ServerTickEvents.END_SERVER_TICK.register(server -> VfxSequencer.get().tick());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricEffectsReload());
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            registerDevCommand();
        }
    }

    // the full dev testmod for the fabric port: the same /cascade command the neoforge testmod builds, out of
    // the shared CascadeDemos. guarded so it never reaches a shipped build
    private static void registerDevCommand() {
        CascadeDemos.registerContainComponent();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var root = CascadeDemos.serverTree();
            CascadeDemos.addToggles(root, () -> FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
            dispatcher.register(root);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> CascadeDemos.resetTour());
    }

    private static void registerPayloads() {
        PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.clientboundPlay();
        s2c.register(EmitterPayload.TYPE, EmitterPayload.STREAM_CODEC);
        s2c.register(EffectPayload.TYPE, EffectPayload.STREAM_CODEC);
        s2c.register(BeamPayload.TYPE, BeamPayload.STREAM_CODEC);
        s2c.register(LightPayload.TYPE, LightPayload.STREAM_CODEC);
        s2c.register(DomePayload.TYPE, DomePayload.STREAM_CODEC);
        s2c.register(SdfPayload.TYPE, SdfPayload.STREAM_CODEC);
        s2c.register(ShakePayload.TYPE, ShakePayload.STREAM_CODEC);
    }
}
