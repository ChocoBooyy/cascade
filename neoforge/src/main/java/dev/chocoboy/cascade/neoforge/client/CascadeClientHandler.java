package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.neoforge.net.CascadeEffects;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import net.minecraft.resources.ResourceLocation;

public final class CascadeClientHandler {

    private CascadeClientHandler() {
    }

    public static void handle(EffectPayload payload) {
        ResourceLocation id = payload.effect();
        RenderedEffect effect;
        if (CascadeEffects.BURST.equals(id)) {
            effect = ParticleBurstEffect.burst(payload.a(), payload.seed());
        } else if (CascadeEffects.BEAM.equals(id)) {
            effect = BeamEffect.bolt(payload.a(), payload.b(), payload.seed());
        } else {
            return;
        }
        VfxRenderManager.get().spawn(effect);
    }
}
