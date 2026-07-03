package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.client.CascadeClientHandler;
import dev.chocoboy.cascade.neoforge.client.ClientShakeHandler;
import dev.chocoboy.cascade.net.BeamPayload;
import dev.chocoboy.cascade.net.DomePayload;
import dev.chocoboy.cascade.net.EffectPayload;
import dev.chocoboy.cascade.net.EmitterPayload;
import dev.chocoboy.cascade.net.LightPayload;
import dev.chocoboy.cascade.net.SdfPayload;
import dev.chocoboy.cascade.net.ShakePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CascadeNetwork {

    private CascadeNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(BeamPayload.TYPE, BeamPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleBeam(payload)))
                .playToClient(ShakePayload.TYPE, ShakePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientShakeHandler.handle(payload)))
                .playToClient(EmitterPayload.TYPE, EmitterPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleEmitter(payload)))
                .playToClient(LightPayload.TYPE, LightPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleLight(payload)))
                .playToClient(DomePayload.TYPE, DomePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleDome(payload)))
                .playToClient(SdfPayload.TYPE, SdfPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleSdf(payload)))
                .playToClient(EffectPayload.TYPE, EffectPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleEffect(payload)));
    }
}
