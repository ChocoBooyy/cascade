package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.engine.effect.BeamState;
import dev.chocoboy.cascade.engine.effect.CollisionProbe;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.ParticleSystem;
import dev.chocoboy.cascade.engine.math.Vec3f;
import dev.chocoboy.cascade.neoforge.net.BeamPayload;
import dev.chocoboy.cascade.neoforge.net.DomePayload;
import dev.chocoboy.cascade.neoforge.net.EffectPayload;
import dev.chocoboy.cascade.neoforge.net.EmitterPayload;
import dev.chocoboy.cascade.neoforge.net.LightPayload;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;

public final class CascadeClientHandler {

    private CascadeClientHandler() {
    }

    public static void handleEmitter(EmitterPayload payload) {
        float density = ParticleQuality.density();
        CollisionProbe probe = payload.spec().collision().enabled()
                ? new LevelCollisionProbe(Minecraft.getInstance().level, payload.origin())
                : null;
        ParticleSystem system = payload.spec().build(new Random(payload.seed()), probe, density);
        VfxRenderManager.get().spawn(new ParticleBurstEffect(
                payload.origin(),
                system,
                payload.spec().render(),
                payload.spec().subEmitter(),
                0));
    }

    // build one particle system per layered emitter at the shared origin, each seeded base+index so the
    // layers differ yet the whole effect replays identically for a given seed
    public static void handleEffect(EffectPayload payload) {
        float density = ParticleQuality.density();
        List<EmitterSpec> emitters = payload.spec().emitters();
        for (int i = 0; i < emitters.size(); i++) {
            EmitterSpec spec = emitters.get(i);
            CollisionProbe probe = spec.collision().enabled()
                    ? new LevelCollisionProbe(Minecraft.getInstance().level, payload.origin())
                    : null;
            ParticleSystem system = spec.build(new Random(payload.seed() + i), probe, density);
            VfxRenderManager.get().spawn(new ParticleBurstEffect(
                    payload.origin(), system, spec.render(), spec.subEmitter(), 0));
        }
    }

    public static void handleBeam(BeamPayload payload) {
        Vec3f from = new Vec3f((float) payload.from().x, (float) payload.from().y, (float) payload.from().z);
        Vec3f to = new Vec3f((float) payload.to().x, (float) payload.to().y, (float) payload.to().z);
        BeamState state = payload.spec().build(from, to, new Random(payload.seed()));
        VfxRenderManager.get().spawn(new BeamEffect(state, payload.spec().color(), payload.spec().width()));
    }

    public static void handleLight(LightPayload payload) {
        VfxRenderManager.get().spawn(
                new LightSplat(payload.pos(), payload.color(), payload.radius(), payload.duration()));
    }

    public static void handleDome(DomePayload payload) {
        VfxRenderManager.get().spawn(
                new DomeField(payload.pos(), payload.radius(), payload.color(), payload.duration()));
    }
}
