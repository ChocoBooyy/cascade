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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    // dev-only parity check for the fabric port: /cascadefab fires a burst at the player. guarded so it
    // never reaches a shipped build
    private static void registerDevCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                dispatcher.register(Commands.literal("cascadefab").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Vfx.burst((ServerLevel) player.level(), player.position().add(0.0, 1.0, 0.0));
                    return 1;
                })));
    }

    private static void registerPayloads() {
        PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.playS2C();
        s2c.register(EmitterPayload.TYPE, EmitterPayload.STREAM_CODEC);
        s2c.register(EffectPayload.TYPE, EffectPayload.STREAM_CODEC);
        s2c.register(BeamPayload.TYPE, BeamPayload.STREAM_CODEC);
        s2c.register(LightPayload.TYPE, LightPayload.STREAM_CODEC);
        s2c.register(DomePayload.TYPE, DomePayload.STREAM_CODEC);
        s2c.register(SdfPayload.TYPE, SdfPayload.STREAM_CODEC);
        s2c.register(ShakePayload.TYPE, ShakePayload.STREAM_CODEC);
    }
}
