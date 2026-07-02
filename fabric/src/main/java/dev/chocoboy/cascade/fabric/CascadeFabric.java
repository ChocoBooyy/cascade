package dev.chocoboy.cascade.fabric;

import dev.chocoboy.cascade.CascadeCommon;
import dev.chocoboy.cascade.neoforge.Vfx;
import dev.chocoboy.cascade.neoforge.VfxSequencer;
import dev.chocoboy.cascade.neoforge.net.BeamPayload;
import dev.chocoboy.cascade.neoforge.net.DomePayload;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import dev.chocoboy.cascade.neoforge.net.LightPayload;
import dev.chocoboy.cascade.neoforge.net.ShakePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public final class CascadeFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CascadeCommon.init();
        Vfx.sender(new FabricNetworkSender());
        registerPayloads();
        ServerTickEvents.END_SERVER_TICK.register(server -> VfxSequencer.get().tick());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricEffectsReload());
    }

    private static void registerPayloads() {
        PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.playS2C();
        s2c.register(EmitterPayload.TYPE, EmitterPayload.STREAM_CODEC);
        s2c.register(EffectPayload.TYPE, EffectPayload.STREAM_CODEC);
        s2c.register(BeamPayload.TYPE, BeamPayload.STREAM_CODEC);
        s2c.register(LightPayload.TYPE, LightPayload.STREAM_CODEC);
        s2c.register(DomePayload.TYPE, DomePayload.STREAM_CODEC);
        s2c.register(ShakePayload.TYPE, ShakePayload.STREAM_CODEC);
    }
}
