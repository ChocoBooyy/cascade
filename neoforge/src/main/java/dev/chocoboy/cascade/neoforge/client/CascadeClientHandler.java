package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.engine.effect.BeamState;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.neoforge.net.BeamPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import java.util.Random;

public final class CascadeClientHandler {

    private CascadeClientHandler() {
    }

    public static void handleEmitter(EmitterPayload payload) {
        VfxRenderManager.get().spawn(
                new ParticleBurstEffect(payload.origin(), payload.spec().build(new Random(payload.seed()))));
    }

    public static void handleBeam(BeamPayload payload) {
        Vec3f from = new Vec3f((float) payload.from().x, (float) payload.from().y, (float) payload.from().z);
        Vec3f to = new Vec3f((float) payload.to().x, (float) payload.to().y, (float) payload.to().z);
        BeamState state = payload.spec().build(from, to, new Random(payload.seed()));
        VfxRenderManager.get().spawn(new BeamEffect(state, payload.spec().color(), payload.spec().width()));
    }
}
