package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.neoforge.client.CascadeClientHandler;
import dev.chocoboy.cascade.neoforge.client.ClientShakeHandler;
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
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handleDome(payload)));
    }
}
