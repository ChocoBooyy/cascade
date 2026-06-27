package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.neoforge.net.CascadeEffects;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;

public final class CascadeClientHandler {

    private CascadeClientHandler() {
    }

    public static void handleEmitter(EmitterPayload payload) {
        VfxRenderManager.get().spawn(
                new ParticleBurstEffect(payload.origin(), payload.spec().build(new Random(payload.seed()))));
    }

    public static void handle(EffectPayload payload) {
        ResourceLocation id = payload.effect();
        if (CascadeEffects.BEAM.equals(id)) {
            VfxRenderManager.get().spawn(BeamEffect.bolt(payload.a(), payload.b(), payload.seed()));
        }
    }
}
