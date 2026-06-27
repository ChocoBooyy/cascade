package dev.chocoboy.cascade.neoforge.net;

import dev.chocoboy.cascade.neoforge.client.CascadeClientHandler;
import dev.chocoboy.cascade.neoforge.client.ClientShakeHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CascadeNetwork {

    private CascadeNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(EffectPayload.TYPE, EffectPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> CascadeClientHandler.handle(payload)))
                .playToClient(ShakePayload.TYPE, ShakePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientShakeHandler.handle(payload)));
    }
}
